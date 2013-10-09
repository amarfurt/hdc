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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

public class SpaceTest {

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
	public void ownerSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		ObjectId spaceId = (ObjectId) spaceObject.get("_id");
		assertTrue(Space.isOwner(spaceId, space.owner));
	}

	@Test
	public void ownerFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		ObjectId spaceId = (ObjectId) spaceObject.get("_id");
		assertFalse(Space.isOwner(spaceId, new ObjectId()));
	}

	@Test
	public void addSpace() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		assertNull(Space.add(space));
		assertEquals(1, spaces.count());
		assertEquals("Test space", spaces.findOne().get("name"));
		assertNotNull(space._id);
	}

	@Test
	public void addSpaceWithExistingName() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		assertNull(Space.add(space));
		assertEquals(1, spaces.count());
		Space anotherSpace = new Space();
		anotherSpace.name = space.name;
		anotherSpace.owner = space.owner;
		anotherSpace.visualization = space.visualization;
		anotherSpace.records = space.records;
		assertEquals("A space with this name already exists.", Space.add(anotherSpace));
		assertEquals(1, spaces.count());
	}

	@Test
	public void renameSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		ObjectId spaceId = (ObjectId) spaceObject.get("_id");
		assertNull(Space.rename(spaceId, "New space"));
		assertEquals(1, spaces.count());
		assertEquals("New space", spaces.findOne().get("name"));
	}

	@Test
	public void renameWrongId() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		spaces.insert(new BasicDBObject(ModelConversion.modelToMap(Space.class, space)));
		assertEquals(1, spaces.count());
		ObjectId spaceId = ObjectId.get();
		assertEquals("This space doesn't exist.", Space.rename(spaceId, "New space"));
		assertEquals(1, spaces.count());
		assertEquals("Test space", spaces.findOne().get("name"));
	}

	@Test
	public void renameExistingName() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		ObjectId spaceId = (ObjectId) spaceObject.get("_id");
		assertEquals("A space with this name already exists.", Space.rename(spaceId, "Test space"));
		assertEquals(1, spaces.count());
		assertEquals("Test space", spaces.findOne().get("name"));
	}

	@Test
	public void deleteSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals("Test space", spaces.findOne().get("name"));
		assertNull(Space.delete((ObjectId) spaceObject.get("_id")));
		assertEquals(0, spaces.count());
	}

	@Test
	public void deleteFailure() throws IllegalArgumentException, IllegalAccessException {
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = new ObjectId();
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		ObjectId randomId = ObjectId.get();
		assertEquals("No space with this id exists.", Space.delete(randomId));
		assertEquals(1, spaces.count());
	}

	@Test
	public void addRecordSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
		assertNull(Space.addRecord((ObjectId) spaceObject.get("_id"), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void addRecordWrongId() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
		assertNull(Space.addRecord(ObjectId.get(), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void addRecordAlreadyInSpace() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		space.records.add(recordIds[1]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
		assertEquals("Record is already in this space.",
				Space.addRecord((ObjectId) spaceObject.get("_id"), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void removeRecordSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		space.records.add(recordIds[1]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
		assertNull(Space.removeRecord((ObjectId) spaceObject.get("_id"), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void removeRecordWrongId() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		space.records.add(recordIds[1]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
		assertEquals("Record is not in this space.", Space.removeRecord(ObjectId.get(), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(2, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void removeRecordNotInSpace() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		DBObject spaceObject = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(spaceObject);
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
		assertEquals("Record is not in this space.",
				Space.removeRecord((ObjectId) spaceObject.get("_id"), recordIds[1]));
		assertEquals(1, spaces.count());
		assertEquals(1, ((BasicDBList) spaces.findOne().get("records")).size());
	}

	@Test
	public void updateRecords() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space 1";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		DBObject space1 = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(space1);
		space.name = "Test space 2";
		space.records.clear();
		space.records.add(recordIds[1]);
		DBObject space2 = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(space2);
		assertEquals(2, spaces.count());
		DBObject query1 = new BasicDBObject("_id", space1.get("_id"));
		DBObject query2 = new BasicDBObject("_id", space2.get("_id"));
		assertEquals(1, ((BasicDBList) spaces.findOne(query1).get("records")).size());
		assertEquals(1, ((BasicDBList) spaces.findOne(query2).get("records")).size());
		List<ObjectId> spaceList = new ArrayList<ObjectId>();
		spaceList.add((ObjectId) space2.get("_id"));
		assertNull(Space.updateRecords(spaceList, recordIds[0], userIds[0]));
		assertEquals(0, ((BasicDBList) spaces.findOne(query1).get("records")).size());
		assertEquals(2, ((BasicDBList) spaces.findOne(query2).get("records")).size());
	}

	@Test
	public void findWithRecord() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection spaces = TestConnection.getCollection("spaces");
		assertEquals(0, spaces.count());
		Space space = new Space();
		space.name = "Test space 1";
		space.owner = userIds[0];
		space.visualization = "Simple List";
		space.records = new BasicDBList();
		space.records.add(recordIds[0]);
		DBObject space1 = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(space1);
		space.name = "Test space 2";
		space.records.clear();
		space.records.add(recordIds[1]);
		DBObject space2 = new BasicDBObject(ModelConversion.modelToMap(Space.class, space));
		spaces.insert(space2);
		assertEquals(2, spaces.count());
		Set<ObjectId> spaceList = Space.findWithRecord(recordIds[0], userIds[0]);
		assertEquals(1, spaceList.size());
		assertTrue(spaceList.contains(space1.get("_id")));
	}

}
