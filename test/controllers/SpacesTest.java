package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.Space;
import models.User;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;
import utils.LoadData;
import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.OrderOperations;
import utils.db.Database;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class SpacesTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Database.connectToTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		Database.close();
	}

	@Test
	public void addSpace() throws ConversionException {
		ObjectId userId = User.getId("test1@example.com");
		ObjectId visualizationId = new ObjectId();
		Result result = callAction(
				controllers.routes.ref.Spaces.add(),
				fakeRequest().withSession("id", userId.toString()).withJsonBody(
						Json.parse("{\"name\": \"Test space\", \"visualization\": \"" + visualizationId.toString()
								+ "\"}")));
		assertEquals(200, status(result));
		DBObject foundSpace = Database.getCollection("spaces").findOne(new BasicDBObject("name", "Test space"));
		Space space = ModelConversion.mapToModel(Space.class, foundSpace.toMap());
		assertNotNull(space);
		assertEquals("Test space", space.name);
		assertEquals(userId, space.owner);
		assertEquals(visualizationId, space.visualization);
		assertEquals(OrderOperations.getMax("spaces", space.owner), space.order);
		assertEquals(0, space.records.size());
	}

	@Test
	public void deleteSpaceSuccess() {
		DBCollection spaces = Database.getCollection("spaces");
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
		DBCollection spaces = Database.getCollection("spaces");
		long originalCount = spaces.count();
		ObjectId userId = User.getId("test2@example.com");
		DBObject query = new BasicDBObject();
		query.put("owner", new BasicDBObject("$ne", userId));
		DBObject space = spaces.findOne(query);
		ObjectId id = (ObjectId) space.get("_id");
		String spaceId = id.toString();
		Result result = callAction(controllers.routes.ref.Spaces.delete(spaceId),
				fakeRequest().withSession("id", userId.toString()));
		assertEquals(400, status(result));
		assertEquals("No space with this id exists.", contentAsString(result));
		assertNotNull(spaces.findOne(new BasicDBObject("_id", id)));
		assertEquals(originalCount, spaces.count());
	}

	@Test
	public void addRecordSuccess() {
		DBObject record = Database.getCollection("records").findOne();
		ObjectId recordId = (ObjectId) record.get("_id");
		DBCollection spaces = Database.getCollection("spaces");
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
				fakeRequest().withSession("id", userId.toString()).withJsonBody(
						Json.parse("{\"records\": [\"" + recordId.toString() + "\"]}")));
		assertEquals(200, status(result));
		DBObject foundSpace = spaces.findOne(new BasicDBObject("_id", id));
		assertEquals(order, foundSpace.get("order"));
		assertEquals(oldSize + 1, ((BasicDBList) foundSpace.get("records")).size());
	}

}
