package utils.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class DatabaseQuery extends DatabaseObject {

	private DBObject projection;

	public DatabaseQuery(String collection) {
		super(collection);
		projection = new BasicDBObject();
	}

	/**
	 * Add a projection onto an attribute.
	 */
	public void show(String key) {
		projection.put(key, 1);
	}

	public boolean exists() {
		projection.put("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	/**
	 * Retrieves a unique item from the database.
	 */
	public Map<String, Object> findOne() {
		DBObject dbObject = Database.getCollection(collection).findOne(query, projection);
		if (dbObject == null) {
			return new HashMap<String, Object>();
		} else {
			return extractObject(dbObject);
		}
	}

	private Map<String, Object> extractObject(DBObject dbObject) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : dbObject.keySet()) {
			result.put(key, dbObject.get(key));
		}
		return result;
	}

	/**
	 * Retrieves non-unique items from the database.
	 */
	public List<Map<String, Object>> find() {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		DBCursor cursor = Database.getCollection(collection).find(query, projection);
		while (cursor.hasNext()) {
			result.add(extractObject(cursor.next()));
		}
		return result;
	}

}
