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

import utils.SearchTestHelper;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;
import utils.search.SearchResult;

public class SearchTest {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ManualTest {
	}

	/**
	 * Manual testing because of timeout error for subsequent tests that call the Search module (even if its not
	 * connected).
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Starting Search tests.");
		SearchTest tester = new SearchTest();
		for (Method method : SearchTest.class.getMethods()) {
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
		Search.connectToTest();
		Thread.sleep(1000);
		Search.destroy();
		Search.initialize();
		SearchTestHelper.refreshIndex();
	}

	@After
	public void tearDown() {
		Search.close();
	}

	@ManualTest
	public void indexUser() throws SearchException {
		assertEquals(0, SearchTestHelper.count("public", "user"));
		ObjectId userId = addUser();
		assertEquals(1, SearchTestHelper.count("public", "user"));
		assertNotNull(SearchTestHelper.getTitle("public", "user", userId));
		assertNotNull(SearchTestHelper.getContent("public", "user", userId));
	}

	private ObjectId addUser() throws SearchException {
		ObjectId userId = new ObjectId();
		Search.addPublic(Type.USER, userId, "Test User", "test@example.com");
		SearchTestHelper.refreshIndex();
		return userId;
	}

	@ManualTest
	public void indexRecord() throws SearchException {
		ObjectId userId = addUser();
		assertEquals(0, SearchTestHelper.count(userId.toString(), "record"));
		ObjectId recordId = new ObjectId();
		String title = "Test title";
		String content = "Test content";
		Search.add(userId, "record", recordId, title, content);
		SearchTestHelper.refreshIndex();
		assertEquals(1, SearchTestHelper.count(userId.toString(), "record"));
		assertEquals(title, SearchTestHelper.getTitle(userId.toString(), "record", recordId));
		assertEquals(content, SearchTestHelper.getContent(userId.toString(), "record", recordId));
	}

	@ManualTest
	public void searchEmptyIndex() throws SearchException {
		ObjectId userId1 = addUser();
		ObjectId userId2 = addUser();
		Map<String, Set<ObjectId>> visibleRecords = new HashMap<String, Set<ObjectId>>();
		visibleRecords.put(userId2.toString(), new HashSet<ObjectId>());
		String query = "title";
		Map<String, List<SearchResult>> result = Search.search(userId1, visibleRecords, query);
		assertEquals(0, result.size());
	}

	@ManualTest
	public void indexAndSearchOwnIndex() throws SearchException {
		ObjectId userId = addUser();
		ObjectId recordId = new ObjectId();
		String title = "Test title";
		Search.add(userId, "record", recordId, title);
		SearchTestHelper.refreshIndex();
		assertEquals(1, SearchTestHelper.count(userId.toString(), "record"));
		String query = "title";
		HashMap<String, Set<ObjectId>> visibleRecords = new HashMap<String, Set<ObjectId>>();
		Map<String, List<SearchResult>> result = Search.search(userId, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(1, result.get("record").size());
		assertEquals(recordId.toString(), result.get("record").get(0).id);
		assertTrue(result.get("record").get(0).score > 0);
		assertEquals(title, result.get("record").get(0).title);
	}

	@ManualTest
	public void indexAndSearchOtherIndex() throws SearchException {
		ObjectId userId1 = addUser();
		ObjectId userId2 = addUser();
		ObjectId recordId = new ObjectId();
		String title = "Test title";
		Search.add(userId1, "record", recordId, title);
		SearchTestHelper.refreshIndex();
		assertEquals(1, SearchTestHelper.count(userId1.toString(), "record"));
		String query = "title";
		Map<String, Set<ObjectId>> visibleRecords = new HashMap<String, Set<ObjectId>>();
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		visibleRecordIds.add(recordId);
		visibleRecords.put(userId1.toString(), visibleRecordIds);
		Map<String, List<SearchResult>> result = Search.search(userId2, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(1, result.get("record").size());
		assertEquals(recordId.toString(), result.get("record").get(0).id);
		assertTrue(result.get("record").get(0).score > 0);
		assertEquals(title, result.get("record").get(0).title);
	}

	@ManualTest
	public void ranking() throws SearchException {
		ObjectId userId = addUser();
		ObjectId recordId1 = new ObjectId();
		String title1 = "Test title 1";
		Search.add(userId, "record", recordId1, title1);
		ObjectId recordId2 = new ObjectId();
		String title2 = "Test title 2";
		Search.add(userId, "record", recordId2, title2);
		ObjectId recordId3 = new ObjectId();
		String title3 = "Unrelated";
		Search.add(userId, "record", recordId3, title3);
		SearchTestHelper.refreshIndex();
		assertEquals(3, SearchTestHelper.count(userId.toString(), "record"));
		String query = "title 2";
		Map<String, Set<ObjectId>> visibleRecords = new HashMap<String, Set<ObjectId>>();
		Map<String, List<SearchResult>> result = Search.search(userId, visibleRecords, query);
		assertEquals(1, result.size());
		assertTrue(result.containsKey("record"));
		assertEquals(2, result.get("record").size());
		SearchResult firstResult = result.get("record").get(0);
		SearchResult secondResult = result.get("record").get(1);
		assertTrue(firstResult.score >= secondResult.score);
		assertEquals(recordId2, firstResult.id);
		assertEquals(title2, firstResult.title);
		assertEquals(recordId1, secondResult.id);
	}

	@ManualTest
	public void complete() throws SearchException {
		ObjectId userId = addUser();
		ObjectId recordId = new ObjectId();
		String title = "Test title";
		String content = "Test content";
		Search.add(userId, "record", recordId, title, content);
		SearchTestHelper.refreshIndex();
		String query = "title";
		Map<String, List<SearchResult>> completions = Search.complete(userId, query);
		assertEquals(1, completions.size());
		assertTrue(completions.containsKey("record"));
		assertEquals(1, completions.get("record").size());
		SearchResult completion = completions.get("record").get(0);
		assertTrue(completion.score > 0);
		assertEquals(recordId, completion.id);
		assertEquals(title, completion.title);
	}

}
