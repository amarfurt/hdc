package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.db.Database;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class UserTest {

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
	public void findSuccess() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "password";
		User.add(user);
		assertEquals(1, users.count());
		User foundUser = User.find(user._id);
		assertEquals("Test User", foundUser.name);
	}

	@Test
	public void findFailure() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "password";
		User.add(user);
		assertEquals(1, users.count());
		boolean exceptionCaught = false;
		try {
			User.find(new ObjectId());
		} catch (NullPointerException e) {
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
	}

	@Test
	public void add() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "password";
		User.add(user);
		assertEquals(1, users.count());
		DBObject query = new BasicDBObject("_id", user._id);
		assertEquals(user.email, users.findOne(query).get("email"));
	}

	@Test
	public void delete() throws ModelException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		User.delete(userIds[0]);
		assertEquals(0, users.count());
	}

	@Test
	public void makeRecordsVisible() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0]);
		Set<ObjectId> visibleUserIds = new HashSet<ObjectId>();
		visibleUserIds.add(userIds[1]);
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(Arrays.asList(recordIds));
		User.makeRecordsVisible(userIds[0], visibleRecordIds, visibleUserIds);
		DBCollection users = Database.getCollection("users");
		DBObject query = new BasicDBObject("_id", userIds[1]);
		BasicDBList visible = (BasicDBList) users.findOne(query).get("visible");
		assertEquals(1, visible.size());
		BasicDBObject entry = (BasicDBObject) visible.get(0);
		assertEquals(userIds[0], entry.get("owner"));
		BasicDBList records = (BasicDBList) entry.get("records");
		assertEquals(2, records.size());
		assertTrue(records.contains(recordIds[0]));
		assertTrue(records.contains(recordIds[1]));
	}

	@Test
	public void getVisibleRecords() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0]);
		Set<ObjectId> visibleUserIds = new HashSet<ObjectId>();
		visibleUserIds.add(userIds[1]);
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(Arrays.asList(recordIds));
		User.makeRecordsVisible(userIds[0], visibleRecordIds, visibleUserIds);
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userIds[1]);
		Set<ObjectId> users = visibleRecords.keySet();
		assertEquals(1, users.size());
		assertTrue(users.contains(userIds[0]));
		assertTrue(visibleRecords.get(userIds[0]).contains(recordIds[0]));
		assertTrue(visibleRecords.get(userIds[0]).contains(recordIds[1]));
	}

	@Test
	public void addApp() throws ModelException {
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		ObjectId appId = new ObjectId();
		User.addApp(userId, appId);
		DBCollection users = Database.getCollection("users");
		BasicDBList apps = (BasicDBList) users.findOne().get("apps");
		assertEquals(1, apps.size());
		assertTrue(apps.contains(appId));
	}

	@Test
	public void removeVisualization() throws ModelException {
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		ObjectId visualizationId = new ObjectId();
		User.addVisualization(userId, visualizationId);
		DBCollection users = Database.getCollection("users");
		BasicDBList visualizations = (BasicDBList) users.findOne().get("visualizations");
		assertEquals(1, visualizations.size());
		assertTrue(visualizations.contains(visualizationId));
		User.removeVisualization(userId, visualizationId);
		visualizations = (BasicDBList) users.findOne().get("visualizations");
		assertEquals(0, visualizations.size());
	}

	@Test
	public void hasApp() throws ModelException {
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		ObjectId appId = new ObjectId();
		User.addApp(userId, appId);
		assertTrue(User.hasApp(userId, appId));
	}

	@Test
	public void findVisualizations() throws ModelException {
		ObjectId userId = CreateDBObjects.insertUsers(1)[0];
		ObjectId[] visualizationIds = CreateDBObjects.insertVisualizations(2);
		User.addVisualization(userId, visualizationIds[0]);
		Set<Visualization> visualizations = User.findVisualizations(userId);
		assertEquals(1, visualizations.size());
		Visualization visualization = visualizations.iterator().next();
		assertEquals(visualizationIds[0], visualization._id);
		assertNotNull(visualization.name);
	}

}
