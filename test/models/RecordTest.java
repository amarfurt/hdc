package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBList;
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
		assertNull(Record.delete(ObjectId.get()));
		assertEquals(1, records.count());
	}

	@Test
	public void findOwned() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		String[] emails = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emails[1], emails[0], 2);
		List<Record> foundRecords = Record.findOwnedBy(User.find(emails[0]));
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[1], foundRecords));
	}

	@Test
	public void findNotInSpace() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		String[] emails = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emails[1], emails[0], 3);
		DBCollection spaces = TestConnection.getCollection("spaces");
		Space space = new Space();
		space.name = "Test space";
		space.owner = emails[0];
		space.visualization = "Simple List";
		space.order = 1;
		space.records = new BasicDBList();
		space.records.add(recordIds[1]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		List<Record> foundRecords = Record.findNotInSpace(User.find(emails[0]), (ObjectId) spaceObject.get("_id"));
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));

	}

	private boolean containsId(ObjectId id, List<Record> recordList) {
		boolean found = false;
		for (Record record : recordList) {
			if (id.equals(record._id)) {
				return true;
			}
		}
		return found;
	}

}
