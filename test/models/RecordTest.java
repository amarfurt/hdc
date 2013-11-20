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
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.DateTimeUtils;
import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.SearchException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RecordTest {

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
	public void creatorSuccess() throws ConversionException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = new ObjectId();
		record.owner = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertTrue(Record.isCreatorOrOwner(recordId, record.creator));
	}

	@Test
	public void ownerSuccess() throws ConversionException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = new ObjectId();
		record.owner = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertTrue(Record.isCreatorOrOwner(recordId, record.owner));
	}

	@Test
	public void creatorOrOwnerFailure() throws ConversionException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = new ObjectId();
		record.owner = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		ObjectId recordId = (ObjectId) recordObject.get("_id");
		assertFalse(Record.isCreatorOrOwner(recordId, new ObjectId()));
	}

	@Test
	public void addRecord() throws ConversionException, SearchException {
		DBCollection records = Database.getCollection("records");
		ObjectId creator = new ObjectId();
		ObjectId owner = new ObjectId();
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = creator;
		record.owner = owner;
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		assertNull(Record.add(record));
		assertEquals(1, records.count());
		assertEquals(creator, records.findOne().get("creator"));
		assertEquals(owner, records.findOne().get("owner"));
		assertEquals(record.data, records.findOne().get("data"));
		assertNotNull(record._id);
	}

	@Test
	public void deleteSuccess() throws ConversionException {
		DBCollection records = Database.getCollection("records");
		ObjectId creator = new ObjectId();
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = creator;
		record.owner = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		assertEquals(creator, records.findOne().get("creator"));
		assertNull(Record.delete((ObjectId) recordObject.get("_id")));
		assertEquals(0, records.count());
	}

	@Test
	public void deleteFailure() throws ConversionException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.creator = new ObjectId();
		record.owner = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"data\": \"Test data.\"}";
		DBObject recordObject = new BasicDBObject(ModelConversion.modelToMap(record));
		records.insert(recordObject);
		assertEquals(1, records.count());
		assertNull(Record.delete(ObjectId.get()));
		assertEquals(1, records.count());
	}

	@Test
	public void findOwned() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], 2);
		Set<Record> foundRecords = Record.findOwnedBy(userIds[0]);
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[1], foundRecords));
	}

	private boolean containsId(ObjectId id, Collection<Record> recordList) {
		boolean found = false;
		for (Record record : recordList) {
			if (id.equals(record._id)) {
				return true;
			}
		}
		return found;
	}

	@Test
	public void findNotInSpace() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], 3);
		DBCollection spaces = Database.getCollection("spaces");
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = new ObjectId();
		space.order = 1;
		space.records = new BasicDBList();
		space.records.add(recordIds[1]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(space));
		spaces.insert(spaceObject);
		Set<Record> foundRecords = Record.findVisible(userIds[0]);
		Set<ObjectId> recordsInSpace = Space.getRecords((ObjectId) spaceObject.get("_id"));
		Iterator<Record> iterator = foundRecords.iterator();
		while (iterator.hasNext()) {
			if (recordsInSpace.contains(iterator.next()._id)) {
				iterator.remove();
			}
		}
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}

	@Test
	public void findVisible() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordId = CreateDBObjects.insertRecords(userIds[1], userIds[1], 1);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], 3);
		DBCollection circles = Database.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[2]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		assertEquals(1, circles.count());
		Set<Record> foundRecords = Record.findVisible(userIds[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(recordId[0], foundRecords));
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}

	@Test
	public void findVisibleInOwnCircle() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordId = CreateDBObjects.insertRecords(userIds[1], userIds[1], 1);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], 3);
		DBCollection circles = Database.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[2]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		circle.owner = userIds[1];
		circle.members.clear();
		circle.members.add(userIds[0]);
		circle.shared.clear();
		circle.shared.add(recordId[0]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		assertEquals(2, circles.count());
		Set<Record> foundRecords = Record.findVisible(userIds[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(recordId[0], foundRecords));
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}

}
