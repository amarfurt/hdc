package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.Circle;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.LoadData;
import utils.ModelConversion;
import utils.TestConnection;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CirclesTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		TestConnection.connectToTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		TestConnection.close();
	}

	@Test
	public void addCircle() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		Result result = callAction(controllers.routes.ref.Circles.add(), fakeRequest().withSession("email", "test1@example.com")
				.withFormUrlEncodedBody(ImmutableMap.of("name", "Test circle")));
		assertEquals(200, status(result));
		DBObject foundCircle = TestConnection.getCollection("circles").findOne(new BasicDBObject("name", "Test circle"));
		Circle circle = ModelConversion.mapToModel(Circle.class, foundCircle.toMap());
		assertNotNull(circle);
		assertEquals("Test circle", circle.name);
		assertEquals("test1@example.com", circle.owner);
		assertEquals(1, circle.members.size());
		assertEquals("test1@example.com", circle.members.get(0));
	}

	@Test
	public void renameCircleSuccess() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject("name", new BasicDBObject("$ne", "Test circle 2"));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.rename(circleId), fakeRequest().withSession("email", owner)
				.withFormUrlEncodedBody(ImmutableMap.of("name", "Test circle 2")));
		assertEquals(200, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertEquals("Test circle 2", circles.findOne(idQuery).get("name"));
		assertEquals(owner, circles.findOne(idQuery).get("owner"));
	}

	@Test
	public void renameCircleForbidden() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", "test2@example.com"));
		query.put("name", new BasicDBObject("$ne", "Test circle 2"));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.rename(circleId), fakeRequest().withSession("email", "test2@example.com")
				.withFormUrlEncodedBody(ImmutableMap.of("name", "Test circle 2")));
		assertEquals(403, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertNotEquals("Test circle 2", circles.findOne(idQuery).get("owner"));
		assertNotEquals("Test circle 2", circles.findOne(idQuery).get("name"));
	}

	@Test
	public void deleteCircleSuccess() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject circle = circles.findOne();
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.delete(circleId), fakeRequest().withSession("email", owner));
		assertEquals(200, status(result));
		assertNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	@Test
	public void deleteCircleForbidden() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", "test2@example.com"));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.rename(circleId), fakeRequest().withSession("email", "test2@example.com"));
		assertEquals(403, status(result));
		assertNotNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	// forbidden requests are blocked by this controller, no longer testing this
	@Test
	public void addMemberSuccess() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$nin", new String[] { "test3@example.com" }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.addMember(circleId), fakeRequest().withSession("email", owner)
				.withFormUrlEncodedBody(ImmutableMap.of("name", "test3@example.com")));
		assertEquals(200, status(result));
		assertEquals(oldSize + 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void addMemberInvalidUser() {
		DBCollection users = TestConnection.getCollection("users");
		assertNull(users.findOne(new BasicDBObject("members", "test5@example.com")));
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject circle = circles.findOne();
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.addMember(circleId), fakeRequest().withSession("email", owner)
				.withFormUrlEncodedBody(ImmutableMap.of("name", "test5@example.com")));
		assertEquals(400, status(result));
		assertEquals(oldSize, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void removeMemberSuccess() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$in", new String[] { "test2@example.com" }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.removeMember(circleId), fakeRequest().withSession("email", owner)
				.withFormUrlEncodedBody(ImmutableMap.of("name", "test2@example.com")));
		assertEquals(200, status(result));
		assertEquals(oldSize - 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void removeMemberNotInCircle() {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$nin", new String[] { "test3@example.com" }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String owner = (String) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.removeMember(circleId), fakeRequest().withSession("email", owner)
				.withFormUrlEncodedBody(ImmutableMap.of("name", "test3@example.com")));
		assertEquals(400, status(result));
		assertEquals(oldSize, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}
}
