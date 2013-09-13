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
import models.Space;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.LoadData;
import utils.ModelConversion;
import utils.OrderOperations;
import utils.TestConnection;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class SpacesTest {

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
	public void addSpace() throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		Result result = callAction(
				controllers.routes.ref.Spaces.add(),
				fakeRequest().withSession("email", "test1@example.com").withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test space", "visualization", "Test visualization")));
		assertEquals(303, status(result));
		DBObject foundSpace = TestConnection.getCollection("spaces").findOne(new BasicDBObject("name", "Test space"));
		Space space = ModelConversion.mapToModel(Space.class, foundSpace.toMap());
		assertNotNull(space);
		assertEquals("Test space", space.name);
		assertEquals("test1@example.com", space.owner);
		assertEquals("Test visualization", space.visualization);
		assertEquals(OrderOperations.getMax("spaces", space.owner), space.order);
		assertEquals(0, space.records.size());
	}

	@Test
	public void renameSpaceSuccess() {
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject("name", new BasicDBObject("$ne", "Renamed test space"));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("email", owner).withFormUrlEncodedBody(
						ImmutableMap.of("name", "Renamed test space")));
		assertEquals(200, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertEquals("Renamed test space", spaces.findOne(idQuery).get("name"));
		assertEquals(owner, spaces.findOne(idQuery).get("owner"));
	}

	@Test
	public void renameSpaceForbidden() {
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", "test2@example.com"));
		query.put("name", new BasicDBObject("$ne", "Test space 2"));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		String originalName = (String) space.get("name");
		String originalOwner = (String) space.get("owner");
		Result result = callAction(
				controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("email", "test2@example.com").withFormUrlEncodedBody(
						ImmutableMap.of("name", "Test space 2")));
		assertEquals(403, status(result));
		BasicDBObject idQuery = new BasicDBObject("_id", id);
		assertEquals(originalName, spaces.findOne(idQuery).get("name"));
		assertEquals(originalOwner, spaces.findOne(idQuery).get("owner"));
	}

	@Test
	public void deleteSpaceSuccess() {
		DBCollection spaces = TestConnection.getCollection("spaces");
		long originalCount = spaces.count();
		DBObject space = spaces.findOne();
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.delete(spaceId),
				fakeRequest().withSession("email", owner));
		assertEquals(200, status(result));
		assertNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount - 1, spaces.count());
	}

	@Test
	public void deleteSpaceForbidden() {
		DBCollection spaces = TestConnection.getCollection("spaces");
		long originalCount = spaces.count();
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", "test2@example.com"));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.rename(spaceId),
				fakeRequest().withSession("email", "test2@example.com"));
		assertEquals(403, status(result));
		assertNotNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount, spaces.count());
	}

	@Test
	public void addRecordSuccess() {
		DBObject record = TestConnection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		int order = (int) space.get("order");
		BasicDBList records = (BasicDBList) space.get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.addRecords(spaceId),
				fakeRequest().withSession("email", owner).withFormUrlEncodedBody(
						ImmutableMap.of(recordId.toString(), "on")));
		assertEquals(303, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize + 1, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void addRecordAlreadyInSpace() {
		// get a record
		DBObject record = TestConnection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");

		// get a space without that record
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		int order = (int) space.get("order");

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
				fakeRequest().withSession("email", owner).withFormUrlEncodedBody(
						ImmutableMap.of(recordId.toString(), "on")));
		assertEquals(400, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void removeRecordSuccess() {
		// get a record
		DBObject record = TestConnection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");

		// get a space without that record
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		int order = (int) space.get("order");

		// insert that record into that space
		DBObject updateQuery = new BasicDBObject("_id", id);
		DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("records", recordId));
		WriteResult writeResult = Connection.getCollection("spaces").update(updateQuery, update);
		assertNull(writeResult.getLastError().getErrorMessage());

		// remove the record from the space again
		BasicDBList records = (BasicDBList) spaces.findOne(updateQuery).get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.removeRecord(spaceId),
				fakeRequest().withSession("email", owner).withFormUrlEncodedBody(
						ImmutableMap.of("id", recordId.toString())));
		assertEquals(200, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize - 1, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void removeRecordNotInSpace() {
		// get a record
		DBObject record = TestConnection.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");

		// get a space without that record
		DBCollection spaces = TestConnection.getCollection("spaces");
		DBObject query = new BasicDBObject();
		query.put("records", new BasicDBObject("$nin", new ObjectId[] { recordId }));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String owner = (String) space.get("owner");
		int order = (int) space.get("order");

		// try to remove that record from that space
		BasicDBList records = (BasicDBList) space.get("records");
		int oldSize = records.size();
		String spaceId = id.toString();
		Result result = callAction(
				controllers.routes.ref.Spaces.removeRecord(spaceId),
				fakeRequest().withSession("email", owner).withFormUrlEncodedBody(
						ImmutableMap.of("id", recordId.toString())));
		assertEquals(400, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize, ((BasicDBList) foundSpace.get("records")).size());
	}

	@Test
	public void manuallyAddRecord() {
		DBCollection records = TestConnection.getCollection("records");
		long oldSize = records.count();
		DBCollection users = TestConnection.getCollection("users");
		String username = (String) users.findOne().get("email");
		Result result = callAction(
				controllers.routes.ref.Spaces.manuallyAddRecord(),
				fakeRequest().withSession("email", username).withFormUrlEncodedBody(
						ImmutableMap.of("data", "Test data", "tags", "test")));
		assertEquals(303, status(result));
		assertEquals(oldSize + 1, records.count());
	}

}
