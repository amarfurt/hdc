package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	public void ownerSuccess() throws IllegalArgumentException, IllegalAccessException, UnknownHostException, InstantiationException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertTrue(Circle.isOwner(circleId, circle.owner));
	}

	@Test
	public void ownerFailure() throws IllegalArgumentException, IllegalAccessException, UnknownHostException, InstantiationException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
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
		assertEquals(null, Circle.add(circle));
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
	}

	@Test
	public void renameSuccess() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId circleId = (ObjectId) circleObject.get("_id");
		assertEquals(1, Circle.rename(circleId, "New circle"));
		assertEquals(1, circles.count());
		assertEquals("New circle", circles.findOne().get("name"));
	}

	@Test
	public void renameFailure() throws IllegalArgumentException, IllegalAccessException {
		DBCollection circles = TestConnection.getCollection("circles");
		assertEquals(0, circles.count());
		Circle circle = new Circle();
		circle.name = "Test circle";
		circle.owner = "test1@example.com";
		circle.members = new BasicDBList();
		circles.insert(new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle)));
		assertEquals(1, circles.count());
		ObjectId circleId = ObjectId.get();
		assertEquals(0, Circle.rename(circleId, "New circle"));
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
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		assertEquals("Test circle", circles.findOne().get("name"));
		assertEquals(1, Circle.delete((ObjectId) circleObject.get("_id")));
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
		DBObject circleObject = new BasicDBObject(ModelConversion.modelToMap(Circle.class, circle));
		circles.insert(circleObject);
		assertEquals(1, circles.count());
		ObjectId randomId = ObjectId.get();
		assertEquals(0, Circle.delete(randomId));
		assertEquals(1, circles.count());
	}

}
