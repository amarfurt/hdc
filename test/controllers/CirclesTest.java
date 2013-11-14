package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.Circle;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.Connection;
import utils.LoadData;
import utils.ModelConversion;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class CirclesTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Connection.connectToTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		Connection.close();
	}

	@Test
	public void addCircle() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		ObjectId userId = User.getId("test1@example.com");
		Result result = callAction(
				controllers.routes.ref.Circles.add(),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test circle")));
		assertEquals(303, status(result));
		DBObject foundCircle = Connection.getCollection("circles").findOne(new BasicDBObject("name", "Test circle"));
		Circle circle = ModelConversion.mapToModel(Circle.class, foundCircle.toMap());
		assertNotNull(circle);
		assertEquals("Test circle", circle.name);
		assertEquals(userId, circle.owner);
		assertEquals(0, circle.members.size());
	}

	@Test
	public void renameCircleSuccess() {
		DBCollection circles = Connection.getCollection("circles");
		DBObject query = new BasicDBObject("name", new BasicDBObject("$ne", "Renamed circle"));
		DBObject circle = circles.findOne(query);
		ObjectId circleId = (ObjectId) circle.get("_id");
		ObjectId userId = (ObjectId) circle.get("owner");
		Result result = callAction(controllers.routes.ref.Circles.rename(circleId.toString()), fakeRequest()
				.withSession("id", userId.toString()).withFormUrlEncodedBody(ImmutableMap.of("name", "Renamed circle")));
		assertEquals(200, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", circleId);
		assertEquals("Renamed circle", circles.findOne(idQuery).get("name"));
		assertEquals(userId, circles.findOne(idQuery).get("owner"));
	}

	@Test
	public void renameCircleForbidden() {
		DBCollection circles = Connection.getCollection("circles");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", "test2@example.com"));
		query.put("name", new BasicDBObject("$ne", "Test circle 2"));
		DBObject circle = circles.findOne(query);
		ObjectId circleId = (ObjectId) circle.get("_id");
		String originalName = (String) circle.get("name");
		ObjectId originalOwner = (ObjectId) circle.get("owner");
		Result result = callAction(
				controllers.routes.ref.Circles.rename(circleId.toString()),
				fakeRequest().withSession("id", User.getId("test2@example.com").toString()).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test circle 2")));
		assertEquals(403, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", circleId);
		assertEquals(originalName, circles.findOne(idQuery).get("name"));
		assertEquals(originalOwner, circles.findOne(idQuery).get("owner"));
	}

	@Test
	public void deleteCircleSuccess() {
		DBCollection circles = Connection.getCollection("circles");
		DBObject circle = circles.findOne();
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId userId = (ObjectId) circle.get("owner");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.delete(circleId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(200, status(result));
		assertNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	@Test
	public void deleteCircleForbidden() {
		DBCollection circles = Connection.getCollection("circles");
		ObjectId userId = User.getId("test2@example.com");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", userId));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.rename(circleId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(403, status(result));
		assertNotNull(circles.findOne(new BasicDBObject("_id", id)));
	}

	// forbidden requests are blocked by this controller, no longer testing this
	@Test
	public void addUserSuccess() {
		DBCollection circles = Connection.getCollection("circles");
		ObjectId userId = User.getId("test3@example.com");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$nin", new ObjectId[] { userId }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId ownerId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Circles.addUsers(circleId),
				fakeRequest().withSession("id", ownerId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of(userId.toString(), "on")));
		assertEquals(303, status(result));
		assertEquals(oldSize + 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void addUserInvalidUser() {
		DBCollection circles = Connection.getCollection("circles");
		DBObject circle = circles.findOne();
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId userId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		boolean exceptionCaught = false;
		try {
			callAction(
					controllers.routes.ref.Circles.addUsers(circleId),
					fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
							ImmutableMap.of(new ObjectId().toString(), "on")));
		} catch (NullPointerException e) {
			exceptionCaught = true;
		}
		assertTrue(exceptionCaught);
		assertEquals(oldSize, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void removeMemberSuccess() {
		DBCollection circles = Connection.getCollection("circles");
		ObjectId userId = User.getId("test2@example.com");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$in", new ObjectId[] { userId }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId ownerId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.removeMember(circleId, userId.toString()),
				fakeRequest().withSession("id", ownerId.toString()));
		assertEquals(200, status(result));
		assertEquals(oldSize - 1, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}

	@Test
	public void removeMemberNotInCircle() {
		DBCollection circles = Connection.getCollection("circles");
		ObjectId userId = User.getId("test3@example.com");
		DBObject query = new BasicDBObject();
		query.put("members", new BasicDBObject("$nin", new ObjectId[] { userId }));
		DBObject circle = circles.findOne(query);
		ObjectId id = (ObjectId) circle.get("_id");
		ObjectId ownerId = (ObjectId) circle.get("owner");
		BasicDBList members = (BasicDBList) circle.get("members");
		int oldSize = members.size();
		String circleId = id.toString();
		Result result = callAction(controllers.routes.ref.Circles.removeMember(circleId, userId.toString()),
				fakeRequest().withSession("id", ownerId.toString()));
		assertEquals(400, status(result));
		assertEquals(oldSize, ((BasicDBList) circles.findOne(new BasicDBObject("_id", id)).get("members")).size());
	}
}
