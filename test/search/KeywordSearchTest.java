package search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import models.Record;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.CreateDBObjects;
import utils.KeywordSearch;
import utils.TestConnection;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class KeywordSearchTest {

	private final String[] keywordList = { "doctor", "dentist", "runtastic", "x-ray", "image" };

	@Before
	public void setUp() throws Exception {
		start(fakeApplication(fakeGlobal()));
		TestConnection.connectToTest();
		TestConnection.dropDatabase();
		insertRecordKeywords();
	}

	@After
	public void tearDown() {
		TestConnection.close();
	}

	@Test
	public void prefixMatch() throws Exception {
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(),
				keywordList[1].substring(0, 4), 10);
		assertEquals(1, list.size());
		assertTrue(list.get(0).tags.contains(keywordList[1]));
	}

	@Test
	public void exactMatch() throws Exception {
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(), keywordList[1], 10);
		assertEquals(1, list.size());
		assertTrue(list.get(0).tags.contains(keywordList[1]));
	}

	@Test
	public void multiPrefixMatch() throws Exception {
		ObjectId userId = (ObjectId) TestConnection.getCollection("users").findOne().get("_id");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userId, userId, 2);
		DBCollection collection = TestConnection.getCollection("records");
		collection.update(new BasicDBObject("_id", recordIds[0]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[3] }))));
		collection.update(new BasicDBObject("_id", recordIds[1]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[3] }))));
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(),
				keywordList[1].substring(0, 3) + " " + keywordList[3].substring(0, 3), 10);
		assertEquals(2, list.size());
		for (Record record : list) {
			assertTrue(keywordsInTags(record, keywordList[1], keywordList[3]));
		}
	}

	@Test
	public void multiExactMatch() throws Exception {
		ObjectId userId = (ObjectId) TestConnection.getCollection("users").findOne().get("_id");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userId, userId, 2);
		DBCollection collection = TestConnection.getCollection("records");
		collection.update(new BasicDBObject("_id", recordIds[0]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[3] }))));
		collection.update(new BasicDBObject("_id", recordIds[1]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[3] }))));
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(), keywordList[1] + " "
				+ keywordList[3], 10);
		assertEquals(2, list.size());
		for (Record record : list) {
			assertTrue(keywordsInTags(record, keywordList[1], keywordList[3]));
		}
	}

	@Test
	public void multiNoMatch() throws Exception {
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(), "keywordli", 10);
		assertEquals(0, list.size());

	}

	@Test
	public void noMatch() throws Exception {
		List<Record> list = KeywordSearch.searchByType(Record.class, Record.getCollection(), "none", 10);
		assertEquals(0, list.size());
	}
	
	@Test
	public void searchInList() throws Exception {
		ObjectId userId = (ObjectId) TestConnection.getCollection("users").findOne().get("_id");
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userId, userId, 2);
		DBCollection collection = TestConnection.getCollection("records");
		collection.update(new BasicDBObject("_id", recordIds[0]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[3] }))));
		collection.update(new BasicDBObject("_id", recordIds[1]), new BasicDBObject("$push", new BasicDBObject("tags",
				new BasicDBObject("$each", new String[] { keywordList[1], keywordList[2], keywordList[3] }))));
		List<Record> list = Record.findVisible(userId);
		assertEquals(keywordList.length + 2, list.size());
		List<Record> result = KeywordSearch.searchInList(list, keywordList[1] + " " + keywordList[3], 10);
		assertEquals(2, result.size());
		for (Record record : result) {
			assertTrue(keywordsInTags(record, keywordList[1], keywordList[3]));
		}
	}

	private boolean keywordsInTags(Record record, String... keywords) {
		boolean found = false;
		for (String keyword : keywords) {
			if (record.tags.contains(keyword)) {
				found = true;
				break;
			}
		}
		return found;
	}

	private void insertRecordKeywords() throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		ObjectId[] userIds = CreateDBObjects.insertUsers(2);
		ObjectId[] recordIds = CreateDBObjects.insertRecords(userIds[1], userIds[0], keywordList.length);
		DBCollection collection = TestConnection.getCollection("records");
		for (int i = 0; i < keywordList.length; i++) {
			collection.update(new BasicDBObject("_id", recordIds[i]), new BasicDBObject("$push", new BasicDBObject(
					"tags", keywordList[i])));
		}
	}

}
