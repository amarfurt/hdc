package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;

import utils.Connection;
import utils.ModelConversion;
import utils.OrderOperations;
import utils.search.TextSearch;

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

	@Override
	public String toString() {
		return name;
	}

	public static boolean isOwner(ObjectId spaceId, ObjectId userId) {
		DBObject query = new BasicDBObject();
		query.put("_id", spaceId);
		query.put("owner", userId);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	public static ObjectId getVisualizationId(ObjectId spaceId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		query.put("owner", userId);
		DBObject projection = new BasicDBObject("visualization", 1);
		return (ObjectId) Connection.getCollection(collection).findOne(query, projection).get("visualization");
	}

	/**
	 * Find the spaces that are owned by the given user.
	 */
	public static List<Space> findOwnedBy(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Space> spaces = new ArrayList<Space>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			spaces.add(ModelConversion.mapToModel(Space.class, cur.toMap()));
		}
		// sort by order field
		Collections.sort(spaces);
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
		DBCursor result = Connection.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			spaces.add((ObjectId) result.next().get("_id"));
		}
		return spaces;
	}

	/**
	 * Adds a space and returns the error message (null in absence of errors). Also adds the generated id to the space
	 * object.
	 */
	public static String add(Space newSpace) throws IllegalArgumentException, IllegalAccessException,
			ElasticSearchException, IOException {
		if (!spaceWithSameNameExists(newSpace.name, newSpace.owner)) {
			newSpace.order = OrderOperations.getMax(collection, newSpace.owner) + 1;
			DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newSpace));
			WriteResult result = Connection.getCollection(collection).insert(insert);
			newSpace._id = (ObjectId) insert.get("_id");
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}

			// also add this space to the user's search index
			TextSearch.add(newSpace.owner, "space", newSpace._id, newSpace.name);
			return null;
		} else {
			return "A space with this name already exists.";
		}
	}

	/**
	 * Tries to rename the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String rename(ObjectId spaceId, String newName) {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject foundSpace = Connection.getCollection(collection).findOne(query);
		if (foundSpace == null) {
			return "This space doesn't exist.";
		}
		ObjectId ownerId = (ObjectId) foundSpace.get("owner");
		if (!spaceWithSameNameExists(newName, ownerId)) {
			DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		} else {
			return "A space with this name already exists.";
		}
	}

	/**
	 * Tries to delete the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String delete(ObjectId spaceId) {
		// find owner and order first
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject space = Connection.getCollection(collection).findOne(query);
		if (space == null) {
			return "No space with this id exists.";
		}
		ObjectId ownerId = (ObjectId) space.get("owner");
		int order = (Integer) space.get("order");

		// remove space
		WriteResult result = Connection.getCollection(collection).remove(query);
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// decrement all order fields greater than the removed space
		return OrderOperations.decrement(collection, ownerId, order, 0);
	}

	/**
	 * Adds a new record to the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String addRecord(ObjectId spaceId, ObjectId recordId) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else if (Space.recordIsInSpace(spaceId, recordId)) {
			return "Record is already in this space.";
		} else {
			DBObject query = new BasicDBObject("_id", spaceId);
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Removes a record from the space with the given id and returns the error message (null in absence of errors).
	 */
	public static String removeRecord(ObjectId spaceId, ObjectId recordId) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else if (!Space.recordIsInSpace(spaceId, recordId)) {
			return "Record is not in this space.";
		} else {
			DBObject query = new BasicDBObject("_id", spaceId);
			DBObject update = new BasicDBObject("$pull", new BasicDBObject("records", recordId));
			WriteResult result = Connection.getCollection(collection).update(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	/**
	 * Adds the record to the given spaces of the user (if not already present), and removes it from the user's other
	 * spaces.
	 */
	public static String updateRecords(List<ObjectId> spaceIds, ObjectId recordId, ObjectId userId)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		if (Record.find(recordId) == null) {
			return "Record doesn't exist.";
		} else {
			DBObject query = new BasicDBObject("owner", userId);
			query.put("_id", new BasicDBObject("$in", spaceIds.toArray()));
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
			WriteResult result = Connection.getCollection(collection).updateMulti(query, update);
			String errorMessage = result.getLastError().getErrorMessage();
			if (errorMessage != null) {
				return errorMessage;
			}
			query = new BasicDBObject("owner", userId);
			query.put("_id", new BasicDBObject("$nin", spaceIds.toArray()));
			update = new BasicDBObject("$pull", new BasicDBObject("records", recordId));
			result = Connection.getCollection(collection).updateMulti(query, update);
			return result.getLastError().getErrorMessage();
		}
	}

	public static Set<ObjectId> getRecords(ObjectId spaceId) {
		DBObject query = new BasicDBObject("_id", spaceId);
		DBObject projection = new BasicDBObject("records", 1);
		BasicDBList records = (BasicDBList) Connection.getCollection(collection).findOne(query, projection)
				.get("records");
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (Object recordId : records) {
			recordIds.add((ObjectId) recordId);
		}
		return recordIds;
	}

	/**
	 * Checks whether a space with the same name already exists for the given owner.
	 */
	private static boolean spaceWithSameNameExists(String name, ObjectId userId) {
		DBObject query = new BasicDBObject();
		query.put("name", name);
		query.put("owner", userId);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	/**
	 * Checks whether the given record is in the given space.
	 */
	private static boolean recordIsInSpace(ObjectId spaceId, ObjectId recordId) {
		DBObject query = new BasicDBObject();
		query.put("_id", spaceId);
		query.put("records", new BasicDBObject("$in", new ObjectId[] { recordId }));
		return (Connection.getCollection(collection).findOne(query) != null);
	}

}
