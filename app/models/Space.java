package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class Space {

	static final String collection = "spaces";

	public ObjectId _id;
	public String name;
	public String owner;
	public String visualization;
	public BasicDBList records;

	public static boolean isOwner(ObjectId spaceId, String email) {
		DBObject query = new BasicDBObject();
		query.put("_id", spaceId);
		query.put("owner", email);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Find the spaces that are owned by the given user.
	 */
	public static List<Space> findOwnedBy(User user) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<Space> spaces = new ArrayList<Space>();
		DBObject query = new BasicDBObject("owner", user.email);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			spaces.add(ModelConversion.mapToModel(Space.class, cur.toMap()));
		}
		// TODO possibly sort by creation timestamp or a specific order field (changeable)
		return spaces;
	}

	/**
	 * Adds a space and returns the error message (null in absence of errors). Also adds the generated id to the space
	 * object.
	 */
	public static String add(Space newSpace) throws IllegalArgumentException, IllegalAccessException {
		if (!spaceWithSameNameExists(newSpace.name, newSpace.owner)) {
			DBObject insert = new BasicDBObject(ModelConversion.modelToMap(Space.class, newSpace));
			WriteResult result = Connection.getCollection(collection).insert(insert);
			newSpace._id = (ObjectId) insert.get("_id");
			return result.getLastError().getErrorMessage();
		} else {
			return "A space with this name already exists.";
		}
	}

	/**
	 * Tries to rename the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String rename(ObjectId spaceId, String newName) {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject foundSpace = Connection.getCollection(collection).findOne(query);
		if (foundSpace == null) {
			return "This space doesn't exist.";
		}
		String owner = (String) foundSpace.get("owner");
		if (!spaceWithSameNameExists(newName, owner)) {
			DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		} else {
			return "A space with this name already exists.";
		}
	}

	/**
	 * Tries to delete the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String delete(ObjectId spaceId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Adds a new record to the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String addRecord(ObjectId spaceId, ObjectId recordId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else if (Space.recordIsInSpace(spaceId, recordId)) {
			return "Record is already in this space.";
		} else {
			DBObject query = new BasicDBObject("_id", spaceId);
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Removes a record from the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String removeRecord(ObjectId spaceId, ObjectId recordId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else if (!Space.recordIsInSpace(spaceId, recordId)) {
			return "Record is not in this space.";
		} else {
			DBObject query = new BasicDBObject("_id", spaceId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("records", recordId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Checks whether a space with the same name already exists for the given owner.
	 */
	private static boolean spaceWithSameNameExists(String name, String owner) {
		DBObject query = new BasicDBObject();
		query.put("name", name);
		query.put("owner", owner);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Checks whether the given record is in the given space.
	 */
	private static boolean recordIsInSpace(ObjectId spaceId, ObjectId recordId) {
		DBObject query = new BasicDBObject();
		query.put("_id", spaceId);
		query.put("records", new BasicDBObject("$in", new ObjectId[] { recordId }));
		return (Connection.getCollection(collection).findOne(query) != null);
	}
	
}
