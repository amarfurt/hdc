package elasticsearch;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Message;
import models.Record;
import models.Space;

import org.bson.types.ObjectId;

import utils.db.Database;
import utils.search.Search;
import utils.search.Search.Type;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Fetches the data from MongoDB and indexes it in ElasticSearch.
 */
public class ImportData {

	public static void main(String[] args) throws Exception {
		System.out.print("Connecting...");
		// connect to MongoDB
		start(fakeApplication(fakeGlobal()));
		Database.connect();

		// connect to ElasticSearch
		Search.connect();
		Search.initialize();

		// waiting for previous operations to finish...
		Thread.sleep(1000);
		System.out.println("done.");

		// users
		System.out.print("Importing users...");
		Map<ObjectId, String> users = new HashMap<ObjectId, String>();
		DBObject query = new BasicDBObject();
		DBObject projection = new BasicDBObject("email", 1);
		projection.put("name", 1);
		DBCursor result = Database.getCollection("users").find(query, projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId userId = (ObjectId) cur.get("_id");
			String email = (String) cur.get("email");
			String name = (String) cur.get("name");
			Search.addPublic(Type.USER, userId, name, email);
			users.put(userId, name);
		}

		// waiting for previous operations to finish...
		Thread.sleep(1000);
		System.out.println("done.");

		// for each user: add all the data
		for (ObjectId userId : users.keySet()) {
			System.out.print("Importing personal data for user '" + users.get(userId) + "'...");
			// messages
			Set<Message> messages = Message.findSentTo(userId);
			for (Message message : messages) {
				Search.add(userId, "message", message._id, message.title, message.content);
			}

			// spaces
			Set<Space> spaces = Space.findOwnedBy(userId);
			for (Space space : spaces) {
				Search.add(userId, "space", space._id, space.name);
			}

			// circles
			Set<Circle> circles = Circle.findOwnedBy(userId);
			for (Circle circle : circles) {
				Search.add(userId, "circle", circle._id, circle.name);
			}

			// records
			Set<Record> records = Record.findOwnedBy(userId);
			for (Record record : records) {
				Search.add(userId, "record", record._id, record.name, record.description);
			}
			System.out.println("done.");
		}

		// apps
		System.out.print("Importing apps...");
		query = new BasicDBObject();
		projection = new BasicDBObject("name", 1);
		projection.put("description", 1);
		result = Database.getCollection("apps").find(query, projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId appId = (ObjectId) cur.get("_id");
			String name = (String) cur.get("name");
			String description = (String) cur.get("description");
			Search.addPublic(Type.APP, appId, name, description);
		}
		System.out.println("done.");

		// visualizations
		System.out.print("Importing visualizations...");
		query = new BasicDBObject();
		projection = new BasicDBObject("name", 1);
		projection.put("description", 1);
		result = Database.getCollection("visualizations").find(query, projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId visualizationId = (ObjectId) cur.get("_id");
			String name = (String) cur.get("name");
			String description = (String) cur.get("description");
			Search.addPublic(Type.VISUALIZATION, visualizationId, name, description);
		}
		System.out.println("done.");

		// disconnect
		Database.close();
		Search.close();
		System.out.println("Finished.");
	}

}
