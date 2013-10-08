package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.OrderOperations;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

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

	public static boolean isOwner(ObjectId circleId, ObjectId user) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("owner", user);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Find the circles that are owned by the given user.
	 */
	public static List<Circle> findOwnedBy(ObjectId user) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("owner", user);
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
	public static List<Circle> findMemberOf(ObjectId user) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("members", user);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
		}
		return circles;
	}

	/**
	 * Find the users that the given user has already added to his circles.
	 */
	public static List<User> findContacts(ObjectId user) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		Set<ObjectId> contacts = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", user);
		DBObject projection = new BasicDBObject("members", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList members = (BasicDBList) result.next().get("members");
			for (Object member : members) {
				contacts.add((ObjectId) member);
			}
		}
		List<User> userList = new ArrayList<User>();
		for (ObjectId userId : contacts) {
			userList.add(User.find(userId));
		}
		Collections.sort(userList);
		return userList;
	}

	/**
	 * Find the circles of the given user that contain the given record.
	 */
	public static Set<ObjectId> findWithRecord(ObjectId recordId, ObjectId user) {
		Set<ObjectId> circles = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", user);
		query.put("shared", recordId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			circles.add((ObjectId) result.next().get("_id"));
		}
		return circles;
	}

	/**
	 * Adds a circle and returns the error message (null in absence of errors). Also adds the generated id to the circle
	 * object.
	 */
	public static String add(Circle newCircle) throws IllegalArgumentException, IllegalAccessException {
		if (!circleWithSameNameExists(newCircle.name, newCircle.owner)) {
			newCircle.order = OrderOperations.getMax(collection, newCircle.owner) + 1;
			newCircle.tags = new BasicDBList();
			for (String namePart : newCircle.name.toLowerCase().split(" ")) {
				newCircle.tags.add(namePart);
			}
			DBObject insert = new BasicDBObject(ModelConversion.modelToMap(Circle.class, newCircle));
			WriteResult result = Connection.getCollection(collection).insert(insert);
			newCircle._id = (ObjectId) insert.get("_id");
			return result.getLastError().getErrorMessage();
		} else {
			return "A circle with this name already exists.";
		}
	}

	/**
	 * Tries to rename the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String rename(ObjectId circleId, String newName) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject foundCircle = Connection.getCollection(collection).findOne(query);
		if (foundCircle == null) {
			return "No circle with this id exists.";
		}
		ObjectId owner = (ObjectId) foundCircle.get("owner");
		if (!circleWithSameNameExists(newName, owner)) {
			DBObject setFields = new BasicDBObject("name", newName);
			BasicDBList newTags = new BasicDBList();
			for (String namePart : newName.split(" ")) {
				newTags.add(namePart);
			}
			setFields.put("tags", newTags);
			DBObject update = new BasicDBObject("$set", setFields);
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
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
		int order = (int) circle.get("order");

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
	public static String addMember(ObjectId circleId, ObjectId newMember) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (User.find(newMember) == null) {
			return "No user with this email address exists.";
		} else if (Circle.isOwner(circleId, newMember)) {
			return "Owner can't be added to own circle.";
		} else if (Circle.userIsInCircle(circleId, newMember)) {
			return "User is already in this circle.";
		} else {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("members", newMember));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Removes a member from the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String removeMember(ObjectId circleId, ObjectId memberId) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (User.find(memberId) == null) {
			return "No user with this email address exists.";
		} else if (!Circle.userIsInCircle(circleId, memberId)) {
			return "User is not in this circle.";
		} else {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("members", memberId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Returns a list of all records shared with this circle.
	 */
	public static Set<ObjectId> getShared(ObjectId circleId, String email) {
		Set<ObjectId> shared = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("owner", email);
		DBObject foundObject = Connection.getCollection(collection).findOne(query);
		if (foundObject != null) {
			BasicDBList sharedRecords = (BasicDBList) foundObject.get("shared");
			for (Object obj : sharedRecords) {
				shared.add((ObjectId) obj);
			}
		}
		return shared;
	}

	public static String updateShared(List<ObjectId> circleIds, ObjectId recordId, String owner)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else {
			DBObject query = new BasicDBObject("owner", owner);
			query.put("_id", new BasicDBObject("$in", circleIds.toArray()));
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("shared", recordId));
			WriteResult result = Connection.getCollection(collection).updateMulti(query, update);
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}
			query = new BasicDBObject("owner", owner);
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
	 * Creates a new list without the members of the given circle.
	 */
	public static List<User> makeDisjoint(ObjectId circleId, List<User> userList) {
		List<User> newUserList = new ArrayList<User>(userList);
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("members", 1);
		DBObject result = Connection.getCollection(collection).findOne(query, projection);
		BasicDBList members = (BasicDBList) result.get("members");
		Set<String> emails = new HashSet<String>();
		for (Object email : members) {
			emails.add((String) email);
		}
		Iterator<User> iterator = newUserList.iterator();
		while (iterator.hasNext()) {
			if (emails.contains(iterator.next().email)) {
				iterator.remove();
			}
		}
		return newUserList;
	}

	/**
	 * Checks whether a circle with the same name already exists for the given owner.
	 */
	private static boolean circleWithSameNameExists(String name, ObjectId owner) {
		DBObject query = new BasicDBObject();
		query.put("name", name);
		query.put("owner", owner);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Checks whether the given user is in the given circle.
	 */
	private static boolean userIsInCircle(ObjectId circleId, ObjectId user) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("members", new BasicDBObject("$in", new ObjectId[] { user }));
		return (Connection.getCollection(collection).findOne(query) != null);
	}

}
