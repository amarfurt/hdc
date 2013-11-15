package utils;

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.Record;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import controllers.visualizations.routes;

public class CreateDBObjects {

	public static ObjectId[] insertUsers(int numUsers) throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = Connection.getCollection("users");
		long originalCount = users.count();
		ObjectId[] userIds = new ObjectId[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = PasswordHash.createHash("secret");
			user.visible = new BasicDBList();
			user.apps = new BasicDBList();
			user.visualizations = new BasicDBList();
			DBObject userObject = new BasicDBObject(ModelConversion.modelToMap(user));
			users.insert(userObject);
			userIds[i] = (ObjectId) userObject.get("_id");
		}
		assertEquals(originalCount + numUsers, users.count());
		return userIds;
	}

	public static ObjectId[] insertRecords(ObjectId creator, ObjectId owner, int numRecords)
			throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = Connection.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record.creator = creator;
			record.owner = owner;
			record.created = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
			record.data = "{\"data\": \"Test data.\"}";
			record.description = "Test data.";
			DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
			records.insert(recordObject);
			recordIds[i] = (ObjectId) recordObject.get("_id");
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

	public static ObjectId createDeveloperAccount() throws NoSuchAlgorithmException, InvalidKeySpecException,
			IllegalArgumentException, IllegalAccessException {
		DBCollection users = Connection.getCollection("users");
		User user = new User();
		user.email = "developers@hdc.ch";
		user.name = "Health Data Cooperative Developers";
		user.password = PasswordHash.createHash("secret");
		DBObject userObject = new BasicDBObject(ModelConversion.modelToMap(user));
		users.insert(userObject);
		return (ObjectId) userObject.get("_id");
	}

	public static void createDefaultVisualization(ObjectId developerId) throws IllegalArgumentException,
			IllegalAccessException {
		DBCollection visualizations = Connection.getCollection("visualizations");
		Visualization visualization = new Visualization();
		visualization.creator = developerId;
		visualization.name = Visualization.getDefaultVisualization();
		visualization.description = "Default record list implementation.";
		visualization.url = routes.RecordList.load().url();
		visualizations.insert(new BasicDBObject(ModelConversion.modelToMap(visualization)));
	}

}
