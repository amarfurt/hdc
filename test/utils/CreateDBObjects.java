package utils;

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.Record;
import models.User;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CreateDBObjects {

	public static ObjectId[] insertUsers(int numUsers) throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = TestConnection.getCollection("users");
		long originalCount = users.count();
		ObjectId[] userIds = new ObjectId[numUsers];
		for (int i = 0; i < numUsers; i++) {
			User user = new User();
			user.email = "test" + (i + 1) + "@example.com";
			user.name = "Test User " + (i + 1);
			user.password = PasswordHash.createHash("secret");
			DBObject userObject = new BasicDBObject(ModelConversion.modelToMap(User.class, user));
			users.insert(userObject);
			userIds[i] = (ObjectId) userObject.get("_id");
		}
		assertEquals(originalCount + numUsers, users.count());
		return userIds;
	}

	public static ObjectId[] insertRecords(ObjectId creator, ObjectId owner, int numRecords)
			throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		long originalCount = records.count();
		ObjectId[] recordIds = new ObjectId[numRecords];
		for (int i = 0; i < numRecords; i++) {
			Record record = new Record();
			record.creator = creator;
			record.owner = owner;
			record.created = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
			record.data = "Random data.";
			record.tags = new BasicDBList();
			DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
			records.insert(recordObject);
			recordIds[i] = (ObjectId) recordObject.get("_id");
		}
		assertEquals(originalCount + numRecords, records.count());
		return recordIds;
	}

}
