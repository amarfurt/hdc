package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;

import utils.Connection;
import utils.ModelConversion;
import utils.OrderOperations;
import utils.search.TextSearch;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Circle extends Model implements Comparable<Circle> {

	private static final String collection = "circles";

	public String name;
	public ObjectId owner;
	public int order;
	public BasicDBList members;
	public BasicDBList shared; // records shared with this circle

	@Override
	public int compareTo(Circle o) {
		return this.order - o.order;
	}

	@Override
	public String toString() {
		return name;
	}

	public static boolean isOwner(ObjectId circleId, ObjectId userId) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("owner", userId);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Find the circles that are owned by the given user.
	 */
	public static List<Circle> findOwnedBy(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
		}
		// sort by order field
		Collections.sort(circles);
		return circles;
	}

	/**
	 * Find the circles this user is a member of.
	 */
	public static List<Circle> findMemberOf(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("members", userId);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
		}
		return circles;
	}

	/**
	 * Returns the ids of the records that are shared with this user.
	 */
	public static Set<ObjectId> getSharedWith(ObjectId userId) {
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("members", userId);
		DBObject projection = new BasicDBObject("shared", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList shared = (BasicDBList) result.next().get("shared");
			for (Object id : shared) {
				recordIds.add((ObjectId) id);
			}
		}
		return recordIds;
	}

	/**
	 * Find the users that the given user has already added to his circles.
	 */
	public static List<User> findContacts(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		Set<ObjectId> contacts = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		DBObject projection = new BasicDBObject("members", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList members = (BasicDBList) result.next().get("members");
			for (Object member : members) {
				contacts.add((ObjectId) member);
			}
		}
		List<User> userList = new ArrayList<User>();
		for (ObjectId contactId : contacts) {
			userList.add(User.find(contactId));
		}
		Collections.sort(userList);
		return userList;
	}

	/**
	 * Find the circles of the given user that contain the given record.
	 */
	public static Set<ObjectId> findWithRecord(ObjectId recordId, ObjectId userId) {
		Set<ObjectId> circles = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		query.put("shared", recordId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			circles.add((ObjectId) result.next().get("_id"));
		}
		return circles;
	}

	/**
	 * Returns a set with ids of the members of the given circle.
	 */
	public static Set<ObjectId> getMembers(ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("members", 1);
		DBObject result = Connection.getCollection(collection).findOne(query, projection);
		BasicDBList members = (BasicDBList) result.get("members");
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (Object userId : members) {
			userIds.add((ObjectId) userId);
		}
		return userIds;
	}

	/**
	 * Adds a circle and returns the error message (null in absence of errors). Also adds the generated id to the circle
	 * object.
	 */
	public static String add(Circle newCircle) throws IllegalArgumentException,
			IllegalAccessException, ElasticSearchException, IOException {
		if (!circleWithSameNameExists(newCircle.name, newCircle.owner)) {
			newCircle.order = OrderOperations.getMax(collection, newCircle.owner) + 1;
			DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newCircle));
			WriteResult result = Connection.getCollection(collection).insert(insert);
			newCircle._id = (ObjectId) insert.get("_id");
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}

			// also add this circle to the user's search index
			TextSearch.add(newCircle.owner, "circle", newCircle._id, newCircle.name);
			return null;
		} else {
			return "A circle with this name already exists.";
		}
	}

	/**
	 * Tries to rename the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String rename(ObjectId circleId, String newName) throws ElasticSearchException, IOException {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject foundCircle = Connection.getCollection(collection).findOne(query);
		if (foundCircle == null) {
			return "No circle with this id exists.";
		}
		ObjectId ownerId = (ObjectId) foundCircle.get("owner");
		if (!circleWithSameNameExists(newName, ownerId)) {
			DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}
			
			// update search index
			TextSearch.delete(ownerId, "circle", circleId);
			TextSearch.add(ownerId, "circle", circleId, newName);
			return null;
		} else {
			return "A circle with this name already exists.";
		}
	}

	/**
	 * Tries to delete the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String delete(ObjectId circleId) {
		// find owner and order first
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject circle = Connection.getCollection(collection).findOne(query);
		if (circle == null) {
			return "No circle with this id exists.";
		}
		ObjectId owner = (ObjectId) circle.get("owner");
		int order = (Integer) circle.get("order");

		// remove circle
		WriteResult result = Connection.getCollection(collection).remove(query);
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// decrement all order fields greater than the removed circle
		return OrderOperations.decrement(collection, owner, order, 0);
	}

	/**
	 * Adds a member to the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String addMember(ObjectId circleId, ObjectId userId) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (User.find(userId) == null) {
			return "No user with this email address exists.";
		} else if (Circle.isOwner(circleId, userId)) {
			return "Owner can't be added to own circle.";
		} else if (Circle.userIsInCircle(circleId, userId)) {
			return "User is already in this circle.";
		} else {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("members", userId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Removes a member from the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String removeMember(ObjectId circleId, ObjectId userId) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (User.find(userId) == null) {
			return "No user with this email address exists.";
		} else if (!Circle.userIsInCircle(circleId, userId)) {
			return "User is not in this circle.";
		} else {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("members", userId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Returns a list of all records shared with this circle.
	 */
	public static Set<ObjectId> getShared(ObjectId circleId, ObjectId userId) {
		Set<ObjectId> shared = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("owner", userId);
		DBObject foundObject = Connection.getCollection(collection).findOne(query);
		if (foundObject != null) {
			BasicDBList sharedRecords = (BasicDBList) foundObject.get("shared");
			for (Object obj : sharedRecords) {
				shared.add((ObjectId) obj);
			}
		}
		return shared;
	}

	public static String updateShared(List<ObjectId> circleIds, ObjectId recordId, ObjectId userId)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else {
			DBObject query = new BasicDBObject("owner", userId);
			query.put("_id", new BasicDBObject("$in", circleIds.toArray()));
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("shared", recordId));
			WriteResult result = Connection.getCollection(collection).updateMulti(query, update);
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}
			query = new BasicDBObject("owner", userId);
			query.put("_id", new BasicDBObject("$nin", circleIds.toArray()));
			update = new BasicDBObject("$pull", new BasicDBObject("shared", recordId));
			result = Connection.getCollection(collection).updateMulti(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Shares the given records with the given circle.
	 */
	public static String shareRecords(ObjectId circleId, Set<ObjectId> recordIds) {
		// TODO check whether circle owner is also record owner?
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("shared", new BasicDBObject("$each",
				recordIds.toArray())));
		WriteResult result = Connection.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Stops sharing the given records with the given circle.
	 */
	public static String pullRecords(ObjectId circleId, Set<ObjectId> recordIds) {
		// TODO check whether circle owner is also record owner?
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("$pullAll", new BasicDBObject("shared", recordIds.toArray()));
		WriteResult result = Connection.getCollection(collection).update(query, update);
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Checks whether a circle with the same name already exists for the given owner.
	 */
	private static boolean circleWithSameNameExists(String name, ObjectId userId) {
		DBObject query = new BasicDBObject();
		query.put("name", name);
		query.put("owner", userId);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Checks whether the given user is in the given circle.
	 */
	private static boolean userIsInCircle(ObjectId circleId, ObjectId userId) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("members", new BasicDBObject("$in", new ObjectId[] { userId }));
		return (Connection.getCollection(collection).findOne(query) != null);
	}

}
