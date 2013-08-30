package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class Record {

	private static final String collection = "records";

	public ObjectId _id;
	public String creator; // any user
	public String owner; // any user of type person
	public String data;

	public static Record find(ObjectId recordId) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject result = Connection.getCollection(collection).findOne(query);
		if (result != null) {
			return ModelConversion.mapToModel(Record.class, result.toMap());
		} else {
			return null;
		}
	}

	/**
	 * Find the records that are owned by the given user.
	 */
	public static List<Record> findOwnedBy(User user) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<Record> records = new ArrayList<Record>();
		DBObject query = new BasicDBObject("owner", user.email);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
		}
		return records;
	}

	/**
	 * Find the records that are owned by the given user and not already in the given space.
	 */
	public static List<Record> findNotInSpace(User user, ObjectId spaceId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		// find all records of this user
		Map<ObjectId, Record> records = new HashMap<ObjectId, Record>();
		for (Record record : Record.findOwnedBy(user)) {
			records.put(record._id, record);
		}
		
		// find all records in the given space
		List<ObjectId> recordsInSpace = new ArrayList<ObjectId>();
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject projection = new BasicDBObject("records", 1);
		Object result = Connection.getCollection(Space.collection).findOne(query, projection).get("records");
		if (result instanceof List<?>) { // workaround to avoid warning
			List<?> recordList = (List<?>) result;
			for (Object recordId : recordList) {
				recordsInSpace.add((ObjectId) recordId);
			}
		}
		
		// remove all records already in the space from the map
		for (ObjectId recordId : recordsInSpace) {
			records.remove(recordId);
		}
		return new ArrayList<Record>(records.values());
	}

	/**
	 * Checks whether the user with the given email is the creator or owner of the record with the given id.
	 */
	public static boolean isCreatorOrOwner(ObjectId recordId, String email) {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject creator = new BasicDBObject("creator", email);
		DBObject owner = new BasicDBObject("owner", email);
		query.put("$or", new DBObject[] { creator, owner });
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Adds a record and returns the error message (null in absence of errors). Also adds the generated id to the record
	 * object.
	 */
	public static String add(Record newRecord) throws IllegalArgumentException, IllegalAccessException {
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(Record.class, newRecord));
		WriteResult result = Connection.getCollection(collection).insert(insert);
		newRecord._id = (ObjectId) insert.get("_id");
		return result.getLastError().getErrorMessage();
	}

	/**
	 * Tries to delete the record with the given id and returns the error message (null in absence of errors).
	 */
	public static String delete(ObjectId recordId) {
		DBObject query = new BasicDBObject("_id", recordId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

}
