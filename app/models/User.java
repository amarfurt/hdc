package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.PasswordHash;
import utils.db.Database;
import utils.search.SearchException;
import utils.search.Search;
import utils.search.Search.Type;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class User extends Model implements Comparable<User> {

	private static final String collection = "users";

	public String email; // must be unique
	public String name;
	public String password;
	public BasicDBList visible; // records that are shared with this user (grouped by owner)
	public BasicDBList apps; // installed apps
	public BasicDBList visualizations; // installed visualizations

	@Override
	public int compareTo(User o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return name;
	}

	public static String getCollection() {
		return collection;
	}

	public static ObjectId getId(String email) {
		DBObject query = new BasicDBObject("email", email);
		DBObject projection = new BasicDBObject("_id", 1);
		return (ObjectId) Database.getCollection(collection).findOne(query, projection).get("_id");
	}

	public static String getName(ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("name", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("name");
	}

	public static User find(ObjectId userId) throws ConversionException {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject result = Database.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(User.class, result.toMap());
	}

	public static Set<User> find(Set<ObjectId> userIds) throws ConversionException {
		Set<User> users = new HashSet<User>();
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", userIds.toArray()));
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			users.add(ModelConversion.mapToModel(User.class, result.next().toMap()));
		}
		return users;
	}

	public static boolean authenticationValid(String email, String password) throws ConversionException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		if (!userExists(email)) {
			return false;
		}
		String storedPassword = getPassword(email);
		return PasswordHash.validatePassword(password, storedPassword);
	}

	public static String add(User newUser) throws ConversionException, InvalidKeySpecException,
			NoSuchAlgorithmException, SearchException {
		if (userExists(newUser.email)) {
			return "A user with this email address already exists.";
		}
		newUser.password = PasswordHash.createHash(newUser.password);
		newUser.visible = new BasicDBList();
		newUser.apps = new BasicDBList();
		newUser.visualizations = new BasicDBList();
		ObjectId defaultVisualizationId = Visualization.getId(Visualization.getDefaultVisualization());
		newUser.visualizations.add(defaultVisualizationId);
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newUser));
		WriteResult result = Database.getCollection(collection).insert(insert);
		newUser._id = (ObjectId) insert.get("_id");
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// add to search index (concatenate email and name)
		Search.addPublic(Type.USER, newUser._id, newUser.email + " " + newUser.name);
		return null;
	}

	public static String remove(ObjectId userId) {
		if (!userExists(userId)) {
			return "No user with this id exists.";
		}

		// remove from search index
		Search.deletePublic(Type.USER, userId);

		// TODO remove all the user's messages, records, spaces, circles, apps (if published, ask whether to leave it in
		// the marketplace), ...
		DBObject query = new BasicDBObject("_id", userId);
		WriteResult result = Database.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

	private static boolean userExists(ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		return (Database.getCollection(collection).findOne(query) != null);
	}

	public static boolean userExists(String email) {
		DBObject query = new BasicDBObject("email", email);
		return (Database.getCollection(collection).findOne(query) != null);
	}

	private static String getPassword(String email) {
		DBObject query = new BasicDBObject("email", email);
		DBObject projection = new BasicDBObject("password", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("password");
	}

	// Record visibility methods
	/**
	 * Makes the given records of an owner visible to the given users.
	 */
	public static String makeRecordsVisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) {
		// create an entry for the owner if it doesn't exist yet in the users' visible field
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", userIds.toArray()));
		query.put("visible", new BasicDBObject("$not", new BasicDBObject("$elemMatch", new BasicDBObject("owner",
				ownerId))));
		DBObject visibleEntry = new BasicDBObject("owner", ownerId);
		visibleEntry.put("records", new BasicDBList());
		DBObject update = new BasicDBObject("$push", new BasicDBObject("visible", visibleEntry));
		String errorMessage = Database.getCollection(collection).updateMulti(query, update).getLastError()
				.getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// add records to visible records
		query = new BasicDBObject("_id", new BasicDBObject("$in", userIds.toArray()));
		query.put("visible.owner", ownerId);
		update = new BasicDBObject("$addToSet", new BasicDBObject("visible.$.records", new BasicDBObject("$each",
				recordIds.toArray())));
		return Database.getCollection(collection).updateMulti(query, update).getLastError().getErrorMessage();
	}

	/**
	 * Removes records from the visible records of the given users.
	 */
	public static String makeRecordsInvisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) {
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", userIds.toArray()));
		query.put("visible.owner", ownerId);
		DBObject update = new BasicDBObject("$pullAll", new BasicDBObject("visible.$.records", recordIds.toArray()));
		return Database.getCollection(collection).updateMulti(query, update).getLastError().getErrorMessage();
	}

	/**
	 * Returns the visible records, grouped by their respective owner.
	 */
	public static Map<ObjectId, Set<ObjectId>> getVisibleRecords(ObjectId userId) {
		Map<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("visible", 1);
		BasicDBList visible = (BasicDBList) Database.getCollection(collection).findOne(query, projection)
				.get("visible");
		for (Object visibleEntry : visible) {
			DBObject curEntry = (DBObject) visibleEntry;
			ObjectId ownerId = (ObjectId) curEntry.get("owner");
			Set<ObjectId> recordIds = new HashSet<ObjectId>();
			BasicDBList sharedRecordIds = (BasicDBList) curEntry.get("records");
			for (Object sharedRecordId : sharedRecordIds) {
				recordIds.add((ObjectId) sharedRecordId);
			}
			visibleRecords.put(ownerId, recordIds);
		}
		return visibleRecords;
	}

	// App methods
	public static boolean hasApp(ObjectId userId, ObjectId appId) {
		DBObject query = new BasicDBObject("_id", userId);
		query.put("apps", appId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static Set<ObjectId> getApps(ObjectId userId) {
		Set<ObjectId> installedAppIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("apps", 1);
		DBObject result = Database.getCollection(collection).findOne(query, projection);
		if (result != null) {
			BasicDBList appIds = (BasicDBList) result.get("apps");
			for (Object appId : appIds) {
				installedAppIds.add((ObjectId) appId);
			}
		}
		return installedAppIds;
	}

	public static Set<App> findApps(ObjectId userId) throws ConversionException {
		Set<ObjectId> installedAppIds = getApps(userId);
		Set<App> installedApps = new HashSet<App>();
		for (ObjectId appId : installedAppIds) {
			installedApps.add(App.find(appId));
		}
		return installedApps;
	}

	public static String addApp(ObjectId userId, ObjectId appId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("apps", appId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

	public static String removeApp(ObjectId userId, ObjectId appId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject update = new BasicDBObject("$pull", new BasicDBObject("apps", appId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

	// Visualization methods
	public static boolean hasVisualization(ObjectId userId, ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", userId);
		query.put("visualizations", visualizationId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static Set<ObjectId> getVisualizations(ObjectId userId) {
		Set<ObjectId> installedVisualizationIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("visualizations", 1);
		DBObject result = Database.getCollection(collection).findOne(query, projection);
		if (result != null) {
			BasicDBList visualizationIds = (BasicDBList) result.get("visualizations");
			for (Object visualizationId : visualizationIds) {
				installedVisualizationIds.add((ObjectId) visualizationId);
			}
		}
		return installedVisualizationIds;
	}

	public static Set<Visualization> findVisualizations(ObjectId userId) throws ConversionException {
		Set<ObjectId> installedVisualizationIds = getVisualizations(userId);
		Set<Visualization> installedVisualizations = new HashSet<Visualization>();
		for (ObjectId visualizationId : installedVisualizationIds) {
			installedVisualizations.add(Visualization.find(visualizationId));
		}
		return installedVisualizations;
	}

	public static String addVisualization(ObjectId userId, ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("visualizations", visualizationId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

	public static String removeVisualization(ObjectId userId, ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject update = new BasicDBObject("$pull", new BasicDBObject("visualizations", visualizationId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

}
