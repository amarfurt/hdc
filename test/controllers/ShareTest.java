package controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.start;
import static play.test.Helpers.status;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.LoadData;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class ShareTest {

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
	public void shared() {
		String owner = "test1@example.com";
		ObjectId[] circleIds = getObjectIds("circles", owner, 2);
		ObjectId[] recordIds = getObjectIds("records", owner, 2);
		addRecordsToCircle(circleIds[0], recordIds);
		addRecordsToCircle(circleIds[1], new ObjectId[] { recordIds[0] });
		List<String> cIds = new ArrayList<String>(circleIds.length);
		for (ObjectId circleId : circleIds) {
			cIds.add(circleId.toString());
		}
		Result result = callAction(controllers.routes.ref.Share.sharedRecords(cIds), fakeRequest().withSession("email", owner));
		assertEquals(200, status(result));
		assertTrue(contentAsString(result).contains("name=\"" + recordIds[0].toString() + "\" value=\"record\" checked"));
		assertFalse(contentAsString(result).contains("name=\"" + recordIds[1].toString() + "\" value=\"record\" checked"));
	}

	@Test
	public void sharedForbidden() {
		String owner = "test1@example.com";
		ObjectId[] circleIds = getObjectIds("circles", owner, 2);
		ObjectId[] recordIds = getObjectIds("records", owner, 2);
		addRecordsToCircle(circleIds[0], recordIds);
		addRecordsToCircle(circleIds[1], new ObjectId[] { recordIds[0] });
		List<String> cIds = new ArrayList<String>(circleIds.length);
		for (ObjectId circleId : circleIds) {
			cIds.add(circleId.toString());
		}
		Result result = callAction(controllers.routes.ref.Share.sharedRecords(cIds),
				fakeRequest().withSession("email", "test2@example.com"));
		assertEquals(200, status(result));
		assertFalse(contentAsString(result).contains("checked>"));
	}

	@Test
	public void sharedNoIntersection() {
		String owner = "test1@example.com";
		ObjectId[] circleIds = getObjectIds("circles", owner, 2);
		ObjectId[] recordIds = getObjectIds("records", owner, 2);
		addRecordsToCircle(circleIds[0], new ObjectId[] { recordIds[1] });
		addRecordsToCircle(circleIds[1], new ObjectId[] { recordIds[0] });
		List<String> cIds = new ArrayList<String>(circleIds.length);
		for (ObjectId circleId : circleIds) {
			cIds.add(circleId.toString());
		}
		Result result = callAction(controllers.routes.ref.Share.sharedRecords(cIds), fakeRequest().withSession("email", owner));
		assertEquals(200, status(result));
		assertFalse(contentAsString(result).contains("checked>"));
	}

	/**
	 * Get the ids of 'num' objects in the given collection, owned by the given owner.
	 */
	private ObjectId[] getObjectIds(String collection, String owner, int num) {
		ObjectId[] objectIds = new ObjectId[num];
		DBCollection coll = TestConnection.getCollection(collection);
		DBObject query = new BasicDBObject("owner", owner);
		DBCursor cursor = coll.find(query);
		for (int i = 0; i < num; i++) {
			assertTrue(cursor.hasNext());
			DBObject cur = cursor.next();
			objectIds[i] = (ObjectId) cur.get("_id");
		}
		return objectIds;
	}

	private void addRecordsToCircle(ObjectId circleId, ObjectId[] recordIds) {
		DBCollection circles = TestConnection.getCollection("circles");
		DBObject query = new BasicDBObject("_id", circleId);
		for (ObjectId recordId : recordIds) {
			DBObject update = new BasicDBObject("$addToSet", new BasicDBObject("shared", recordId));
			WriteResult wr = circles.update(query, update);
			assertNull(wr.getLastError().getErrorMessage());
		}
	}

}
