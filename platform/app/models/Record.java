package models;

import java.util.HashSet;
import java.util.Map;
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
	public ObjectId owner; // person the record is about
	public ObjectId creator; // user that imported the record
	public String created; // date + time created
	public String data; // arbitrary string data
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

	public static boolean exists(ObjectId ownerId, ObjectId recordId) {
		DBObject query = new BasicDBObject("_id", recordId);
		query.put("owner", ownerId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
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
	 * Find all records visible to the given user. TODO Very expensive operation, load in chunks.
	 */
	public static Set<Record> findVisible(ObjectId ownerId) throws ModelException {
		Set<Record> records = findOwnedBy(ownerId);
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(ownerId);
		for (ObjectId userId : visibleRecords.keySet()) {
			for (ObjectId recordId : visibleRecords.get(userId)) {
				records.add(Record.find(recordId));
			}
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
