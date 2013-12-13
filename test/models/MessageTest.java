package models;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.DateTimeUtils;
import utils.db.Database;

import com.mongodb.DBCollection;

public class MessageTest {

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
	public void add() throws ModelException {
		DBCollection messages = Database.getCollection("messages");
		assertEquals(0, messages.count());
		Message message = new Message();
		message.sender = new ObjectId();
		message.receiver = new ObjectId();
		message.created = DateTimeUtils.getNow();
		message.title = "Test title";
		message.content = "Test content.";
		Message.add(message);
		assertEquals(1, messages.count());
		assertEquals(message._id, messages.findOne().get("_id"));
		assertEquals(message.title, messages.findOne().get("title"));
	}

	@Test
	public void findSuccess() throws ModelException {
		DBCollection messages = Database.getCollection("messages");
		assertEquals(0, messages.count());
		Message message = new Message();
		message.sender = new ObjectId();
		message.receiver = new ObjectId();
		message.created = DateTimeUtils.getNow();
		message.title = "Test title";
		message.content = "Test content.";
		Message.add(message);
		assertEquals(1, messages.count());
		Set<Message> foundMessages = Message.findSentTo(message.receiver);
		assertEquals(1, foundMessages.size());
		assertEquals(message.title, foundMessages.iterator().next().title);
	}

	@Test
	public void findFailure() throws ModelException {
		DBCollection messages = Database.getCollection("messages");
		assertEquals(0, messages.count());
		Message message = new Message();
		message.sender = new ObjectId();
		message.receiver = new ObjectId();
		message.created = DateTimeUtils.getNow();
		message.title = "Title";
		message.content = "Content content.";
		Message.add(message);
		assertEquals(1, messages.count());
		Set<Message> foundMessages = Message.findSentTo(message.sender);
		assertEquals(0, foundMessages.size());
	}
}
