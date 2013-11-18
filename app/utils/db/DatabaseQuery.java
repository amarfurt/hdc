package utils.db;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class DatabaseQuery extends DatabaseObject {

	private DBObject projection;

	public DatabaseQuery(Type type) {
		super(type);
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
		return getCollection().findOne(query, projection) != null;
	}

	/**
	 * Retrieves a unique item from the database.
	 */
	public Map<String, Object> findOne() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject dbObject = getCollection().findOne(query, projection);
		if (dbObject == null) {
			return new HashMap<String, Object>();
		} else {
			return extractObject(dbObject);
		}
	}

	private Map<String, Object> extractObject(DBObject dbObject) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		Map<String, Object> result = new HashMap<String, Object>();
		if (projection.keySet().isEmpty()) {
			result.put("model", toModel(dbObject));
		} else {
			for (String key : dbObject.keySet()) {
				result.put(key, dbObject.get(key));
			}
		}
		return result;
	}

	/**
	 * Retrieves non-unique items from the database.
	 */
	public Map<ObjectId, Map<String, Object>> find() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		Map<ObjectId, Map<String, Object>> result = new HashMap<ObjectId, Map<String, Object>>();
		DBCursor cursor = getCollection().find(query, projection);
		while (cursor.hasNext()) {
			DBObject cur = cursor.next();
			result.put((ObjectId) cur.get("_id"), extractObject(cur));
		}
		return result;
	}

}
