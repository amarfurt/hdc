package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.db.Database;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class SpaceTest {

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
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertTrue(Space.exists(space.owner, space._id));
	}

	@Test
	public void notExists() throws ModelException {
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertFalse(Space.exists(new ObjectId(), space._id));
	}

	@Test
	public void add() throws ModelException {
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		Space.add(space);
		assertEquals(1, spaces.count());
		assertEquals(space.name, spaces.findOne().get("name"));
		assertNotNull(space._id);
	}

	@Test
	public void rename() throws ModelException {
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		Space.add(space);
		assertEquals(1, spaces.count());
		Space.rename(space.owner, space._id, "New space");
		assertEquals(1, spaces.count());
		assertEquals("New space", spaces.findOne().get("name"));
	}

	@Test
	public void delete() throws ModelException {
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		Space.add(space);
		assertEquals(1, spaces.count());
		Space.delete(space.owner, space._id);
		assertEquals(0, spaces.count());
	}

	@Test
	public void addRecord() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0], userIds[1]);
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		Space.add(space);
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
		Space.addRecord(space._id, recordIds[1]);
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void removeRecord() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0], userIds[1]);
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = new ObjectId();
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		space.records.add(recordIds[1]);
		Space.add(space);
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
		Space.removeRecord(space._id, recordIds[1]);
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void updateRecords() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0], userIds[1]);
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space1 = new Space();
		space1.name = "Test space 1";
		space1.owner = userIds[0];
		space1.visualization = new ObjectId();
		space1.records = new BasicDBList();
		space1.records.add(recordIds[0]);
		Space.add(space1);
		Space space2 = new Space();
		space2.name = "Test space 2";
		space2.owner = userIds[0];
		space2.visualization = new ObjectId();
		space2.records = new BasicDBList();
		space2.records.add(recordIds[1]);
		Space.add(space2);
		assertEquals(2, spaces.count());
		DBObject query1 = new BasicDBObject("_id", space1._id);
		DBObject query2 = new BasicDBObject("_id", space2._id);
		assertEquals(1, ((BasicDBList) spaces.findOne(query1).get("records")).size());
		assertEquals(1, ((BasicDBList) spaces.findOne(query2).get("records")).size());
		Set<ObjectId> spaceList = new HashSet<ObjectId>();
		spaceList.add((ObjectId) space2._id);
		Space.updateRecords(spaceList, recordIds[0], userIds[0]);
		assertEquals(0, ((BasicDBList) spaces.findOne(query1).get("records")).size());
		assertEquals(2, ((BasicDBList) spaces.findOne(query2).get("records")).size());
	}

	@Test
	public void findWithRecord() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2, userIds[0], userIds[1]);
		DBCollection spaces = Database.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space1 = new Space();
		space1.name = "Test space 1";
		space1.owner = userIds[0];
		space1.visualization = new ObjectId();
		space1.records = new BasicDBList();
		space1.records.add(recordIds[0]);
		Space.add(space1);
		Space space2 = new Space();
		space2.name = "Test space 2";
		space2.owner = userIds[1];
		space2.visualization = new ObjectId();
		space2.records = new BasicDBList();
		space2.records.add(recordIds[1]);
		Space.add(space2);
		assertEquals(2, spaces.count());
		Set<ObjectId> spaceList = Space.findWithRecord(recordIds[0], userIds[0]);
		assertEquals(1, spaceList.size());
		assertTrue(spaceList.contains(space1._id));
	}

}
