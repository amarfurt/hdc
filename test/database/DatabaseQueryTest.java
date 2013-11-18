package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.Map;

import models.Model;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.db.DatabaseObject.Type;
import utils.db.Database;
import utils.db.DatabaseQuery;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class DatabaseQueryTest {

	private static final Type type = Type.USER;

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

	private ObjectId insertTestObject() {
		ObjectId userId = new ObjectId();
		DBObject obj = new BasicDBObject("_id", userId);
		obj.put("email", "test@example.com");
		obj.put("name", "Test User");
		obj.put("password", "Test password");
		WriteResult wr = new DatabaseQuery(type).getCollection().insert(obj);
		assertNull(wr.getLastError().getErrorMessage());
		return userId;
	}

	@Test
	public void exists() {
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("email", "test@example.com");
		assertFalse(dbQuery.exists());
		insertTestObject();
		assertTrue(dbQuery.exists());
	}

	@Test
	public void findOneNotExisting() throws Exception {
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("_id", new ObjectId());
		assertTrue(dbQuery.findOne().isEmpty());
	}

	@Test
	public void findOneNoProjection() throws Exception {
		insertTestObject();
		ObjectId userId = insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("_id", userId);
		Map<String, Object> result = dbQuery.findOne();
		Model model = (Model) result.get("model");
		assertTrue(model instanceof User);
		User user = (User) model;
		assertEquals(userId, user._id);
	}

	@Test
	public void findOneWithProjection() throws Exception {
		insertTestObject();
		ObjectId userId = insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("_id", userId);
		dbQuery.show("name");
		Map<String, Object> result = dbQuery.findOne();
		assertEquals(2, result.size());
		assertEquals("Test User", result.get("name"));
	}

	@Test
	public void findOneWithProjectionSet() throws Exception {
		insertTestObject();
		ObjectId userId = insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("_id", userId);
		dbQuery.show("email");
		dbQuery.show("password");
		Map<String, Object> result = dbQuery.findOne();
		assertEquals(3, result.size());
		assertEquals("test@example.com", result.get("email"));
	}

	@Test
	public void find() throws Exception {
		insertTestObject();
		insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(type);
		dbQuery.query("name", "Test User");
		dbQuery.show("_id");
		Map<ObjectId, Map<String, Object>> result = dbQuery.find();
		assertEquals(3, result.size());
		ObjectId firstId = result.keySet().iterator().next();
		assertEquals(1, result.get(firstId).size());
		assertTrue(result.get(firstId).containsKey("_id"));
	}

}
