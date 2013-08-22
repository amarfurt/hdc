package utils;

import static org.junit.Assert.assertEquals;

import org.bson.types.ObjectId;

import models.Record;
import models.User;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class CreateDBObjects {

	public static String[] insertUsers(int numUsers) throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		String[] emailAddresses = new String[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = "secret";
			users.insert(new BasicDBObject(ModelConversion.modelToMap(User.class, user)));
			emailAddresses[i] = user.email;
		}
		return emailAddresses;
	}

	public static ObjectId[] insertRecords(String creator, String owner, int numRecords) throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record.creator = creator;
			record.owner = owner;
			record.data = "Random data.";
			records.insert(new BasicDBObject(ModelConversion.modelToMap(Record.class, record)));
			recordIds[i] = record._id;
		}
		return recordIds;
	}

}
