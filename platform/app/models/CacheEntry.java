package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.collections.ChainedMap;

public class CacheEntry extends Model {

	private static final String collection = "cache";

	public long expires;
	public Set<ObjectId> items;

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(collection, properties);
	}

	public static CacheEntry get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(CacheEntry.class, collection, properties, fields);
	}

	public static void set(ObjectId entryId, String field, Object value) throws ModelException {
		Model.set(collection, entryId, field, value);
	}

	public static void add(CacheEntry entry) throws ModelException {
		Model.insert(collection, entry);
	}

	public static void delete(ObjectId entryId) throws ModelException {
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", entryId).get();
		Model.delete(collection, properties);
	}

}
