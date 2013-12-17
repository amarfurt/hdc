package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.Application;

public class App extends Model implements Comparable<App> {

	private static final String collection = "apps";

	public ObjectId creator;
	public String name;
	public String description;
	public String create; // url for creating a new record
	public String details; // url for detailed view of a record

	@Override
	public int compareTo(App other) {
		return this.name.compareTo(other.name);
	}

	/**
	 * Validate form input data for registering a new app.
	 */
	public String validate() {
		if (name.isEmpty() || description.isEmpty() || create.isEmpty() || details.isEmpty()) {
			return "Please fill in all fields.";
		} else if (App.exists(Application.getCurrentUserId(), name)) {
			return "An app with the same name already exists.";
		}
		return null;
	}

	public static boolean exists(ObjectId creatorId, String name) {
		DBObject query = new BasicDBObject("creator", creatorId);
		query.put("name", name);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static String getName(ObjectId appId) {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject projection = new BasicDBObject("name", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("name");
	}

	public static String getCreate(ObjectId appId) {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject projection = new BasicDBObject("create", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("create");
	}

	public static String getDetails(ObjectId appId) {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject projection = new BasicDBObject("details", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("details");
	}

	public static App find(ObjectId appId) throws ModelException {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject result = Database.getCollection(collection).findOne(query);
		try {
			return ModelConversion.mapToModel(App.class, result.toMap());
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
	}

	public static Set<App> findSpotlighted() throws ModelException {
		Set<App> apps = new HashSet<App>();
		// TODO return only spotlighted apps
		// for now: return all apps
		DBCursor result = Database.getCollection(collection).find();
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				apps.add(ModelConversion.mapToModel(App.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return apps;
	}

	public static void add(App newApp) throws ModelException {
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newApp));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newApp._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// add to search index
		try {
			Search.addPublic(Type.APP, newApp._id, newApp.name, newApp.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

}
