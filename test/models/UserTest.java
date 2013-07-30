package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

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
	public void findSuccessTest() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = "secret";
		person.birthday = "2000-01-01";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		User foundUser = User.find(person.email);
		assertEquals("Test User", foundUser.name);
	}

	@Test
	public void findFailureTest() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		Person person = new Person();
		person.email = "test1@example.com";
		person.name = "Test User";
		person.password = "secret";
		person.birthday = "2000-01-01";
		users.insert(new BasicDBObject(ModelConversion.modelToMap(Person.class, person)));
		assertEquals(1, users.count());
		User foundUser = User.find("wrong@example.com");
		assertNull(foundUser);
	}

}
