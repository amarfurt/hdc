package utils;

import static org.junit.Assert.assertEquals;
import models.Record;
import models.User;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CreateDBObjects {

	public static String[] insertUsers(int numUsers) throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = TestConnection.getCollection("users");
		long originalCount = users.count();
		String[] emailAddresses = new String[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = "secret";
			users.insert(new BasicDBObject(ModelConversion.modelToMap(User.class, user)));
			emailAddresses[i] = user.email;
		}
		assertEquals(originalCount + numUsers, users.count());
		return emailAddresses;
	}

	public static ObjectId[] insertRecords(String creator, String owner, int numRecords) throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record.creator = creator;
			record.owner = owner;
			record.data = "Random data.";
			DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
			records.insert(recordObject);
			recordIds[i] = (ObjectId) recordObject.get("_id");
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

}
