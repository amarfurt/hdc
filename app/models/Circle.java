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

public class Circle extends Model implements Comparable<Circle> {

	private static final String collection = "circles";

	public String name;
	public ObjectId owner;
	public int order;
	public BasicDBList members;
	public BasicDBList shared; // records shared with this circle

	@Override
	public int compareTo(Circle other) {
		return this.order - other.order;
	}

	public static boolean exists(ObjectId ownerId, String name) {
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("name", name);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static boolean exists(ObjectId ownerId, ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		query.put("owner", ownerId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	/**
	 * Find the circles that are owned by the given user.
	 */
	public static Set<Circle> findOwnedBy(ObjectId userId) throws ModelException {
		Set<Circle> circles = new HashSet<Circle>();
		DBObject query = new BasicDBObject("owner", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return circles;
	}

	/**
	 * Find the circles this user is a member of.
	 */
	public static Set<Circle> findMemberOf(ObjectId userId) throws ModelException {
		Set<Circle> circles = new HashSet<Circle>();
		DBObject query = new BasicDBObject("members", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				circles.add(ModelConversion.mapToModel(Circle.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return circles;
	}

	/**
	 * Returns a list of all records shared with this circle.
	 */
	public static Set<ObjectId> getShared(ObjectId circleId) {
		Set<ObjectId> sharedRecordIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("shared", 1);
		BasicDBList shared = (BasicDBList) Database.getCollection(collection).findOne(query, projection).get("shared");
		for (Object sharedRecord : shared) {
			sharedRecordIds.add((ObjectId) sharedRecord);
		}
		return sharedRecordIds;
	}

	/**
	 * Returns the ids of the records that are shared with this user.
	 */
	public static Set<ObjectId> getSharedWith(ObjectId ownerId, ObjectId userId) {
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("members", userId);
		DBObject projection = new BasicDBObject("shared", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList shared = (BasicDBList) result.next().get("shared");
			for (Object recordId : shared) {
				recordIds.add((ObjectId) recordId);
			}
		}
		return recordIds;
	}

	/**
	 * Returns the ids of the users that this record is shared with.
	 */
	public static Set<ObjectId> getUsersSharedWith(ObjectId ownerId, ObjectId recordId) {
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("shared", recordId);
		DBObject projection = new BasicDBObject("members", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList members = (BasicDBList) result.next().get("members");
			for (Object userId : members) {
				userIds.add((ObjectId) userId);
			}
		}
		return userIds;
	}

	/**
	 * Find the users that the given user has already added to his circles.
	 */
	public static Set<User> findContacts(ObjectId userId) throws ModelException {
		Set<ObjectId> contacts = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		DBObject projection = new BasicDBObject("members", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			BasicDBList members = (BasicDBList) result.next().get("members");
			for (Object member : members) {
				contacts.add((ObjectId) member);
			}
		}
		Set<User> users = new HashSet<User>();
		for (ObjectId contactId : contacts) {
			users.add(User.find(contactId));
		}
		return users;
	}

	/**
	 * Find the circles of the given user that contain the given record.
	 */
	public static Set<ObjectId> findWithRecord(ObjectId recordId, ObjectId userId) {
		Set<ObjectId> circles = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("owner", userId);
		query.put("shared", recordId);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Database.getCollection(collection).find(query, projection);
		while (result.hasNext()) {
			circles.add((ObjectId) result.next().get("_id"));
		}
		return circles;
	}

	/**
	 * Returns the owner of the circle
	 */
	public static ObjectId getOwner(ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("owner", 1);
		return (ObjectId) Database.getCollection(collection).findOne(query, projection).get("owner");
	}

	public static int getOrder(ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("order", 1);
		return (Integer) Database.getCollection(collection).findOne(query, projection).get("order");
	}

	/**
	 * Returns a set with ids of the members of the given circle.
	 */
	public static Set<ObjectId> getMembers(ObjectId circleId) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject projection = new BasicDBObject("members", 1);
		DBObject result = Database.getCollection(collection).findOne(query, projection);
		BasicDBList members = (BasicDBList) result.get("members");
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (Object userId : members) {
			userIds.add((ObjectId) userId);
		}
		return userIds;
	}

	public static void add(Circle newCircle) throws ModelException {
		newCircle.order = OrderOperations.getMax(collection, newCircle.owner) + 1;
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newCircle));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newCircle._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also add this circle to the user's search index
		try {
			Search.add(newCircle.owner, "circle", newCircle._id, newCircle.name);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void rename(ObjectId ownerId, ObjectId circleId, String newName) throws ModelException {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("$set", new BasicDBObject("name", newName));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// update search index
		try {
			Search.update(ownerId, "circle", circleId, newName);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId ownerId, ObjectId circleId) throws ModelException {
		// find order first
		DBObject query = new BasicDBObject("_id", circleId);
		int order = getOrder(circleId);

		// remove circle
		WriteResult result = Database.getCollection(collection).remove(query);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// decrement all order fields greater than the removed circle
		ModelException.throwIfPresent(OrderOperations.decrement(collection, ownerId, order, 0));

		// remove from search index
		Search.delete(ownerId, "circle", circleId);
	}

	/**
	 * Adds a member to the given circle with the given id.
	 */
	public static void addMembers(ObjectId ownerId, ObjectId circleId, Set<ObjectId> userIds) throws ModelException {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("members", new BasicDBObject("$each",
				userIds.toArray())));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also add all the records shared with this circle to the visible records of the newly added member
		Set<ObjectId> recordIds = Circle.getShared(circleId);
		User.makeRecordsVisible(ownerId, recordIds, userIds);
	}

	/**
	 * Removes a member from the circle with the given id.
	 */
	public static void removeMember(ObjectId ownerId, ObjectId circleId, ObjectId userId) throws ModelException {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("$pull", new BasicDBObject("members", userId));
		WriteResult result = Database.getCollection(collection).update(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also remove the records shared with this circle from the visible records of the removed member
		// (that are not shared with this user via another circle)
		Set<ObjectId> removedRecordIds = Circle.getShared(circleId);

		// get the records that are still shared with this user (by the same owner)
		Set<ObjectId> sharedRecordIds = Circle.getSharedWith(ownerId, userId);
		for (ObjectId recordId : sharedRecordIds) {
			removedRecordIds.remove(recordId);
		}
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		userIds.add(userId);
		User.makeRecordsInvisible(ownerId, removedRecordIds, userIds);
	}

	public static void startSharingWith(ObjectId ownerId, ObjectId recordId, Set<ObjectId> circleIds)
			throws ModelException {
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("_id", new BasicDBObject("$in", circleIds.toArray()));
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("shared", recordId));
		WriteResult result = Database.getCollection(collection).updateMulti(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also add this record to the visible records of all members of the given circles
		Set<ObjectId> recordIds = new HashSet<ObjectId>(1);
		recordIds.add(recordId);
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (ObjectId circleId : circleIds) {
			userIds.addAll(Circle.getMembers(circleId));
		}
		User.makeRecordsVisible(ownerId, recordIds, userIds);
	}

	public static void stopSharingWith(ObjectId ownerId, ObjectId recordId, Set<ObjectId> circleIds)
			throws ModelException {
		DBObject query = new BasicDBObject("owner", ownerId);
		query.put("_id", new BasicDBObject("$in", circleIds.toArray()));
		DBObject update = new BasicDBObject("$pull", new BasicDBObject("shared", recordId));
		WriteResult result = Database.getCollection(collection).updateMulti(query, update);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// also remove this record from the visible records of all members of the given circles
		Set<ObjectId> recordIds = new HashSet<ObjectId>(1);
		recordIds.add(recordId);
		Set<ObjectId> invisibleUserIds = new HashSet<ObjectId>();
		for (ObjectId circleId : circleIds) {
			invisibleUserIds.addAll(Circle.getMembers(circleId));
		}

		// remove the users that this record is still shared with via another circle
		Set<ObjectId> stillVisibleUserIds = Circle.getUsersSharedWith(ownerId, recordId);
		invisibleUserIds.removeAll(stillVisibleUserIds);
		User.makeRecordsInvisible(ownerId, recordIds, invisibleUserIds);
	}

}
