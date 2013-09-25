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
import utils.DateTimeUtils;
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
		record.created = DateTimeUtils.getNow();
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
		record.created = DateTimeUtils.getNow();
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
		record.created = DateTimeUtils.getNow();
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
		record.created = DateTimeUtils.getNow();
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
		record.created = DateTimeUtils.getNow();
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
		record.created = DateTimeUtils.getNow();
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
		List<Record> foundRecords = Record.findOwnedBy(emails[0]);
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[1], foundRecords));
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
		List<Record> foundRecords = Record.findSharedWith(emails[0]);
		foundRecords = Space.makeDisjoint((ObjectId) spaceObject.get("_id"), foundRecords);
		assertEquals(2, foundRecords.size());
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}

	@Test
	public void findSharedWith() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		String[] emails = CreateDBObjects.insertUsers(2);
		ObjectId[] recordId = CreateDBObjects.insertRecords(emails[1], emails[1], 1);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emails[1], emails[0], 3);
		DBCollection circles = TestConnection.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emails[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(emails[1]);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[2]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		assertEquals(1, circles.count());
		List<Record> foundRecords = Record.findSharedWith(emails[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(recordId[0], foundRecords));
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}

	@Test
	public void findSharedWithOwnCircle() throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException, InstantiationException {
		DBCollection records = TestConnection.getCollection("records");
		assertEquals(0, records.count());
		String[] emails = CreateDBObjects.insertUsers(2);
		ObjectId[] recordId = CreateDBObjects.insertRecords(emails[1], emails[1], 1);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emails[1], emails[0], 3);
		DBCollection circles = TestConnection.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emails[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(emails[1]);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[2]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		circle.owner = emails[1];
		circle.members.clear();
		circle.members.add(emails[0]);
		circle.shared.clear();
		circle.shared.add(recordId[0]);
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		assertEquals(2, circles.count());
		List<Record> foundRecords = Record.findSharedWith(emails[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(recordId[0], foundRecords));
		assertTrue(containsId(recordIds[0], foundRecords));
		assertTrue(containsId(recordIds[2], foundRecords));
	}
	
	@Test
	public void dataToString() {
		String shortString = "Some medical data.";
		String longString = "Doctor Frankenstein detected a fracture of the bone in the patient's lower left leg.";
		String noSpacesString = "DoctorFrankensteindetectedafractureoftheboneinthepatient'slowerleftleg."; 
		String splitAt39 = "10letters 10letters 10letters 10letters no longer shown";
		String splitAt40 = "10letters 10letters 10letters 10letters1 no longer shown";
		String splitAt41 = "10letters 10letters 10letters 10letters11 no longer shown";
		assertEquals(shortString, Record.dataToString(shortString));
		assertEquals("Doctor Frankenstein detected a fracture ...", Record.dataToString(longString));
		assertEquals("...", Record.dataToString(noSpacesString));
		assertEquals("10letters 10letters 10letters 10letters ...", Record.dataToString(splitAt39));
		assertEquals("10letters 10letters 10letters 10letters1 ...", Record.dataToString(splitAt40));
		assertEquals("10letters 10letters 10letters ...", Record.dataToString(splitAt41));
	}

}
