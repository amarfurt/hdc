package elasticsearch;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.search.TextSearch;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Fetches the data of all records and indexes it in ElasticSearch.
 */
public class ImportRecords {

	public static void main(String[] args) throws Exception {
		// connect to MongoDB
		start(fakeApplication(fakeGlobal()));
		Connection.connect();

		// connect to ElasticSearch
		TextSearch.connect();

		// waiting for previous operations to finish...
		Thread.sleep(1000);

		// get the all records and group them by owner
		Map<ObjectId, Map<ObjectId, String>> userRecords = new HashMap<ObjectId, Map<ObjectId, String>>();
		DBObject projection = new BasicDBObject("owner", 1);
		projection.put("data", 1);
		DBCursor result = Connection.getCollection("records").find(new BasicDBObject(), projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId ownerId = (ObjectId) cur.get("owner");
			if (!userRecords.containsKey(ownerId)) {
				userRecords.put(ownerId, new HashMap<ObjectId, String>());
			}
			userRecords.get(ownerId).put((ObjectId) cur.get("_id"), (String) cur.get("data"));
		}

		// add the records of each user to ElasticSearch
		for (ObjectId ownerId : userRecords.keySet()) {
			TextSearch.addMultiple(ownerId, "record", userRecords.get(ownerId));
		}

		// disconnect
		Connection.close();
		TextSearch.close();
	}

}
