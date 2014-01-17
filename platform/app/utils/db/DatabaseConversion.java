package utils.db;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import models.Model;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class DatabaseConversion {

	/**
	 * Turns a model into a database object.
	 */
	public static <T extends Model> DBObject toDBObject(T modelObject) throws DatabaseConversionException {
		DBObject dbObject = new BasicDBObject();
		for (Field field : modelObject.getClass().getFields()) {
			try {
				dbObject.put(field.getName(), field.get(modelObject));
			} catch (IllegalArgumentException e) {
				throw new DatabaseConversionException(e);
			} catch (IllegalAccessException e) {
				throw new DatabaseConversionException(e);
			}
		}
		return dbObject;
	}

	/**
	 * Converts a database object back into a model.
	 */
	public static <T extends Model> T toModel(Class<T> modelClass, DBObject dbObject) throws DatabaseConversionException {
		T modelObject;
		try {
			modelObject = modelClass.newInstance();
		} catch (InstantiationException e) {
			throw new DatabaseConversionException(e);
		} catch (IllegalAccessException e) {
			throw new DatabaseConversionException(e);
		}
		for (Field field : modelClass.getFields()) {
			if (dbObject.keySet().contains(field.getName())) {
				try {
					field.set(modelObject, dbObject.get(field.getName()));
				} catch (IllegalArgumentException e) {
					throw new DatabaseConversionException(e);
				} catch (IllegalAccessException e) {
					throw new DatabaseConversionException(e);
				}
			}
		}
		return modelObject;
	}

	/**
	 * Converts a set of database objects to models.
	 */
	public static <T extends Model> Set<T> toModel(Class<T> modelClass, Set<DBObject> dbObjects)
			throws DatabaseConversionException {
		Set<T> models = new HashSet<T>();
		for (DBObject dbObject : dbObjects) {
			models.add(toModel(modelClass, dbObject));
		}
		return models;
	}
}
