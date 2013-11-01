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
 * Fetches the data of all records and indexes it in elasticsearch.
 */
public class CreateIndexFromRecords {

	public static void main(String[] args) throws Exception {
		// connect to mongo db
		start(fakeApplication(fakeGlobal()));
		Connection.connect();

		// connect to and clear elasticsearch
		TextSearch.connect();
		TextSearch.clearIndex();
		TextSearch.createIndex();
		
		// waiting for previous operations to finish...
		Thread.sleep(1000);

		// get the data of all records and add it to elasticsearch
		Map<ObjectId, String> data = new HashMap<ObjectId, String>();
		DBCursor result = Connection.getCollection("records").find(new BasicDBObject(), new BasicDBObject("data", 1));
		while (result.hasNext()) {
			DBObject cur = result.next();
			data.put((ObjectId) cur.get("_id"), (String) cur.get("data"));
		}
		TextSearch.addMultiple(data);

		// disconnect
		Connection.close();
		TextSearch.close();
	}

}
