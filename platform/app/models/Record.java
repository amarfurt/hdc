package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.DBObject;

public class Record extends Model implements Comparable<Record> {

	private static final String collection = "records";

	public ObjectId app; // app that created the record
	public ObjectId owner; // person the record is about
	public ObjectId creator; // user that imported the record
	public String created; // date + time created
	public String name; // used to display a record and for autocompletion
	public String description; // this will be indexed in the search cluster
	public DBObject data; // arbitrary json data

	@Override
	public int compareTo(Record other) {
		if (this.created != null && other.created != null) {
			// newest first
			return -this.created.compareTo(other.created);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(collection, properties);
	}

	public static Record get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(Record.class, collection, properties, fields);
	}

	public static Set<Record> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(Record.class, collection, properties, fields);
	}

	public static void set(ObjectId recordId, String field, Object value) throws ModelException {
		Model.set(collection, recordId, field, value);
	}

	public static void add(Record record) throws ModelException {
		Model.insert(collection, record);

		// also index the data for the text search
		try {
			Search.add(record.owner, "record", record._id, record.name, record.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId ownerId, ObjectId recordId) throws ModelException {
		// also remove from search index
		Search.delete(ownerId, "record", recordId);

		// TODO remove from spaces and circles
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("_id", recordId).get());
	}

}
