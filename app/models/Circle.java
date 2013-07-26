package models;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
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
	 * Adds a circle and returns the error message (which is null in absence of errors). Also adds the generated id to
	 * the circle object.
	 */
	public static String add(Circle newCircle) throws IllegalArgumentException, IllegalAccessException {
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(Circle.class, newCircle));
		newCircle._id = (ObjectId) insert.get("_id");
		WriteResult result = Connection.getCollection(collection).insert(insert);
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Tries to rename the circle with the given id and returns the number of updated circles (0 or 1).
	 */
	public static int rename(ObjectId circleId, String newName) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("name", newName);
		WriteResult result = Connection.getCollection(collection).update(query, update);
		return result.getN();
	}

	/**
	 * Tries to delete the circle with the given id and returns the number of deleted circles (0 or 1).
	 */
	public static int delete(ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getN();
	}

}
