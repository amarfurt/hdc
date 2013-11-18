package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.LoadData;
import utils.ModelConversion;
import utils.PasswordHash;
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
	}

	@After
	public void tearDown() {
		Database.close();
	}

	@Test
	public void findSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = PasswordHash.createHash("secret");
		DBObject userObject = new BasicDBObject(ModelConversion.modelToMap(user));
		users.insert(userObject);
		assertEquals(1, users.count());
		User foundUser = User.find((ObjectId) userObject.get("_id"));
		assertEquals("Test User", foundUser.name);
	}

	@Test
	public void findFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = PasswordHash.createHash("secret");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(user)));
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
	public void add() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException, ElasticSearchException, IOException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		LoadData.createDefaultVisualization();
		assertEquals(1, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "secret";
		assertNull(User.add(user));
		assertEquals(2, users.count());
		DBObject query = new BasicDBObject("_id", user._id);
		assertEquals(user.email, users.findOne(query).get("email"));
	}

	@Test
	public void addSameEmail() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException, ElasticSearchException, IOException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "secret";
		assertEquals("A user with this email address already exists.", User.add(user));
		assertEquals(1, users.count());
	}

	@Test
	public void remove() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		assertNull(User.remove(userIds[0]));
		assertEquals(0, users.count());
	}

	@Test
	public void removeNotExisting() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		DBCollection users = Database.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertFalse("new@example.com".equals(userIds[0]));
		assertEquals(1, users.count());
		assertEquals("No user with this id exists.", User.remove(new ObjectId()));
		assertEquals(1, users.count());
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
