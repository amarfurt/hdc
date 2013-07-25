package database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.Circle;
import models.Message;
import models.Person;
import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DatabaseObjectTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		TestConnection.connectTest();
		TestConnection.dropDatabase();
	}

	@After
	public void tearDown() {
		TestConnection.close();
	}

	@Test
	public void createAndSaveObject() {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		users.insert(new BasicDBObject("name", "Test User"));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSaveUser() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "secret";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(User.class, user)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndSavePerson() throws IllegalArgumentException, IllegalAccessException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = "secret";
		person.birthday = "2000-01-01";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		assertTrue(foundObject.containsField("name"));
		assertEquals("Test User", foundObject.get("name"));
	}

	@Test
	public void createAndRetrievePerson() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = "secret";
		person.birthday = "2000-01-01";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		DBObject foundObject = users.findOne();
		Person retrievedPerson = ModelConversion.mapToModel(Person.class, foundObject.toMap());
		assertEquals("Test User", retrievedPerson.name);
	}
	
	@Test
	public void createAndRetrieveMessage() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection messages = TestConnection.getCollection("messages");
		assertEquals(0, messages.count());
		Message message = new Message();
		message.sender = "test1@example.com";
		message.receiver = "test2@example.com";
		message.datetime = "2000-01-01-120000Z";
		message.title = "Test";
		message.content = "This is a test message.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(Message.class, message)));
		assertEquals(1, messages.count());
		DBObject foundObject = messages.findOne();
		Message retrievedMessage = ModelConversion.mapToModel(Message.class, foundObject.toMap());
		assertEquals("Test", retrievedMessage.title);
	}
	
	@Test
	public void createAndRetrieveCircle() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Family";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.members.add("test2@example.com");
		circle.members.add("test3@example.com");
		circle.members.add("test4@example.com");
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		assertEquals(1, circles.count());
		DBObject foundObject = circles.findOne();
		Circle retrievedCircle = ModelConversion.mapToModel(Circle.class, foundObject.toMap());
		assertEquals("Family", retrievedCircle.name);
	}

}
