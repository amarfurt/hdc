package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;

import java.io.IOException;

import models.Space;
import models.User;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.Connection;
import utils.LoadData;
import utils.ModelConversion;
import utils.OrderOperations;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class SpacesTest {

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
	public void addSpace() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		ObjectId userId = User.getId("test1@example.com");
		ObjectId visualizationId = new ObjectId();
		Result result = callAction(
				controllers.routes.ref.Spaces.add(),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test space", "visualization", visualizationId.toString())));
		assertEquals(303, status(result));
		DBObject foundSpace = Connection.getCollection("spaces").findOne(new BasicDBObject("name", "Test space"));
		Space space = ModelConversion.mapToModel(Space.class, foundSpace.toMap());
		assertNotNull(space);
		assertEquals("Test space", space.name);
		assertEquals(userId, space.owner);
		assertEquals(visualizationId, space.visualization);
		assertEquals(OrderOperations.getMax("spaces", space.owner), space.order);
		assertEquals(0, space.records.size());
	}

	@Test
	public void renameSpaceSuccess() {
		DBCollection spaces = Connection.getCollection("spaces");
		DBObject query = new BasicDBObject("name", new BasicDBObject("$ne", "Renamed test space"));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Renamed test space")));
		assertEquals(200, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertEquals("Renamed test space", spaces.findOne(idQuery).get("name"));
		assertEquals(userId, spaces.findOne(idQuery).get("owner"));
	}

	@Test
	public void renameSpaceForbidden() {
		DBCollection spaces = Connection.getCollection("spaces");
		ObjectId userId = User.getId("test2@example.com");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", userId));
		query.put("name", new BasicDBObject("$ne", "Test space 2"));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		String originalName = (String) space.get("name");
		ObjectId originalOwner = (ObjectId) space.get("owner");
		Result result = callAction(
				controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test space 2")));
		assertEquals(403, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertEquals(originalName, spaces.findOne(idQuery).get("name"));
		assertEquals(originalOwner, spaces.findOne(idQuery).get("owner"));
	}

	@Test
	public void deleteSpaceSuccess() {
		DBCollection spaces = Connection.getCollection("spaces");
		long originalCount = spaces.count();
		DBObject space = spaces.findOne();
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.delete(spaceId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(200, status(result));
		assertNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount - 1, spaces.count());
	}

	@Test
	public void deleteSpaceForbidden() {
		DBCollection spaces = Connection.getCollection("spaces");
		long originalCount = spaces.count();
		ObjectId userId = User.getId("test2@example.com");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", userId));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(403, status(result));
		assertNotNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount, spaces.count());
	}

	@Test
	public void addRecordSuccess() {
		DBObject record = Connection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");
		DBCollection spaces = Connection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		int order = (Integer) space.get("order");
		BasicDBList records = (BasicDBList) space.get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.addRecords(spaceId),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of(recordId.toString(), "on")));
		assertEquals(303, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize + 1, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void addRecordAlreadyInSpace() {
		// get a record
		DBObject record = Connection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");

		// get a space without that record
		DBCollection spaces = Connection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		ObjectId userId = (ObjectId) space.get("owner");
		int order = (Integer) space.get("order");

		// insert that record into that space
		DBObject updateQuery = new BasicDBObject("_id", id);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
		WriteResult writeResult = Connection.getCollection("spaces").update(updateQuery, update);
		assertNull(writeResult.getLastError().getErrorMessage());

		// try to insert the same record again
		BasicDBList records = (BasicDBList) spaces.findOne(updateQuery).get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.addRecords(spaceId),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of(recordId.toString(), "on")));
		assertEquals(400, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void manuallyCreateRecord() throws ElasticSearchException, IOException {
		DBCollection records = Connection.getCollection("records");
		long oldSize = records.count();
		DBCollection users = Connection.getCollection("users");
		ObjectId userId = (ObjectId) users.findOne().get("_id");
		Result result = callAction(
				controllers.routes.ref.Spaces.manuallyCreateRecord(),
				fakeRequest().withSession("id", userId.toString()).withFormUrlEncodedBody(
						ImmutableMap.of("data", "Test data", "keywords", "test")));
		assertEquals(303, status(result));
		assertEquals(oldSize + 1, records.count());
	}

}
