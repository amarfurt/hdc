package utils.db;

import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class DatabaseUpdate extends DatabaseObject {

	private DBObject update;
	private boolean updateMulti;

	public DatabaseUpdate(String collection) {
		super(collection);
	}

	/**
	 * Set an attribute to a given value;
	 */
	public void set(String key, Object value) {
		key = checkNesting(key);
		update = new BasicDBObject("$set", new BasicDBObject(key, value));
	}

	/**
	 * Add a value to a set of values (no update if it is already part of the set).
	 */
	public void addToSet(String key, Object value) {
		key = checkNesting(key);
		update = new BasicDBObject("$addToSet", new BasicDBObject(key, value));
	}

	/**
	 * Add each value in the given set to a set of values.
	 */
	public void addEachToSet(String key, Set<?> values) {
		key = checkNesting(key);
		update = new BasicDBObject("$addToSet", new BasicDBObject(key, new BasicDBObject("$each", values.toArray())));
	}

	/**
	 * Remove a value from a set of values (if it is present).
	 */
	public void pull(String key, Object value) {
		key = checkNesting(key);
		update = new BasicDBObject("$pull", new BasicDBObject(key, value));
	}

	/**
	 * Remove all values of the given set from the set.
	 */
	public void pullAll(String key, Set<Object> values) {
		key = checkNesting(key);
		update = new BasicDBObject("$pullAll", new BasicDBObject(key, values.toArray()));
	}

	/**
	 * Update multiple documents, not just the first one that matches the query.
	 */
	public void updateMultiple() {
		updateMulti = true;
	}

	/**
	 * Execute the update and return the error message (null in absence of errors).
	 */
	public void execute() throws DatabaseException {
		String errorMessage = null;
		if (!updateMulti) {
			errorMessage = Database.getCollection(collection).update(query, update).getLastError().getErrorMessage();
		} else {
			errorMessage = Database.getCollection(collection).updateMulti(query, update).getLastError()
					.getErrorMessage();
		}
		if (errorMessage != null) {
			throw new DatabaseException(errorMessage);
		}
	}

	private static String checkNesting(String key) {
		int lastIndex = key.lastIndexOf(".");
		if (lastIndex != -1) {
			key = checkNesting(key.substring(0, lastIndex)) + ".$." + key.substring(lastIndex + 1);
		}
		return key;
	}

}
