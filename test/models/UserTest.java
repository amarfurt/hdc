package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.LoadData;
import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class UserTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Database.connectToTest();
		Database.destroy();
		try {
			CreateDBObjects.createDefaultVisualization();
		} catch (ModelException e) {
			Assert.fail();
		}
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
		LoadData.createDefaultVisualization();
		assertEquals(1, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "password";
		User.add(user);
		assertEquals(2, users.count());
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
	public void makeRecordsVisible() {
		// TODO
	}

	@Test
	public void getVisibleRecords() {
		// TODO
	}

	@Test
	public void addVisualization() {
		// TODO
	}

	@Test
	public void removeVisualization() {
		// TODO
	}

	@Test
	public void hasVisualization() {
		// TODO
	}

	@Test
	public void findVisualizations() {
		// TODO
	}

}
