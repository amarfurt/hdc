package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.ModelConversion;
import utils.PasswordHash;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class UserTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		TestConnection.connectToTest();
		TestConnection.dropDatabase();
	}

	@After
	public void tearDown() {
		TestConnection.close();
	}

	@Test
	public void findSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = PasswordHash.createHash("secret");
		person.birthday = "2000-01-01";
		DBObject personObject = new BasicDBObject(ModelConversion.modelToMap(person));
		users.insert(personObject);
		assertEquals(1, users.count());
		User foundUser = User.find((ObjectId) personObject.get("_id"));
		assertEquals("Test User", foundUser.name);
	}

	@Test
	public void findFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = PasswordHash.createHash("secret");
		person.birthday = "2000-01-01";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(person)));
		assertEquals(1, users.count());
		User foundUser = User.find(new ObjectId());
		assertNull(foundUser);
	}

	@Test
	public void add() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "secret";
		assertNull(User.add(user));
		assertEquals(1, users.count());
		assertEquals(user.email, users.findOne().get("email"));
	}

	@Test
	public void addSameEmail() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
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
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertEquals(1, users.count());
		assertNull(User.remove(userIds[0]));
		assertEquals(0, users.count());
	}

	@Test
	public void removeNotExisting() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		assertFalse("new@example.com".equals(userIds[0]));
		assertEquals(1, users.count());
		assertEquals("No user with this id exists.", User.remove(new ObjectId()));
		assertEquals(1, users.count());
	}

}
