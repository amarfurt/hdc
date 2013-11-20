package search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;

import utils.TextSearchTestHelper;
import utils.search.SearchException;
import utils.search.SearchResult;
import utils.search.TextSearch;
import utils.search.TextSearch.Type;

public class TextSearchTest {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ManualTest {
	}

	/**
	 * Manual testing because of timeout error for subsequent tests that call the TextSearch module (even if its not
	 * connected).
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Starting TextSearch tests.");
		TextSearchTest tester = new TextSearchTest();
		for (Method method : TextSearchTest.class.getMethods()) {
			if (method.isAnnotationPresent(ManualTest.class)) {
				tester.setUp();
				System.out.print("Testing " + method.getName() + "...");
				method.invoke(tester, new Object[] {});
				System.out.println("successful.");
				tester.tearDown();
			}
		}
		System.out.println("All tests successful!");
	}

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

	@ManualTest
	public void indexUser() throws SearchException {
		assertEquals(0, TextSearchTestHelper.count(null, "user"));
		ObjectId userId = addUser();
		assertEquals(1, TextSearchTestHelper.count(null, "user"));
		assertNotNull(TextSearchTestHelper.getData(null, "user", userId));
	}

	private ObjectId addUser() throws SearchException {
		ObjectId userId = new ObjectId();
		String data = "test@example.com Test User";
		TextSearch.addPublic(Type.USER, userId, data);
		TextSearchTestHelper.refreshIndex();
		return userId;
	}

	@ManualTest
	public void indexRecord() throws SearchException {
		ObjectId userId = addUser();
		assertEquals(0, TextSearchTestHelper.count(userId, "record"));
		ObjectId recordId = new ObjectId();
		String data = "Test data";
		TextSearch.add(userId, "record", recordId, data);
		TextSearchTestHelper.refreshIndex();
		assertEquals(1, TextSearchTestHelper.count(userId, "record"));
		assertEquals(data, TextSearchTestHelper.getData(userId, "record", recordId));
	}

	@ManualTest
	public void searchEmptyIndex() throws SearchException {
		ObjectId userId1 = addUser();
		ObjectId userId2 = addUser();
		Map<ObjectId, Set<ObjectId>> visibleRecords = new HashMap<ObjectId, Set<ObjectId>>();
		visibleRecords.put(userId2, new HashSet<ObjectId>());
		String query = "data";
		Map<String, List<SearchResult>> result = TextSearch.search(userId1, visibleRecords, query);
		assertEquals(0, result.size());
	}

	@ManualTest
	public void indexAndSearchOwnIndex() throws SearchException {
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

	@ManualTest
	public void indexAndSearchOtherIndex() throws SearchException {
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

	@ManualTest
	public void ranking() throws SearchException {
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
