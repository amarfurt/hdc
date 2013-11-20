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
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.SearchException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CircleTest {

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
	public void ownerSuccess() throws ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertTrue(Circle.isOwner(circleId, circle.owner));
	}

	@Test
	public void ownerFailure() throws ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertFalse(Circle.isOwner(circleId, new ObjectId()));
	}

	// not testing order any further, has already been done in SpaceTest
	@Test
	public void addCircle() throws SearchException, ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		assertNull(Circle.add(circle));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNotNull(circle._id);
	}

	@Test
	public void addCircleWithExistingName() throws SearchException, ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
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
	public void renameSuccess() throws SearchException, ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertNull(Circle.rename(circleId, "New circle"));
		assertEquals(1, circles.count());
		assertEquals("New circle", circles.findOne().get("name"));
	}

	@Test
	public void renameWrongId() throws SearchException, ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		assertEquals(1, circles.count());
		ObjectId circleId = ObjectId.get();
		assertEquals("No circle with this id exists.", Circle.rename(circleId, "New circle"));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
	}

	@Test
	public void renameExistingName() throws SearchException, ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertEquals("A circle with this name already exists.", Circle.rename(circleId, "Test circle"));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
	}

	@Test
	public void deleteSuccess() throws ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNull(Circle.delete((ObjectId) circleObject.get("_id")));
		assertEquals(0, circles.count());
	}

	@Test
	public void deleteFailure() throws ConversionException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId randomId = ObjectId.get();
		assertEquals("No circle with this id exists.", Circle.delete(randomId));
		assertEquals(1, circles.count());
	}

	@Test
	public void addMemberSuccess() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
		assertNull(Circle.addMember((ObjectId) circleObject.get("_id"), userIds[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberWrongId() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
		boolean exceptionCaught = false;
		try {
			Circle.addMember(ObjectId.get(), userIds[1]);
		} catch (NullPointerException e) {
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberOwner() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(1);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("Owner can't be added to own circle.",
				Circle.addMember((ObjectId) circleObject.get("_id"), circle.owner));
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void addMemberAlreadyInCircle() throws NoSuchAlgorithmException, InvalidKeySpecException,
			ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is already in this circle.",
				Circle.addMember((ObjectId) circleObject.get("_id"), userIds[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberSuccess() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertNull(Circle.removeMember((ObjectId) circleObject.get("_id"), userIds[1]));
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberWrongId() throws ConversionException, NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is not in this circle.", Circle.removeMember(ObjectId.get(), userIds[1]));
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMemberNotInCircle() throws NoSuchAlgorithmException, InvalidKeySpecException, ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
		assertEquals("User is not in this circle.", Circle.removeMember((ObjectId) circleObject.get("_id"), userIds[1]));
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void getShared() throws NoSuchAlgorithmException, InvalidKeySpecException, ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], 2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[1]);
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("shared")).size());
		Set<ObjectId> shared = Circle.getShared((ObjectId) circleObject.get("_id"));
		assertEquals(recordIds.length, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertTrue(shared.contains(recordIds[1]));
	}

	@Test
	public void findMemberOf() throws NoSuchAlgorithmException, InvalidKeySpecException, ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle 1";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		circle = new Circle();
		circle.name = "Test circle 2";
		circle.owner = userIds[0];
		circle.order = 2;
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		assertEquals(2, circles.count());
		Set<Circle> memberCircles = Circle.findMemberOf(userIds[1]);
		assertEquals(1, memberCircles.size());
		assertEquals("Test circle 1", memberCircles.iterator().next().name);
	}

	@Test
	public void findContacts() throws NoSuchAlgorithmException, InvalidKeySpecException, ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(3);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle 1";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		circle = new Circle();
		circle.name = "Test circle 2";
		circle.owner = userIds[1];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[0]);
		circle.members.add(userIds[2]);
		circle.shared = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(circle)));
		assertEquals(2, circles.count());
		Set<User> contacts = Circle.findContacts(userIds[0]);
		assertEquals(1, contacts.size());
		assertTrue(contacts.iterator().next()._id.equals(userIds[1]));
	}

	@Test
	public void updateShared() throws NoSuchAlgorithmException, InvalidKeySpecException, ConversionException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[0], userIds[1], 2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle 1";
		circle.owner = userIds[0];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		DBObject circle1 = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circle1);
		circle = new Circle();
		circle.name = "Test circle 2";
		circle.owner = userIds[0];
		circle.order = 2;
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[1]);
		DBObject circle2 = new BasicDBObject(ModelConversion.modelToMap(circle));
		circles.insert(circle2);
		assertEquals(2, circles.count());
		Set<ObjectId> circleIds = new HashSet<ObjectId>();
		circleIds.add((ObjectId) circle1.get("_id"));
		assertNull(Circle.updateShared(circleIds, recordIds[1], userIds[0]));
		BasicDBList shared1 = (BasicDBList) circles.findOne(new BasicDBObject("_id", circle1.get("_id"))).get("shared");
		assertEquals(2, shared1.size());
		assertTrue(shared1.contains(recordIds[0]));
		assertTrue(shared1.contains(recordIds[1]));
		BasicDBList shared2 = (BasicDBList) circles.findOne(new BasicDBObject("_id", circle2.get("_id"))).get("shared");
		assertEquals(0, shared2.size());
	}

}
