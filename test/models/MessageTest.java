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
import utils.db.Database;

import com.mongodb.BasicDBObject;
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
	public void findSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection messages = Database.getCollection("messages");
		assertEquals(0, messages.count());
		User user = new User();
		user._id = new ObjectId();
		Message message = new Message();
		message.sender = new ObjectId();
		message.receiver = user._id;
		message.created = DateTimeUtils.getNow();
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(user._id);
		assertEquals(1, foundMessages.size());
		assertEquals("Title", foundMessages.get(0).title);
	}

	@Test
	public void findFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection messages = Database.getCollection("messages");
		assertEquals(0, messages.count());
		User user = new User();
		user._id = new ObjectId();
		Message message = new Message();
		message.sender = user._id;
		message.receiver = new ObjectId();
		message.created = DateTimeUtils.getNow();
		message.title = "Title";
		message.content = "Content content.";
		messages.insert(new BasicDBObject(ModelConversion.modelToMap(message)));
		assertEquals(1, messages.count());
		List<Message> foundMessages = Message.findSentTo(user._id);
		assertEquals(0, foundMessages.size());
	}
}
