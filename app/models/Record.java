package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;

import utils.Connection;
import utils.ModelConversion;
import utils.search.TextSearch;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Record extends Model implements Comparable<Record> {

	private static final String collection = "records";

	public ObjectId creator; // any user
	public ObjectId owner; // any user of type person
	public String created; // date + time created
	public String data; // arbitrary data (base64 encoded json string)
	public String description; // this will be indexed in the search cluster

	@Override
	public int compareTo(Record o) {
		// newest first
		return -this.created.compareTo(o.created);
	}

	public static String getCollection() {
		return collection;
	}

	public static Record find(ObjectId recordId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject result = Connection.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(Record.class, result.toMap());
	}

	public static Set<Record> findAll(ObjectId... recordIds) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		Set<Record> records = new HashSet<Record>();
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", recordIds));
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
		}
		return records;
	}

	/**
	 * Find the records that are owned by the given user.
	 */
	public static List<Record> findOwnedBy(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Record> records = new ArrayList<Record>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
		}
		Collections.sort(records);
		return records;
	}

	/**
	 * Find all records visible to the given user.
	 */
	public static List<Record> findVisible(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		// get records of this user
		List<Record> records = findOwnedBy(userId);

		// get shared records of all circles this user is a member of
		List<Circle> memberCircles = Circle.findMemberOf(userId);
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

	public static Set<ObjectId> getOwnedBy(ObjectId userId) {
		Set<ObjectId> ownedRecordIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			ownedRecordIds.add((ObjectId) result.next().get("_id"));
		}
		return ownedRecordIds;
	}

	public static Set<ObjectId> getVisible(ObjectId userId) {
		// get all owned records
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		visibleRecordIds.addAll(getOwnedBy(userId));

		// get all records that are shared with this user
		visibleRecordIds.addAll(Circle.getSharedWith(userId));
		return visibleRecordIds;
	}

	/**
	 * Checks whether the user with the given email is the creator or owner of the record with the given id.
	 */
	public static boolean isCreatorOrOwner(ObjectId recordId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject creator = new BasicDBObject("creator", userId);
		DBObject owner = new BasicDBObject("owner", userId);
		query.put("$or", new DBObject[] { creator, owner });
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Adds a record and returns the error message (null in absence of errors). Also adds the generated id to the record
	 * object.
	 */
	public static String add(Record newRecord) throws IllegalArgumentException, IllegalAccessException,
			ElasticSearchException, IOException {
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newRecord));
		WriteResult result = Connection.getCollection(collection).insert(insert);
		newRecord._id = (ObjectId) insert.get("_id");
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// also index the data for the text search
		TextSearch.add(newRecord.owner, "record", newRecord._id, newRecord.description);
		return null;
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
