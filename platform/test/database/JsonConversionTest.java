package database;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.ModelException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class JsonConversionTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Database.connectToTest();
		Database.destroy();
	}

	@After
	public void tearDown() {
		Database.close();
	}

	@Test
	public void jsonConversion() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		DBObject user = users.findOne();
		System.out.println(JSON.serialize(user));
		System.out.println(((BasicDBObject) user).toString());
	}

	@Test
	public void jsonConversionAll() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		CreateDBObjects.insertUsers(2);
		assertEquals(2, users.count());
		DBCursor cursor = users.find();
		System.out.println(JSON.serialize(cursor));
	}
}
