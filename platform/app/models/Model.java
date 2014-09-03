package models;

import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.db.Database;
import utils.db.DatabaseException;

public abstract class Model {

	public ObjectId _id;

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			Model otherModel = (Model) other;
			return _id.equals(otherModel._id);
		}
		return false;
	}

	protected static <T extends Model> void insert(String collection, T modelObject) throws ModelException {
		try {
			Database.insert(collection, modelObject);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static void delete(String collection, Map<String, ? extends Object> properties) throws ModelException {
		try {
			Database.delete(collection, properties);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static boolean exists(String collection, Map<String, ? extends Object> properties) throws ModelException {
		try {
			return Database.exists(collection, properties);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static <T extends Model> T get(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		try {
			return Database.get(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		try {
			return Database.getAll(modelClass, collection, properties, fields);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

	protected static void set(String collection, ObjectId modelId, String field, Object value) throws ModelException {
		try {
			Database.set(collection, modelId, field, value);
		} catch (DatabaseException e) {
			throw new ModelException(e);
		}
	}

}
