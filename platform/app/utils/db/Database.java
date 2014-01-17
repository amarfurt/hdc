package utils.db;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import models.Model;

import org.bson.types.ObjectId;

import play.Play;
import utils.collections.CollectionConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

public class Database {

	private static MongoClient mongoClient; // mongo client is already a connection pool
	private static String database; // database currently in use

	/**
	 * Open mongo client.
	 */
	private static void openConnection() {
		String host = Play.application().configuration().getString("mongo.host");
		int port = Play.application().configuration().getInt("mongo.port");
		try {
			mongoClient = new MongoClient(host, port);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Connects to the production database 'healthdata'.
	 */
	public static void connect() {
		openConnection();
		database = Play.application().configuration().getString("mongo.database");
	}

	/**
	 * Connects to the test database 'test'.
	 */
	public static void connectToTest() {
		openConnection();
		database = "test";
	}

	/**
	 * Closes all connections.
	 */
	public static void close() {
		mongoClient.close();
	}

	/**
	 * Sets up the collections and creates all indices.
	 */
	public static void initialize() {
		// TODO
	}

	/**
	 * Drops the database.
	 */
	public static void destroy() {
		getDB().dropDatabase();
	}

	/**
	 * Get a connection to the database in use.
	 */
	private static DB getDB() {
		return mongoClient.getDB(database);
	}

	/**
	 * Gets the specified collection.
	 */
	public static DBCollection getCollection(String collection) {
		return getDB().getCollection(collection);
	}

	/* Database operations */
	/**
	 * Insert a document into the given collection.
	 */
	public static <T extends Model> void insert(String collection, T modelObject) throws DatabaseException {
		DBObject dbObject;
		try {
			dbObject = DatabaseConversion.toDBObject(modelObject);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
		WriteResult writeResult = getCollection(collection).insert(dbObject);
		DatabaseException.throwIfPresent(writeResult.getLastError().getErrorMessage());
	}

	/**
	 * Remove all documents with the given properties from the given collection.
	 */
	public static void delete(String collection, Map<String, ? extends Object> properties) throws DatabaseException {
		DBObject query = toDBObject(properties);
		WriteResult writeResult = getCollection(collection).remove(query);
		DatabaseException.throwIfPresent(writeResult.getLastError().getErrorMessage());
	}

	/**
	 * Check whether a document exists that has the given properties.
	 */
	public static boolean exists(String collection, Map<String, ? extends Object> properties) {
		DBObject query = toDBObject(properties);
		DBObject projection = new BasicDBObject("_id", 1);
		return getCollection(collection).findOne(query, projection) != null;
	}

	/**
	 * Return the given fields of the object that has the given properties.
	 */
	public static <T extends Model> T get(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws DatabaseException {
		DBObject query = toDBObject(properties);
		DBObject projection = toDBObject(fields);
		DBObject dbObject = getCollection(collection).findOne(query, projection);
		try {
			return DatabaseConversion.toModel(modelClass, dbObject);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Return the given fields of all objects that have the given properties.
	 */
	public static <T extends Model> Set<T> getAll(Class<T> modelClass, String collection,
			Map<String, ? extends Object> properties, Set<String> fields) throws DatabaseException {
		DBObject query = toDBObject(properties);
		DBObject projection = toDBObject(fields);
		DBCursor cursor = getCollection(collection).find(query, projection);
		Set<DBObject> dbObjects = CollectionConversion.toSet(cursor);
		try {
			return DatabaseConversion.toModel(modelClass, dbObjects);
		} catch (DatabaseConversionException e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Set the given field of the object with the given id.
	 */
	public static void set(String collection, ObjectId modelId, String field, Object value) throws DatabaseException {
		DBObject query = new BasicDBObject("_id", modelId);
		DBObject update = new BasicDBObject("$set", new BasicDBObject(field, value));
		WriteResult writeResult = getCollection(collection).update(query, update);
		DatabaseException.throwIfPresent(writeResult.getLastError().getErrorMessage());
	}

	/**
	 * Convert the properties map to a database object. If an array is given as the value, use the $in operator.
	 */
	private static DBObject toDBObject(Map<String, ? extends Object> properties) {
		DBObject dbObject = new BasicDBObject();
		for (String key : properties.keySet()) {
			Object property = properties.get(key);
			if (property instanceof Set<?>) {
				dbObject.put(key, new BasicDBObject("$in", ((Set<?>) property).toArray()));
			} else {
				dbObject.put(key, property);
			}
		}
		return dbObject;
	}

	/**
	 * Convert the fields set to a database object. Project to all fields given.
	 */
	private static DBObject toDBObject(Set<String> fields) {
		DBObject dbObject = new BasicDBObject();
		for (String field : fields) {
			dbObject.put(field, 1);
		}
		return dbObject;
	}

}
