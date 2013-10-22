package models;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.List;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.DateTimeUtils;
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
	public void findSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection messages = TestConnection.getCollection("messages");
		assertEquals(0, messages.count());
		Person person = new Person();
		person._id = new ObjectId();
		Message message = new Message();
		message.sender = new ObjectId();
		message.receiver = person._id;
		message.created = DateTimeUtils.getNow();
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(person._id);
		assertEquals(1, foundMessages.size());
		assertEquals("Title", foundMessages.get(0).title);
	}

	@Test
	public void findFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection messages = TestConnection.getCollection("messages");
		assertEquals(0, messages.count());
		Person person = new Person();
		person._id = new ObjectId();
		Message message = new Message();
		message.sender = person._id;
		message.receiver = new ObjectId();
		message.created = DateTimeUtils.getNow();
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(person._id);
		assertEquals(0, foundMessages.size());
	}
}
