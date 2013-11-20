package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.List;
import java.util.Map;

import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.db.Database;
import utils.db.DatabaseQuery;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class DatabaseQueryTest {

	private static final String collection = "users";

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
		WriteResult wr = Database.getCollection(collection).insert(obj);
		assertNull(wr.getLastError().getErrorMessage());
		return userId;
	}

	@Test
	public void exists() {
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("email", "test@example.com");
		assertFalse(dbQuery.exists());
		insertTestObject();
		assertTrue(dbQuery.exists());
	}

	@Test
	public void findOneNotExisting() throws Exception {
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("_id", new ObjectId());
		assertTrue(dbQuery.findOne().isEmpty());
	}

	@Test
	public void findOneNoProjection() throws Exception {
		insertTestObject();
		ObjectId userId = insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("_id", userId);
		Map<String, Object> result = dbQuery.findOne();
		User user = ModelConversion.mapToModel(User.class, result);
		assertEquals(userId, user._id);
	}

	@Test
	public void findOneWithProjection() throws Exception {
		insertTestObject();
		ObjectId userId = insertTestObject();
		insertTestObject();
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("_id", userId);
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
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("_id", userId);
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
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("name", "Test User");
		dbQuery.show("_id");
		List<Map<String, Object>> result = dbQuery.find();
		assertEquals(3, result.size());
		assertEquals(1, result.get(0).size());
		assertTrue(result.get(0).containsKey("_id"));
	}

}
