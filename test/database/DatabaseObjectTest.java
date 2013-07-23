package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import models.Person;
import models.User;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DatabaseObjectTest {

	private static TestConnection con;
	private static DB db;

	@BeforeClass
	public static void setUp() {
		try {
			// connect to test database
			con = new TestConnection();
			db = con.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void tearDown() {
		con.close();
	}

	@Before
	public void createCollections() {
		db.createCollection("users", null);
	}

	@After
	public void cleanDatabase() {
		db.dropDatabase();
	}

	@Test
	public void createAndSaveObject() {
		DBCollection users = db.getCollection("users");
		assertEquals(0, users.count());
		users.insert(new BasicDBObject("name", "Test User"));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSaveUser() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = db.getCollection("users");
		assertEquals(0, users.count());
		User user = new User("andreas.marfurt@healthbank.ch", "Andreas Marfurt", "secreta");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(User.class, user)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Andreas Marfurt", foundObject.get("name"));
	}
	
	@Test
	public void createAndSavePerson() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = db.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person("andreas.marfurt@healthbank.ch", "Andreas Marfurt", "secreta", "1989-03-05");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Andreas Marfurt", foundObject.get("name"));
	}
	
	@Test
	public void createAndRetrievePerson() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection users = db.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person("andreas.marfurt@healthbank.ch", "Andreas Marfurt", "secreta", "1989-03-05");
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		Person retrievedPerson = ModelConversion.mapToModel(Person.class, foundObject.toMap());
		assertEquals("Andreas Marfurt", retrievedPerson.name);
	}

}
