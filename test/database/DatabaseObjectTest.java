package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.Model;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.db.Database;
import utils.db.DatabaseObject;
import utils.db.DatabaseObject.Type;
import utils.db.DatabaseQuery;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DatabaseObjectTest {

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
	public void getCollection() {
		DatabaseObject dbObj = new DatabaseQuery(Type.USER);
		DBCollection collection = dbObj.getCollection();
		assertNotNull(collection);
		assertEquals("users", collection.getName());
	}

	@Test
	public void modelConversion() throws Exception {
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		DatabaseQuery dbObj = new DatabaseQuery(Type.USER);
		DBObject foundObject = dbObj.getCollection().findOne();
		assertEquals(userId, foundObject.get("_id"));
		Model model = dbObj.toModel(foundObject);
		assertTrue(model instanceof User);
		User user = (User) model;
		assertEquals(userId, user._id);
	}
}
