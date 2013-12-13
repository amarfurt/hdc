package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Record extends Model implements Comparable<Record> {

	private static final String collection = "records";

	public ObjectId app; // app that created the record
	public ObjectId creator; // user that imported the record
	public ObjectId owner; // person the record is about
	public String created; // date + time created
	public String data; // arbitrary data (base64 encoded json string)
	public String name; // used to display a record and for autocompletion
	public String description; // this will be indexed in the search cluster

	@Override
	public int compareTo(Record o) {
		// newest first
		return -this.created.compareTo(o.created);
	}

	public static String getCollection() {
		return collection;
	}

	public static Record find(ObjectId recordId) throws ModelException {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject result = Database.getCollection(collection).findOne(query);
		try {
			return ModelConversion.mapToModel(Record.class, result.toMap());
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
	}

	public static Set<Record> findAll(ObjectId... recordIds) throws ModelException {
		Set<Record> records = new HashSet<Record>();
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$in", recordIds));
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return records;
	}

	/**
	 * Find the records that are owned by the given user.
	 */
	public static Set<Record> findOwnedBy(ObjectId userId) throws ModelException {
		Set<Record> records = new HashSet<Record>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				records.add(ModelConversion.mapToModel(Record.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return records;
	}

	/**
	 * Find all records visible to the given user.
	 */
	public static Set<Record> findVisible(ObjectId userId) throws ModelException {
		// get records of this user
		Set<Record> records = findOwnedBy(userId);

		// get shared records of all circles this user is a member of
		Set<Circle> memberCircles = Circle.findMemberOf(userId);
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
		return records;
	}

	public static Set<ObjectId> getOwnedBy(ObjectId userId) {
		Set<ObjectId> ownedRecordIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			ownedRecordIds.add((ObjectId) result.next().get("_id"));
		}
		return ownedRecordIds;
	}

	public static String getData(ObjectId recordId) {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject projection = new BasicDBObject("data", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("data");
	}

	/**
	 * Checks whether the user with the given email is the creator or owner of the record with the given id.
	 */
	public static boolean isCreatorOrOwner(ObjectId recordId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", recordId);
		DBObject creator = new BasicDBObject("creator", userId);
		DBObject owner = new BasicDBObject("owner", userId);
		query.put("$or", new DBObject[] { creator, owner });
		return (Database.getCollection(collection).findOne(query) != null);
	}

	public static void add(Record newRecord) throws ModelException {
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newRecord));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newRecord._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also index the data for the text search
		try {
			Search.add(newRecord.owner, "record", newRecord._id, newRecord.name, newRecord.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId recordId) throws ModelException {
		// TODO remove from spaces and circles
		DBObject query = new BasicDBObject("_id", recordId);
		WriteResult result = Database.getCollection(collection).remove(query);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
	}

}
