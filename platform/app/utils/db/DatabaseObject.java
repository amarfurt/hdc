package utils.db;

import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public abstract class DatabaseObject {

	protected String collection;
	protected DBObject query;

	public DatabaseObject(String collection) {
		this.collection = collection;
		query = new BasicDBObject();
	}

	/**
	 * Add an equality selection to the query.
	 */
	public void equals(String key, Object value) {
		query.put(key, value);
	}

	/**
	 * Selects documents where the array 'key' contains the value.
	 */
	public void contains(String key, Object value) {
		query.put(key, value);
	}

	/**
	 * Selects documents where the value of 'key' is in the given set 'values'.
	 */
	public void in(String key, Set<Object> values) {
		query.put(key, new BasicDBObject("$in", values.toArray()));
	}

	/**
	 * Selects documents where the value of 'key' is not in the given set 'values'.
	 */
	public void notIn(String key, Set<Object> values) {
		query.put(key, new BasicDBObject("$nin", values.toArray()));
	}

}
