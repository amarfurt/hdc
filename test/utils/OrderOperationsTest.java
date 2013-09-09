package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class OrderOperationsTest {

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
	public void maxOfUser() {
		DBCollection collection = TestConnection.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("owner", "test1@example.com");
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("order", 2);
		collection.insert(new BasicDBObject(values));
		values.put("order", 5);
		collection.insert(new BasicDBObject(values));
		values.put("order", 4);
		collection.insert(new BasicDBObject(values));
		values.put("owner", "test2@example.com");
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		assertEquals(5, OrderOperations.getMax("order", "test1@example.com"));
	}

	@Test
	public void maxNoEntries() {
		assertEquals(0, OrderOperations.getMax("order", "test1@example.com"));
	}

	@Test
	public void maxNoEntriesForUser() {
		DBCollection collection = TestConnection.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("owner", "test1@example.com");
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("owner", "test2@example.com");
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		assertEquals(0, OrderOperations.getMax("order", "test3@example.com"));
	}

	@Test
	public void incrementGreaterThan() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.increment("order", "test1@example.com", 4, 0));
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(6, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void incrementBetween() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.increment("order", "test1@example.com", 3, 4));
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void incrementFromGreaterTo() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.increment("order", "test1@example.com", 4, 1));
		assertEquals(2, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(5, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementGreaterThan() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.decrement("order", "test1@example.com", 0, 5));
		assertEquals(0, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementBetween() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.decrement("order", "test1@example.com", 2, 5));
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	@Test
	public void decrementFromGreaterTo() {
		insertTestValues();
		DBCollection collection = TestConnection.getCollection("order");
		assertNull(OrderOperations.decrement("order", "test1@example.com", 5, 3));
		assertEquals(1, collection.findOne(new BasicDBObject("_id", 1)).get("order"));
		assertEquals(4, collection.findOne(new BasicDBObject("_id", 2)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 3)).get("order"));
		assertEquals(7, collection.findOne(new BasicDBObject("_id", 4)).get("order"));
		assertEquals(3, collection.findOne(new BasicDBObject("_id", 5)).get("order"));
	}

	private void insertTestValues() {
		DBCollection collection = TestConnection.getCollection("order");
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("_id", 1);
		values.put("owner", "test1@example.com");
		values.put("order", 1);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 2);
		values.put("owner", "test1@example.com");
		values.put("order", 5);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 3);
		values.put("owner", "test1@example.com");
		values.put("order", 4);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 4);
		values.put("owner", "test2@example.com");
		values.put("order", 7);
		collection.insert(new BasicDBObject(values));
		values.put("_id", 5);
		values.put("owner", "test3@example.com");
		values.put("order", 3);
		collection.insert(new BasicDBObject(values));
	}
}
