package utils;

import org.bson.types.ObjectId;

import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class OrderOperations {

	/**
	 * Returns the maximum of the order fields in the given collection.
	 */
	public static int getMax(String collection, ObjectId userId) {
		DBCollection coll = Database.getCollection(collection);
		DBObject query = new BasicDBObject("owner", userId);
		DBObject projection = new BasicDBObject("order", 1);
		DBCursor maxOrder = coll.find(query, projection).sort(new BasicDBObject("order", -1)).limit(1);
		int max = 0;
		if (maxOrder.hasNext()) {
			max = (Integer) maxOrder.next().get("order");
		}
		return max;
	}

	/**
	 * Decrements all order fields from (and including) 'fromLimit' to (and including) 'toLimit' by one. If either one
	 * is zero, only the other condition will be considered.
	 */
	public static String decrement(String collection, ObjectId userId, int fromLimit, int toLimit) {
		// fromLimit is never greater than toLimit
		if (toLimit != 0 && fromLimit > toLimit) {
			int tmp = fromLimit;
			fromLimit = toLimit;
			toLimit = tmp;
		}
		return incOperation(collection, userId, fromLimit, toLimit, -1);
	}

	/**
	 * Increments all order fields from (and including) 'fromLimit' to (and including) 'toLimit' by one. If either one
	 * is zero, only the other condition will be considered.
	 */
	public static String increment(String collection, ObjectId userId, int fromLimit, int toLimit) {
		// fromLimit is never greater than toLimit
		if (toLimit != 0 && fromLimit > toLimit) {
			int tmp = fromLimit;
			fromLimit = toLimit;
			toLimit = tmp;
		}
		return incOperation(collection, userId, fromLimit, toLimit, 1);
	}

	private static String incOperation(String collection, ObjectId userId, int fromLimit, int toLimit, int increment) {
		DBObject query = new BasicDBObject("owner", userId);
		if (fromLimit == 0) {
			query.put("order", new BasicDBObject("$lte", toLimit));
		} else if (toLimit == 0) {
			query.put("order", new BasicDBObject("$gte", fromLimit));
		} else {
			DBObject[] and = { new BasicDBObject("order", new BasicDBObject("$gte", fromLimit)),
					new BasicDBObject("order", new BasicDBObject("$lte", toLimit)) };
			query.put("$and", and);
		}
		DBObject update = new BasicDBObject("$inc", new BasicDBObject("order", increment));
		DBCollection coll = Database.getCollection(collection);
		WriteResult result = coll.updateMulti(query, update);
		return result.getLastError().getErrorMessage();
	}

}
