package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

public class Circle implements Comparable<Circle> {

	private static final String collection = "circles";

	public ObjectId _id;
	public String name;
	public String owner;
	public int order;
	public BasicDBList members;
	public BasicDBList shared; // records shared with this circle
	public BasicDBList tags;

	@Override
	public int compareTo(Circle o) {
		return this.order - o.order;
	}

	public static boolean isOwner(ObjectId circleId, String email) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("owner", email);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Find the circles that are owned by the given user.
	 */
	public static List<Circle> findOwnedBy(User user) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("owner", user.email);
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
	 * Find the circles this user is a member of (excluding own circles).
	 */
	public static List<Circle> findMemberOf(User user) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("members", user.email);
		query.put("owner", new BasicDBObject("$ne", user.email));
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
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
		String owner = (String) foundCircle.get("owner");
		if (!circleWithSameNameExists(newName, owner)) {
			DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
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
		String owner = (String) circle.get("owner");
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
	public static String addMember(ObjectId circleId, String newMember) throws IllegalArgumentException,
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
	public static String removeMember(ObjectId circleId, String member) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (User.find(member) == null) {
			return "No user with this email address exists.";
		} else if (!Circle.userIsInCircle(circleId, member)) {
			return "User is not in this circle.";
		} else {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("members", member));
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
	private static boolean circleWithSameNameExists(String name, String owner) {
		DBObject query = new BasicDBObject();
		query.put("name", name);
		query.put("owner", owner);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Checks whether the given user is in the given circle.
	 */
	private static boolean userIsInCircle(ObjectId circleId, String user) {
		DBObject query = new BasicDBObject();
		query.put("_id", circleId);
		query.put("members", new BasicDBObject("$in", new String[] { user }));
		return (Connection.getCollection(collection).findOne(query) != null);
	}

}
