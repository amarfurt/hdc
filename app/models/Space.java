package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.OrderOperations;
import utils.db.Database;
import utils.search.Search;
import utils.search.SearchException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Space extends Model implements Comparable<Space> {

	private static final String collection = "spaces";

	public String name;
	public ObjectId owner;
	public ObjectId visualization;
	public int order;
	public BasicDBList records;

	@Override
	public int compareTo(Space o) {
		return this.order - o.order;
	}

	public static boolean exists(ObjectId ownerId, String name) {
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("name", name);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static boolean exists(ObjectId ownerId, ObjectId spaceId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		query.put("owner", ownerId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	private static int getOrder(ObjectId spaceId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject projection = new BasicDBObject("order", 1);
		return (Integer) Database.getCollection(collection).findOne(query, projection).get("order");
	}

	public static ObjectId getVisualizationId(ObjectId spaceId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		query.put("owner", userId);
		DBObject projection = new BasicDBObject("visualization", 1);
		return (ObjectId) Database.getCollection(collection).findOne(query, projection).get("visualization");
	}

	public static Set<ObjectId> getRecords(ObjectId spaceId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject projection = new BasicDBObject("records", 1);
		BasicDBList records = (BasicDBList) Database.getCollection(collection).findOne(query, projection)
				.get("records");
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (Object recordId : records) {
			recordIds.add((ObjectId) recordId);
		}
		return recordIds;
	}

	/**
	 * Find the spaces that are owned by the given user.
	 */
	public static Set<Space> findOwnedBy(ObjectId userId) throws ModelException {
		Set<Space> spaces = new HashSet<Space>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				spaces.add(ModelConversion.mapToModel(Space.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return spaces;
	}

	/**
	 * Find the spaces that contain the given record.
	 */
	public static Set<ObjectId> findWithRecord(ObjectId recordId, ObjectId userId) {
		Set<ObjectId> spaces = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		query.put("records", recordId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			spaces.add((ObjectId) result.next().get("_id"));
		}
		return spaces;
	}

	public static void add(Space newSpace) throws ModelException {
		newSpace.order = OrderOperations.getMax(collection, newSpace.owner) + 1;
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newSpace));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newSpace._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also add this space to the user's search index
		try {
			Search.add(newSpace.owner, "space", newSpace._id, newSpace.name);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void rename(ObjectId ownerId, ObjectId spaceId, String newName) throws ModelException {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// update search index
		try {
			Search.update(ownerId, "space", spaceId, newName);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId ownerId, ObjectId spaceId) throws ModelException {
		// find order first
		DBObject query = new BasicDBObject("_id", spaceId);
		int order = getOrder(spaceId);

		// remove space
		WriteResult result = Database.getCollection(collection).remove(query);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// decrement all order fields greater than the removed space
		ModelException.throwIfPresent(OrderOperations.decrement(collection, ownerId, order, 0));

		// remove from search index
		Search.delete(ownerId, "space", spaceId);
	}

	/**
	 * Adds a new record to the space with the given id.
	 */
	public static void addRecord(ObjectId spaceId, ObjectId recordId) throws ModelException {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
	}

	/**
	 * Removes a record from the space with the given id.
	 */
	public static void removeRecord(ObjectId spaceId, ObjectId recordId) throws ModelException {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject update = new BasicDBObject("$pull", new BasicDBObject("records", recordId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
	}

	/**
	 * Adds the record to the given spaces of the user (if not already present), and removes it from the user's other
	 * spaces.
	 */
	public static void updateRecords(Set<ObjectId> spaceIds, ObjectId recordId, ObjectId userId) throws ModelException {
		DBObject query = new BasicDBObject("owner", userId);
		query.put("_id", new BasicDBObject("$in", spaceIds.toArray()));
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
		WriteResult result = Database.getCollection(collection).updateMulti(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
		query = new BasicDBObject("owner", userId);
		query.put("_id", new BasicDBObject("$nin", spaceIds.toArray()));
		update = new BasicDBObject("$pull", new BasicDBObject("records", recordId));
		result = Database.getCollection(collection).updateMulti(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
	}

}
