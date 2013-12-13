package utils;

import static org.junit.Assert.assertEquals;
import models.ModelException;
import models.Record;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import utils.db.Database;

import com.mongodb.DBCollection;

import controllers.visualizations.routes;

public class CreateDBObjects {

	public static ObjectId[] insertUsers(int numUsers) throws ModelException {
		DBCollection users = Database.getCollection("users");
		long originalCount = users.count();
		ObjectId[] userIds = new ObjectId[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = "password";
			User.add(user);
			userIds[i] = user._id;
		}
		assertEquals(originalCount + numUsers, users.count());
		return userIds;
	}

	public static ObjectId[] insertRecords(int numRecords) throws ModelException {
		return insertRecords(numRecords, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner) throws ModelException {
		return insertRecords(numRecords, owner, new ObjectId());
	}

	public static ObjectId[] insertRecords(int numRecords, ObjectId owner, ObjectId creator) throws ModelException {
		DBCollection records = Database.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record.app = new ObjectId();
			record.owner = owner;
			record.creator = creator;
			record.created = DateTimeUtils.getNow();
			record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
			record.name = "Test record";
			record.description = "Test data.";
			Record.add(record);
			recordIds[i] = record._id;
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

	public static ObjectId createDeveloperAccount() throws ModelException {
		DBCollection users = Database.getCollection("users");
		long originalCount = users.count();
		User user = new User();
		user.email = "developers@hdc.ch";
		user.name = "Health Data Cooperative Developers";
		user.password = "password";
		User.add(user);
		assertEquals(originalCount + 1, users.count());
		return user._id;
	}

	public static ObjectId createDefaultVisualization() throws ModelException {
		return createDefaultVisualization(new ObjectId());
	}

	public static ObjectId createDefaultVisualization(ObjectId developerId) throws ModelException {
		DBCollection visualizations = Database.getCollection("visualizations");
		long originalCount = visualizations.count();
		Visualization visualization = new Visualization();
		visualization.creator = developerId;
		visualization.name = Visualization.getDefaultVisualization();
		visualization.description = "Default record list implementation.";
		visualization.url = routes.RecordList.load().url();
		Visualization.add(visualization);
		assertEquals(originalCount + 1, visualizations.count());
		return visualization._id;
	}

}
