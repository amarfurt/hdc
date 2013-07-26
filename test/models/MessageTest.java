package models;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.net.UnknownHostException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class MessageTest {

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
	public void findSuccessTest() throws IllegalArgumentException, IllegalAccessException, UnknownHostException, InstantiationException {
		DBCollection messages = TestConnection.getCollection("messages");
		assertEquals(0, messages.count());
		Person person = new Person();
		person.email = "test2@example.com";
		Message message = new Message();
		message.sender = "test1@example.com";
		message.receiver = person.email;
		message.datetime = "2000-01-01-000000Z";
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(Message.class, message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(person);
		assertEquals(1, foundMessages.size());
		assertEquals("Title", foundMessages.get(0).title);
	}

	@Test
	public void findFailureTest() throws IllegalArgumentException, IllegalAccessException, UnknownHostException, InstantiationException {
		DBCollection messages = TestConnection.getCollection("messages");
		assertEquals(0, messages.count());
		Person person = new Person();
		person.email = "test1@example.com";
		Message message = new Message();
		message.sender = person.email;
		message.receiver = "test2@example.com";
		message.datetime = "2000-01-01-000000Z";
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(Message.class, message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(person);
		assertEquals(0, foundMessages.size());
	}
}
