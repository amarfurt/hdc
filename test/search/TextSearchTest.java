package search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.TextSearchTestHelper;
import utils.search.SearchResult;
import utils.search.TextSearch;
import utils.search.TextSearch.Type;

public class TextSearchTest {

	@Before
	public void setUp() throws Exception {
		TextSearch.connectToTest();
		Thread.sleep(1000);
		TextSearch.destroy();
		TextSearch.initialize();
		TextSearchTestHelper.refreshIndex();
	}

	@After
	public void tearDown() {
		TextSearch.close();
	}

	@Test
	public void indexUser() throws ElasticSearchException, IOException {
		assertEquals(0, TextSearchTestHelper.count(null, "user"));
		ObjectId userId = addUser();
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count(null, "user"));
		assertNotNull(TextSearchTestHelper.getData(null, "user", userId));
	}

	private ObjectId addUser() throws ElasticSearchException, IOException {
		ObjectId userId = new ObjectId();
		String data = "test@example.com Test User";
		TextSearch.addPublic(Type.USER, userId, data);
		return userId;
	}

	@Test
	public void indexRecord() throws ElasticSearchException, IOException {
		ObjectId userId = addUser();
		assertEquals(0, TextSearchTestHelper.count(userId, "record"));
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(userId, "record", recordId, data);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count(userId, "record"));
		assertEquals(data, TextSearchTestHelper.getData(userId, "record", recordId));
	}

	@Test
	public void searchEmptyIndex() throws ElasticSearchException, IOException {
		ObjectId userId1 = addUser();
		ObjectId userId2 = addUser();
		Map<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		visibleRecords.put(userId2, new HashSet<ObjectId>());
		String query = "data";
		TextSearchTestHelper.refreshIndex();
		Map<String, List<SearchResult>> result = TextSearch.search(userId1, visibleRecords, query);
		assertEquals(0, result.size());
	}

	@Test
	public void indexAndSearchOwnIndex() throws ElasticSearchException, IOException {
		ObjectId userId = addUser();
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(userId, "record", recordId, data);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count(userId, "record"));
		String query = "data";
		HashMap<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		Map<String, List<SearchResult>> result = TextSearch.search(userId, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(1, result.get("record").size());
		assertEquals(recordId.toString(), result.get("record").get(0).id);
		assertTrue(result.get("record").get(0).score > 0);
		assertEquals(data, result.get("record").get(0).data);
	}

	@Test
	public void indexAndSearchOtherIndex() throws ElasticSearchException, IOException {
		ObjectId userId1 = addUser();
		ObjectId userId2 = addUser();
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(userId1, "record", recordId, data);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count(userId1, "record"));
		String query = "data";
		Map<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		visibleRecordIds.add(recordId);
		visibleRecords.put(userId1, visibleRecordIds);
		Map<String, List<SearchResult>> result = TextSearch.search(userId2, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(1, result.get("record").size());
		assertEquals(recordId.toString(), result.get("record").get(0).id);
		assertTrue(result.get("record").get(0).score > 0);
		assertEquals(data, result.get("record").get(0).data);
	}

	@Test
	public void ranking() throws ElasticSearchException, IOException {
		ObjectId userId = addUser();
		ObjectId recordId1 = new ObjectId();
		String data1 = "Test data 1";
		TextSearch.add(userId, "record", recordId1, data1);
		ObjectId recordId2 = new ObjectId();
		String data2 = "Test data 2";
		TextSearch.add(userId, "record", recordId2, data2);
		ObjectId recordId3 = new ObjectId();
		String data3 = "Unrelated";
		TextSearch.add(userId, "record", recordId3, data3);
		TextSearchTestHelper.refreshIndex();
		assertEquals(3, TextSearchTestHelper.count(userId, "record"));
		String query = "data 2";
		Map<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		Map<String, List<SearchResult>> result = TextSearch.search(userId, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(2, result.get("record").size());
		SearchResult firstResult = result.get("record").get(0);
		SearchResult secondResult = result.get("record").get(1);
		assertTrue(firstResult.score >= secondResult.score);
		assertEquals(recordId2, firstResult.id);
		assertEquals(data2, firstResult.data);
		assertEquals(recordId1, secondResult.id);
	}

}
