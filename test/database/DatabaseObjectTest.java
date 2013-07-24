package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.Person;
import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class DatabaseObjectTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Connection.connectTest();
		Connection.getDB().dropDatabase();
	}

	@After
	public void tearDown() {
		Connection.close();
	}

	@Test
	public void createAndSaveObject() {
		DBCollection users = Connection.getDB().getCollection("users");
		assertEquals(0, users.count());
		users.insert(new BasicDBObject("name", "Test User"));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSaveUser() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = Connection.getDB().getCollection("users");
		assertEquals(0, users.count());
		User user = new User("test.user@example.com", "Test User", "secret");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(User.class, user)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSavePerson() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = Connection.getDB().getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person("test.user@example.com", "Test User", "secret", "2000-01-01");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndRetrievePerson() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection users = Connection.getDB().getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person("test.user@example.com", "Test User", "secret", "2000-01-01");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		Person retrievedPerson = ModelConversion.mapToModel(Person.class, foundObject.toMap());
		assertEquals("Test User", retrievedPerson.name);
	}

}
