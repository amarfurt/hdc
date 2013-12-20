package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.DateTimeUtils;
import utils.db.Database;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;

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
	public void exists() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertTrue(Record.exists(record.owner, record._id));
	}

	@Test
	public void notExists() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertFalse(Record.exists(new ObjectId(), record._id));
	}

	@Test
	public void add() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertEquals(record.creator, records.findOne().get("creator"));
		assertEquals(record.owner, records.findOne().get("owner"));
		assertEquals(record.data, records.findOne().get("data"));
		assertNotNull(record._id);
	}

	@Test
	public void delete() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		Record record = new Record();
		record.app = new ObjectId();
		record.owner = new ObjectId();
		record.creator = new ObjectId();
		record.created = DateTimeUtils.getNow();
		record.data = "{\"title\":\"Test record\",\"data\":\"Test data.\"}";
		record.name = "Test record";
		record.description = "Test data.";
		Record.add(record);
		assertEquals(1, records.count());
		assertEquals(record.creator, records.findOne().get("creator"));
		Record.delete(record._id);
		assertEquals(0, records.count());
	}

	@Test
	public void findOwned() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0]);
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
	public void findVisibleAddMember() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] user0RecordIds = CreateDBObjects.insertRecords(3, userIds[0]);
		ObjectId[] user1RecordIds = CreateDBObjects.insertRecords(1, userIds[1]);
		DBCollection circles = Database.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circle.shared.add(user0RecordIds[0]);
		circle.shared.add(user0RecordIds[2]);
		Circle.add(circle);
		Circle.addMember(circle.owner, circle._id, userIds[1]);
		assertEquals(1, circles.count());
		Set<Record> foundRecords = Record.findVisible(userIds[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(user1RecordIds[0], foundRecords));
		assertTrue(containsId(user0RecordIds[0], foundRecords));
		assertTrue(containsId(user0RecordIds[2], foundRecords));
	}

	@Test
	public void findVisibleAddRecordToCircle() throws ModelException {
		DBCollection records = Database.getCollection("records");
		assertEquals(0, records.count());
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] user0RecordIds = CreateDBObjects.insertRecords(3, userIds[0]);
		ObjectId[] user1RecordIds = CreateDBObjects.insertRecords(1, userIds[1]);
		DBCollection circles = Database.getCollection("circles");
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		Circle.add(circle);
		Set<ObjectId> circleIds = new HashSet<ObjectId>();
		circleIds.add(circle._id);
		Circle.startSharingWith(circle.owner, user0RecordIds[0], circleIds);
		Circle.startSharingWith(circle.owner, user0RecordIds[2], circleIds);
		assertEquals(1, circles.count());
		Set<Record> foundRecords = Record.findVisible(userIds[1]);
		assertEquals(3, foundRecords.size());
		assertTrue(containsId(user1RecordIds[0], foundRecords));
		assertTrue(containsId(user0RecordIds[0], foundRecords));
		assertTrue(containsId(user0RecordIds[2], foundRecords));
	}

}
