package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.db.Database;
import utils.db.DatabaseException;
import utils.db.DatabaseQuery;
import utils.db.DatabaseUpdate;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class DatabaseUpdateTest {

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
		obj.put("visible", new BasicDBList());
		obj.put("apps", new BasicDBList());
		WriteResult wr = Database.getCollection(collection).insert(obj);
		assertNull(wr.getLastError().getErrorMessage());
		return userId;
	}

	private Map<String, Object> getObject(ObjectId id, String... fields) {
		DatabaseQuery dbQuery = new DatabaseQuery(collection);
		dbQuery.equals("_id", id);
		for (String field : fields) {
			dbQuery.show(field);
		}
		Map<String, Object> result = null;
		try {
			result = dbQuery.findOne();
		} catch (Exception e) {
			fail("Getting the object failed: " + e.getMessage());
		}
		return result;
	}

	@Test
	public void set() throws DatabaseException {
		String newName = "Baptized Test User";
		ObjectId userId = insertTestObject();
		DatabaseUpdate dbUpdate = new DatabaseUpdate(collection);
		dbUpdate.equals("_id", userId);
		dbUpdate.set("name", newName);
		dbUpdate.execute();
		assertEquals(newName, getObject(userId, "name").get("name"));
	}

	@Test
	public void addToSet() throws DatabaseException {
		ObjectId userId = insertTestObject();
		ObjectId appId = new ObjectId();
		DatabaseUpdate dbUpdate = new DatabaseUpdate(collection);
		dbUpdate.equals("_id", userId);
		dbUpdate.addToSet("apps", appId);
		dbUpdate.execute();
		BasicDBList apps = (BasicDBList) getObject(userId, "apps").get("apps");
		assertEquals(1, apps.size());
		assertEquals(appId, apps.get(0));
	}

	@Test
	public void addEachToSet() throws DatabaseException {
		ObjectId userId = insertTestObject();
		DatabaseUpdate dbUpdate = new DatabaseUpdate(collection);
		dbUpdate.equals("_id", userId);
		ObjectId ownerId = new ObjectId();
		DBObject visibleRecords = new BasicDBObject("owner", ownerId);
		visibleRecords.put("records", new BasicDBList());
		dbUpdate.addToSet("visible", visibleRecords);
		dbUpdate.execute();

		DatabaseUpdate dbUpdate2 = new DatabaseUpdate(collection);
		dbUpdate2.equals("_id", userId);
		dbUpdate2.equals("visible.owner", ownerId);
		Set<ObjectId> newRecordIds = new HashSet<ObjectId>();
		ObjectId newRecordId = new ObjectId();
		newRecordIds.add(newRecordId);
		newRecordIds.add(new ObjectId());
		newRecordIds.add(new ObjectId());
		dbUpdate2.addEachToSet("visible.records", newRecordIds);
		dbUpdate2.execute();
		BasicDBList visible = (BasicDBList) getObject(userId, "visible").get("visible");
		assertEquals(1, visible.size());
		BasicDBObject visibleEntry = (BasicDBObject) visible.get(0);
		assertEquals(ownerId, visibleEntry.get("owner"));
		BasicDBList records = (BasicDBList) visibleEntry.get("records");
		assertEquals(3, records.size());
		assertTrue(records.contains(newRecordId));
	}

	@Test
	public void updateMultiple() throws DatabaseException {
		String newName = "Mr. and Mrs. Test User-User";
		ObjectId userId1 = insertTestObject();
		ObjectId userId2 = insertTestObject();
		DatabaseUpdate dbUpdate = new DatabaseUpdate(collection);
		dbUpdate.equals("name", "Test User");
		dbUpdate.set("name", newName);
		dbUpdate.updateMultiple();
		dbUpdate.execute();
		assertEquals(newName, getObject(userId1, "name").get("name"));
		assertEquals(newName, getObject(userId2, "name").get("name"));
	}

	@Test
	public void pull() throws DatabaseException {
		ObjectId userId = insertTestObject();
		ObjectId appId = new ObjectId();
		DatabaseUpdate dbUpdate = new DatabaseUpdate(collection);
		dbUpdate.equals("_id", userId);
		dbUpdate.addToSet("apps", appId);
		dbUpdate.execute();
		DatabaseUpdate dbUpdate2 = new DatabaseUpdate(collection);
		dbUpdate2.equals("_id", userId);
		dbUpdate2.pull("apps", appId);
		dbUpdate2.execute();
		BasicDBList apps = (BasicDBList) getObject(userId, "apps").get("apps");
		assertEquals(0, apps.size());
	}
}
