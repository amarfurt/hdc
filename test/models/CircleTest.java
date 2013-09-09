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
import java.util.Arrays;
import java.util.HashSet;
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

public class CircleTest {

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
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertTrue(Circle.isOwner(circleId, circle.owner));
	}

	@Test
	public void ownerFailure() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertFalse(Circle.isOwner(circleId, "wrong@example.com"));
	}

	@Test
	public void addCircle() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		assertNull(Circle.add(circle));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNotNull(circle._id);
	}

	@Test
	public void addCircleWithExistingName() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		assertNull(Circle.add(circle));
		assertEquals(1, circles.count());
		Circle anotherCircle = new Circle();
		anotherCircle.name = circle.name;
		anotherCircle.owner = circle.owner;
		anotherCircle.members = circle.members;
		anotherCircle.shared = circle.shared;
		assertEquals("A circle with this name already exists.", Circle.add(anotherCircle));
		assertEquals(1, circles.count());
	}

	@Test
	public void renameSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertNull(Circle.rename(circleId, "New circle"));
		assertEquals(1, circles.count());
		assertEquals("New circle", circles.findOne().get("name"));
	}

	@Test
	public void renameWrongId() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		assertEquals(1, circles.count());
		ObjectId circleId = ObjectId.get();
		assertEquals("No circle with this id exists.", Circle.rename(circleId, "New circle"));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
	}

	@Test
	public void renameExistingName() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertEquals("A circle with this name already exists.", Circle.rename(circleId, "Test circle"));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
	}

	@Test
	public void deleteSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNull(Circle.delete((ObjectId) circleObject.get("_id")));
		assertEquals(0, circles.count());
	}

	@Test
	public void deleteFailure() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId randomId = ObjectId.get();
		assertNull(Circle.delete(randomId));
		assertEquals(1, circles.count());
	}

	@Test
	public void addMemberSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertNull(Circle.addMember((ObjectId) circleObject.get("_id"), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberWrongId() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertNull(Circle.addMember(ObjectId.get(), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberOwner() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(1);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("Owner can't be added to own circle.",
				Circle.addMember((ObjectId) circleObject.get("_id"), circle.owner));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberAlreadyInCircle() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.members.add(emailAddresses[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is already in this circle.",
				Circle.addMember((ObjectId) circleObject.get("_id"), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberSuccess() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.members.add(emailAddresses[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
		assertNull(Circle.removeMember((ObjectId) circleObject.get("_id"), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberWrongId() throws IllegalArgumentException, IllegalAccessException, InstantiationException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.members.add(emailAddresses[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is not in this circle.", Circle.removeMember(ObjectId.get(), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberNotInCircle() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is not in this circle.",
				Circle.removeMember((ObjectId) circleObject.get("_id"), emailAddresses[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void getShared() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[1]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("shared")).size());
		Set<ObjectId> shared = Circle.getShared((ObjectId) circleObject.get("_id"), emailAddresses[0]);
		assertEquals(recordIds.length, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertTrue(shared.contains(recordIds[1]));
	}

	@Test
	public void shareRecord() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("shared")).size());
		Circle.shareRecords((ObjectId) circleObject.get("_id"), new HashSet<ObjectId>(Arrays.asList(recordIds[1])));
		assertEquals(1, circles.count());
		BasicDBList shared = (BasicDBList) circles.findOne().get("shared");
		assertEquals(2, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertTrue(shared.contains(recordIds[1]));
	}

	@Test
	public void shareRecords() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 4);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("shared")).size());
		ObjectId[] recordsToShare = new ObjectId[3];
		System.arraycopy(recordIds, 1, recordsToShare, 0, recordsToShare.length);
		Circle.shareRecords((ObjectId) circleObject.get("_id"), new HashSet<ObjectId>(Arrays.asList(recordsToShare)));
		assertEquals(1, circles.count());
		BasicDBList shared = (BasicDBList) circles.findOne().get("shared");
		assertEquals(4, shared.size());
		assertTrue(shared.containsAll(Arrays.asList(recordIds)));
	}

	@Test
	public void pullRecordSuccess() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[1]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("shared")).size());
		Circle.pullRecords((ObjectId) circleObject.get("_id"), new HashSet<ObjectId>(Arrays.asList(recordIds[1])));
		assertEquals(1, circles.count());
		BasicDBList shared = (BasicDBList) circles.findOne().get("shared");
		assertEquals(1, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertFalse(shared.contains(recordIds[1]));
	}

	@Test
	public void pullRecordsSuccess() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 4);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.addAll(Arrays.asList(recordIds));
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(4, ((BasicDBList) circles.findOne().get("shared")).size());
		ObjectId[] recordsToPull = new ObjectId[2];
		System.arraycopy(recordIds, 2, recordsToPull, 0, recordsToPull.length);
		Circle.pullRecords((ObjectId) circleObject.get("_id"), new HashSet<ObjectId>(Arrays.asList(recordsToPull)));
		assertEquals(1, circles.count());
		BasicDBList shared = (BasicDBList) circles.findOne().get("shared");
		assertEquals(2, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertTrue(shared.contains(recordIds[1]));
		assertFalse(shared.contains(recordIds[2]));
		assertFalse(shared.contains(recordIds[3]));
	}

	@Test
	public void pullRecordNotShared() throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		String[] emailAddresses = CreateDBObjects.insertUsers(2);
		DBCollection circles = TestConnection.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(emailAddresses[1], emailAddresses[0], 2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = emailAddresses[0];
		circle.members = new BasicDBList();
		circle.members.add(circle.owner);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("shared")).size());
		Circle.pullRecords((ObjectId) circleObject.get("_id"), new HashSet<ObjectId>(Arrays.asList(recordIds[1])));
		assertEquals(1, circles.count());
		BasicDBList shared = (BasicDBList) circles.findOne().get("shared");
		assertEquals(1, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertFalse(shared.contains(recordIds[1]));
	}

}
