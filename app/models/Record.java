package models;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBObject;
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

	public static boolean isCreatorOrOwner(ObjectId recordId, String email) {
		DBObject query = new BasicDBObject();
		query.put("_id", recordId);
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
