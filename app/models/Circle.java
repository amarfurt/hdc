package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class Circle {

	private static final String collection = "circles";

	public ObjectId _id;
	public String name;
	public String owner;
	public BasicDBList members;

	public static boolean isOwner(ObjectId circleId, String email) {
		DBObject query = new BasicDBObjectBuilder().append("_id", circleId).append("owner", email).get();
		DBObject result = Connection.getCollection(collection).findOne(query);
		return (result != null);
	}

	/**
	 * Find the circles that are owned by the given user.
	 */
	public static List<Circle> findOwnedBy(User user) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<Circle> circles = new ArrayList<Circle>();
		DBObject query = new BasicDBObject("owner", user.email);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
		}
		// TODO possibly sort by creation timestamp or a specific order field (changeable)
		return circles;
	}

	/**
	 * Adds a circle and returns the error message (null in absence of errors). Also adds the generated id to the circle
	 * object.
	 */
	public static String add(Circle newCircle) throws IllegalArgumentException, IllegalAccessException {
		if (!circleWithSameNameExists(newCircle.name, newCircle.owner)) {
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
			return "This circle doesn't exist.";
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
		DBObject query = new BasicDBObject("_id", circleId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Adds a member to the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String addMember(ObjectId circleId, String newMember) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		if (User.find(newMember) != null) {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("members", newMember));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		} else {
			return "No user with this email address exists.";
		}
	}

	/**
	 * Removes a member from the circle with the given id and returns the error message (null in absence of errors).
	 */
	public static String removeMember(ObjectId circleId, String member) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		if (User.find(member) != null) {
			DBObject query = new BasicDBObject("_id", circleId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("members", member));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		} else {
			return "No user with this email address exists.";
		}
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

}
