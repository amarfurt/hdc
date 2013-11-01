package search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.search.TextSearch;
import utils.search.TextSearch.SearchResult;
import utils.TextSearchTestHelper;

public class TextSearchTest {

	@Before
	public void setUp() throws Exception {
		TextSearch.connectToTest();
		TextSearch.clearIndex();
		TextSearch.createIndex();
		TextSearchTestHelper.refreshIndex();
	}

	@After
	public void tearDown() {
		TextSearch.close();
	}

	@Test
	public void index() throws ElasticSearchException, IOException {
		assertEquals(0, TextSearchTestHelper.count());
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(recordId, data);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count());
		assertEquals(data, TextSearchTestHelper.getData(recordId));
	}

	@Test
	public void searchEmptyIndex() {
		int numVisibleRecords = 10;
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(numVisibleRecords);
		for (int i = 0; i < numVisibleRecords; i++) {
			visibleRecordIds.add(new ObjectId());
		}
		String search = "test";
		List<SearchResult> result = TextSearch.search(search, visibleRecordIds);
		assertEquals(0, result.size());
	}

	@Test
	public void indexAndSearch() throws ElasticSearchException, IOException {
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(recordId, data);
		int numVisibleRecords = 10;
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(numVisibleRecords + 1);
		for (int i = 0; i < numVisibleRecords; i++) {
			visibleRecordIds.add(new ObjectId());
		}
		visibleRecordIds.add(recordId);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count());
		String search = "test";
		List<SearchResult> result = TextSearch.search(search, visibleRecordIds);
		assertEquals(1, result.size());
		assertEquals(recordId.toString(), result.get(0).id);
		assertTrue(result.get(0).score > 0);
		assertEquals(data, result.get(0).data);
	}

	@Test
	public void ranking() throws ElasticSearchException, IOException {
		int numVisibleRecords = 10;
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(numVisibleRecords + 1);
		for (int i = 0; i < numVisibleRecords; i++) {
			visibleRecordIds.add(new ObjectId());
		}

		ObjectId firstId = new ObjectId();
		String firstData = "Test data";
		TextSearch.add(firstId, firstData);
		visibleRecordIds.add(firstId);

		ObjectId secondId = new ObjectId();
		String secondData = "Test data 2";
		TextSearch.add(secondId, secondData);
		visibleRecordIds.add(secondId);

		ObjectId thirdId = new ObjectId();
		String thirdData = "Unrelated";
		TextSearch.add(thirdId, thirdData);
		visibleRecordIds.add(thirdId);

		TextSearchTestHelper.refreshIndex();
		assertEquals(3, TextSearchTestHelper.count());

		String search = "data 2";
		List<SearchResult> result = TextSearch.search(search, visibleRecordIds);
		assertEquals(2, result.size());
		SearchResult firstResult = result.get(0);
		SearchResult secondResult = result.get(1);
		assertTrue(firstResult.score >= secondResult.score);
		assertEquals(secondId, firstResult.id);
		assertEquals(secondData, firstResult.data);
		assertEquals(firstId, secondResult.id);
	}

}
