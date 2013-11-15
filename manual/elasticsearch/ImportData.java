package elasticsearch;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Circle;
import models.Message;
import models.Record;
import models.Space;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.search.TextSearch;
import utils.search.TextSearch.Type;

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
		Connection.connect();

		// connect to ElasticSearch
		TextSearch.connect();
		TextSearch.initialize();

		// waiting for previous operations to finish...
		Thread.sleep(1000);
		System.out.println("done.");

		// users
		System.out.print("Importing users...");
		Map<ObjectId, String> users = new HashMap<ObjectId, String>();
		DBObject query = new BasicDBObject();
		DBObject projection = new BasicDBObject("email", 1);
		projection.put("name", 1);
		DBCursor result = Connection.getCollection("users").find(query, projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId userId = (ObjectId) cur.get("_id");
			String email = (String) cur.get("email");
			String name = (String) cur.get("name");
			TextSearch.addPublic(Type.USER, userId, email + " " + name);
			users.put(userId, name);
		}

		// waiting for previous operations to finish...
		Thread.sleep(1000);
		System.out.println("done.");

		// for each user: add all the data
		for (ObjectId userId : users.keySet()) {
			System.out.print("Importing personal data for user '" + users.get(userId) + "'...");
			// messages
			List<Message> messages = Message.findSentTo(userId);
			Map<ObjectId, String> data = new HashMap<ObjectId, String>();
			for (Message message : messages) {
				data.put(message._id, message.title + ": " + message.content);
			}
			TextSearch.addMultiple(userId, "message", data);

			// spaces
			List<Space> spaces = Space.findOwnedBy(userId);
			data.clear();
			for (Space space : spaces) {
				data.put(space._id, space.name);
			}
			TextSearch.addMultiple(userId, "space", data);

			// circles
			List<Circle> circles = Circle.findOwnedBy(userId);
			data.clear();
			for (Circle circle : circles) {
				data.put(circle._id, circle.name);
			}
			TextSearch.addMultiple(userId, "circle", data);

			// records
			List<Record> records = Record.findOwnedBy(userId);
			data.clear();
			for (Record record : records) {
				data.put(record._id, record.description);
			}
			TextSearch.addMultiple(userId, "record", data);
			System.out.println("done.");
		}

		// TODO apps

		// visualizations
		System.out.print("Importing visualizations...");
		query = new BasicDBObject();
		projection = new BasicDBObject("name", 1);
		projection.put("description", 1);
		result = Connection.getCollection("visualizations").find(query, projection);
		while (result.hasNext()) {
			DBObject cur = result.next();
			ObjectId visualizationId = (ObjectId) cur.get("_id");
			String name = (String) cur.get("name");
			String description = (String) cur.get("description");
			TextSearch.addPublic(Type.VISUALIZATION, visualizationId, name + ": " + description);
		}
		System.out.println("done.");

		// disconnect
		Connection.close();
		TextSearch.close();
		System.out.println("Finished.");
	}

}
