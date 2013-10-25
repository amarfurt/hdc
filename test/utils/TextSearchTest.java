package utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.TextSearch.SearchResult;

public class TextSearchTest {

	@Before
	public void setUp() throws Exception {
		TextSearch.connectToTest();
		TextSearch.clearIndex();
		TextSearch.createIndex();
	}

	@After
	public void tearDown() {
		TextSearch.close();
	}

	@Test
	public void index() throws ElasticSearchException, IOException {
		ObjectId recordId = new ObjectId();
		String data = "Test data.";
		TextSearch.add(recordId, data);
	}

	@Test
	public void searchEmptyCollection() {
		int numVisibleRecords = 10;
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(numVisibleRecords);
		for (int i = 0; i < numVisibleRecords; i++) {
			visibleRecordIds.add(new ObjectId());
		}
		assertEquals(numVisibleRecords, visibleRecordIds.size());
		String search = "test";
		List<SearchResult> result = TextSearch.search(search, visibleRecordIds);
		assertEquals(0, result.size());
	}

	@Test
	public void indexAndSearch() throws ElasticSearchException, IOException {
		ObjectId recordId = new ObjectId();
		String data = "Test data.";
		TextSearch.add(recordId, data);
		int numVisibleRecords = 10;
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>(numVisibleRecords + 1);
		for (int i = 0; i < numVisibleRecords; i++) {
			visibleRecordIds.add(new ObjectId());
		}
		visibleRecordIds.add(recordId);
		assertEquals(numVisibleRecords + 1, visibleRecordIds.size());
		String search = "test";
		List<SearchResult> result = TextSearch.search(search, visibleRecordIds);
		assertEquals(1, result.size());
		assertEquals(recordId.toString(), result.get(0).id);
	}
}
