package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.db.Database;

import com.mongodb.BasicDBList;
import com.mongodb.DBCollection;

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
	public void exists() throws ModelException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertTrue(Circle.exists(circle.owner, circle._id));
	}

	@Test
	public void notExists() throws ModelException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertFalse(Circle.exists(new ObjectId(), circle._id));
	}

	// not testing order any further, has already been done in SpaceTest
	@Test
	public void add() throws ModelException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertNotNull(circle._id);
	}

	@Test
	public void rename() throws ModelException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		Circle.rename(circle.owner, circle._id, "New circle");
		assertEquals(1, circles.count());
		assertEquals("New circle", circles.findOne().get("name"));
	}

	@Test
	public void delete() throws ModelException {
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = new ObjectId();
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		Circle.delete(circle.owner, circle._id);
		assertEquals(0, circles.count());
	}

	@Test
	public void addMemberSuccess() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
		Circle.addMember(circle.owner, circle._id, userIds[1]);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void removeMember() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.members.add(userIds[1]);
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals(1, ((BasicDBList) circles.findOne().get("members")).size());
		Circle.removeMember(circle.owner, circle._id, userIds[1]);
		assertEquals(1, circles.count());
		assertEquals(0, ((BasicDBList) circles.findOne().get("members")).size());
	}

	@Test
	public void getShared() throws ModelException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		DBCollection circles = Database.getCollection("circles");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(2);
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = userIds[0];
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		circle.shared.add(recordIds[0]);
		circle.shared.add(recordIds[1]);
		Circle.add(circle);
		assertEquals(1, circles.count());
		assertEquals(2, ((BasicDBList) circles.findOne().get("shared")).size());
		Set<ObjectId> shared = Circle.getShared(circle._id);
		assertEquals(recordIds.length, shared.size());
		assertTrue(shared.contains(recordIds[0]));
		assertTrue(shared.contains(recordIds[1]));
	}

	@Test
	public void findMemberOf() throws ModelException {
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
		Circle.add(circle);
		circle = new Circle();
		circle.name = "Test circle 2";
		circle.owner = userIds[0];
		circle.order = 2;
		circle.members = new BasicDBList();
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(2, circles.count());
		Set<Circle> memberCircles = Circle.findMemberOf(userIds[1]);
		assertEquals(1, memberCircles.size());
		assertEquals("Test circle 1", memberCircles.iterator().next().name);
	}

	@Test
	public void findContacts() throws ModelException {
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
		Circle.add(circle);
		circle = new Circle();
		circle.name = "Test circle 2";
		circle.owner = userIds[1];
		circle.order = 1;
		circle.members = new BasicDBList();
		circle.members.add(userIds[0]);
		circle.members.add(userIds[2]);
		circle.shared = new BasicDBList();
		Circle.add(circle);
		assertEquals(2, circles.count());
		Set<User> contacts = Circle.findContacts(userIds[0]);
		assertEquals(1, contacts.size());
		assertTrue(contacts.iterator().next()._id.equals(userIds[1]));
	}

}
