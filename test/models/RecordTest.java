package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RecordTest {

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
	public void creatorSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertTrue(Record.isCreatorOrOwner(recordId, record.creator));
	}

	@Test
	public void ownerSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertTrue(Record.isCreatorOrOwner(recordId, record.owner));
	}

	@Test
	public void creatorOrOwnerFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertFalse(Record.isCreatorOrOwner(recordId, "wrong@example.com"));
	}

	@Test
	public void addRecord() throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		assertNull(Record.add(record));
		assertEquals(1, records.count());
		assertEquals("test2@example.com", records.findOne().get("creator"));
		assertEquals("test1@example.com", records.findOne().get("owner"));
		assertEquals("Test data.", records.findOne().get("data"));
		assertNotNull(record._id);
	}

	@Test
	public void deleteSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		assertEquals("test2@example.com", records.findOne().get("creator"));
		assertNull(Record.delete((ObjectId) recordObject.get("_id")));
		assertEquals(0, records.count());
	}

	@Test
	public void deleteFailure() throws IllegalArgumentException, IllegalAccessException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = "test2@example.com";
		record.owner = "test1@example.com";
		record.data = "Test data.";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(Record.class, record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId randomId = ObjectId.get();
		assertNull(Record.delete(randomId));
		assertEquals(1, records.count());
	}

}
