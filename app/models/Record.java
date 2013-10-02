package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class Record implements Comparable<Record> {

	private static final String collection = "records";

	public ObjectId _id;
	public String creator; // any user
	public String owner; // any user of type person
	public String created; // date + time created
	public String data;
	public BasicDBList tags;

	@Override
	public int compareTo(Record o) {
		// newest first
		return -this.created.compareTo(o.created);
	}

	public static String getCollection() {
		return collection;
	}

	/**
	 * Return the first few words of the data up to a maximum of 40 characters.
	 */
	public static String dataToString(String data) {
		int maxChars = 40;
		if (data.length() < maxChars) {
			return data;
		} else {
			int lastIndex = data.lastIndexOf(" ", maxChars);
			return data.substring(0, lastIndex + 1) + "...";
		}
	}

	public static Record find(ObjectId recordId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
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
	public static List<Record> findOwnedBy(String email) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Record> records = new ArrayList<Record>();
		DBObject query = new BasicDBObject("owner", email);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
		}
		Collections.sort(records);
		return records;
	}

	/**
	 * Find all records shared with the given user (including own records).
	 */
	public static List<Record> findSharedWith(String email) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		// get records of this user
		List<Record> records = findOwnedBy(email);

		// get shared records of all circles this user is a member of
		List<Circle> memberCircles = Circle.findMemberOf(email);
		Set<ObjectId> sharedRecords = new HashSet<ObjectId>();
		for (Circle circle : memberCircles) {
			for (Object recordId : circle.shared) {
				sharedRecords.add((ObjectId) recordId);
			}
		}

		// add all of these records (there is no intersection because only owners can share records)
		for (ObjectId recordId : sharedRecords) {
			records.add(find(recordId));
		}

		Collections.sort(records);
		return records;
	}

	/**
	 * Find the records specified by the given list of ids in the given list.
	 */
	public static List<Record> findInList(List<Object> recordIds, List<Record> records) {
		List<Record> foundRecords = new ArrayList<Record>(records);
		Set<Object> ids = new HashSet<Object>(recordIds);
		Iterator<Record> iterator = foundRecords.iterator();
		while (iterator.hasNext()) {
			if (!ids.contains(iterator.next()._id)) {
				iterator.remove();
			}
		}
		return foundRecords;
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
		// TODO remove from spaces and circles
		DBObject query = new BasicDBObject("_id", recordId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

}
