package utils.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

public class TextSearch {

	public static enum Type {
		USER, APP, VISUALIZATION
	}

	private static final String CLUSTER_NAME = "healthbank";
	private static final String PUBLIC = "public"; // public index visible to all users
	private static final String FIELD = "data";

	private static Node node;
	private static Client client;

	public static void connect() {
		// start a client node (holds no data)
		node = NodeBuilder.nodeBuilder().clusterName(CLUSTER_NAME).client(true).node();
		client = node.client();
	}

	public static void connectToTest() {
		node = NodeBuilder.nodeBuilder().local(true).node();
		client = node.client();
	}

	public static void close() {
		node.close();
	}

	/**
	 * Create global indices.
	 */
	public static void initialize() {
		// check whether the public index exists and create it otherwise
		if (!client.admin().indices().prepareExists(PUBLIC).execute().actionGet().isExists()) {
			client.admin().indices().prepareCreate(PUBLIC).execute().actionGet();
		}
	}

	/**
	 * Delete all indices.
	 */
	public static void destroy() {
		client.admin().indices().prepareDelete().execute().actionGet();
		client.admin().indices().prepareClearCache().execute().actionGet();
	}

	/**
	 * Create a user's index.
	 */
	private static void createIndex(ObjectId userId) throws ElasticSearchException, IOException {
		if (!client.admin().indices().prepareExists(userId.toString()).execute().actionGet().isExists()) {
			client.admin().indices().prepareCreate(userId.toString()).execute().actionGet();
		}
	}

	/**
	 * Delete a user's index.
	 */
	private static void deleteIndex(ObjectId userId) {
		if (client.admin().indices().prepareExists(userId.toString()).execute().actionGet().isExists()) {
			client.admin().indices().prepareDelete(userId.toString()).execute().actionGet();
			client.admin().indices().prepareClearCache(userId.toString()).execute().actionGet();
		}
	}

	private static String getType(Type type) {
		switch (type) {
		case USER:
			return "user";
		case APP:
			return "app";
		case VISUALIZATION:
			return "visualization";
		default:
			throw new NoSuchElementException("There is no such type.");
		}
	}

	public static void add(ObjectId userId, String type, ObjectId modelId, String data) throws ElasticSearchException,
			IOException {
		client.prepareIndex(userId.toString(), type, modelId.toString())
				.setSource(XContentFactory.jsonBuilder().startObject().field(FIELD, data).endObject()).execute()
				.actionGet();
	}

	public static void addMultiple(ObjectId userId, String type, Map<ObjectId, String> data) throws IOException {
		if (data.size() == 0) {
			return;
		}
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (ObjectId modelId : data.keySet()) {
			bulkRequest.add(client.prepareIndex(userId.toString(), type, modelId.toString()).setSource(
					XContentFactory.jsonBuilder().startObject().field(FIELD, data.get(modelId)).endObject()));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			for (BulkItemResponse response : bulkResponse) {
				// TODO error handling
				System.out.println(response.getFailureMessage());
			}
		}
	}

	public static void addPublic(Type type, ObjectId documentId, String data) throws ElasticSearchException,
			IOException {
		switch (type) {
		case USER:
			// add the user to the global user index and create an own index for the user
			client.prepareIndex(PUBLIC, getType(Type.USER), documentId.toString())
					.setSource(XContentFactory.jsonBuilder().startObject().field(FIELD, data).endObject()).execute()
					.actionGet();
			createIndex(documentId);
			break;
		case APP:
			client.prepareIndex(PUBLIC, getType(Type.APP), documentId.toString())
					.setSource(XContentFactory.jsonBuilder().startObject().field(FIELD, data).endObject()).execute()
					.actionGet();
			break;
		case VISUALIZATION:
			client.prepareIndex(PUBLIC, getType(Type.VISUALIZATION), documentId.toString())
					.setSource(XContentFactory.jsonBuilder().startObject().field(FIELD, data).endObject()).execute()
					.actionGet();
			break;
		default:
			throw new NoSuchElementException("There is no such type.");
		}
	}

	public static void delete(ObjectId userId, String type, ObjectId modelId) {
		client.prepareDelete(userId.toString(), type, modelId.toString()).execute().actionGet();
	}

	public static void deleteMultiple(ObjectId userId, String type, Set<ObjectId> modelIds) {
		if (modelIds.size() == 0) {
			return;
		}
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (ObjectId modelId : modelIds) {
			bulkRequest.add(client.prepareDelete(userId.toString(), type, modelId.toString()));
		}
		bulkRequest.execute().actionGet();

	}

	public static void deletePublic(Type type, ObjectId documentId) {
		switch (type) {
		case USER:
			// remove the user from the global user index and delete the user's index
			ListenableActionFuture<DeleteResponse> deleteRequest = client.prepareDelete(PUBLIC, getType(Type.USER),
					documentId.toString()).execute();
			deleteIndex(documentId);
			deleteRequest.actionGet();
			break;
		case APP:
			client.prepareDelete(PUBLIC, getType(Type.APP), documentId.toString()).execute().actionGet();
			break;
		case VISUALIZATION:
			client.prepareDelete(PUBLIC, getType(Type.VISUALIZATION), documentId.toString()).execute().actionGet();
			break;
		default:
			throw new NoSuchElementException("There is no such type.");
		}
	}

	public static List<SearchResult> prefixSearch() {
		// TODO
		return null;
	}

	public static List<SearchResult> searchPublic(Type type, String query) {
		SearchResponse response = client.prepareSearch(PUBLIC).setTypes(getType(type))
				.setQuery(QueryBuilders.matchQuery(FIELD, query)).execute().actionGet();
		Map<String, List<SearchResult>> searchResults = getSearchResults(response);
		if (!searchResults.containsKey(getType(type))) {
			return new ArrayList<SearchResult>();
		}
		return searchResults.get(getType(type));
	}

	public static List<SearchResult> searchRecords(ObjectId userId, Map<ObjectId, Set<ObjectId>> visibleRecords,
			String query) {
		// search in user's records
		ListenableActionFuture<SearchResponse> privateRecordsQuery = searchPrivateIndex(userId, query, "record")
				.execute();

		// search in other users' visible records
		ListenableActionFuture<SearchResponse> visibleRecordsQuery = searchVisibleRecords(visibleRecords, query)
				.execute();

		// wait for the results
		SearchResponse privateRecordsResponse = privateRecordsQuery.actionGet();
		SearchResponse visibleRecordsResponse = visibleRecordsQuery.actionGet();

		// construct response
		Map<String, List<SearchResult>> searchResults = getSearchResults(privateRecordsResponse, visibleRecordsResponse);

		// sort the results according to score
		if (!searchResults.containsKey("record")) {
			return new ArrayList<SearchResult>();
		}
		Collections.sort(searchResults.get("record"));
		return searchResults.get("record");
	}

	/**
	 * Search in all the user's data and all further visible records.
	 */
	public static Map<String, List<SearchResult>> search(ObjectId userId, Map<ObjectId, Set<ObjectId>> visibleRecords,
			String query) {
		// search in user's index
		ListenableActionFuture<SearchResponse> privateIndexQuery = searchPrivateIndex(userId, query).execute();

		// search in other users' visible records
		ListenableActionFuture<SearchResponse> visibleRecordsQuery = searchVisibleRecords(visibleRecords, query)
				.execute();

		// search in public index
		ListenableActionFuture<SearchResponse> publicIndexQuery = searchPublicIndex(query).execute();

		// wait for the results
		SearchResponse privateIndexResponse = privateIndexQuery.actionGet();
		SearchResponse visibleRecordsResponse = visibleRecordsQuery.actionGet();
		SearchResponse publicIndexResponse = publicIndexQuery.actionGet();

		// construct response
		Map<String, List<SearchResult>> searchResults = getSearchResults(privateIndexResponse, visibleRecordsResponse,
				publicIndexResponse);

		// sort the results of the records according to its score
		// rest is already sorted
		if (searchResults.containsKey("record")) {
			Collections.sort(searchResults.get("record"));
		}
		return searchResults;
	}

	private static SearchRequestBuilder searchPrivateIndex(ObjectId userId, String query, String... types) {
		SearchRequestBuilder builder = client.prepareSearch(userId.toString()).setQuery(
				QueryBuilders.matchQuery(FIELD, query));
		if (types != null) {
			builder.setTypes(types);
		}
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder searchPublicIndex(String query) {
		SearchRequestBuilder builder = client.prepareSearch(PUBLIC).setQuery(QueryBuilders.matchQuery(FIELD, query));
		return addHighlighting(builder);
	}

	/**
	 * Builds the search request for other users' visible records.
	 * @param visibleRecords Key: User id, Value: Set of record ids of records that are visible
	 */
	private static SearchRequestBuilder searchVisibleRecords(Map<ObjectId, Set<ObjectId>> visibleRecords, String query) {
		String[] queriedIndices = new String[visibleRecords.size()];
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		int i = 0;
		for (ObjectId curUserId : visibleRecords.keySet()) {
			queriedIndices[i++] = curUserId.toString();
			visibleRecordIds.addAll(visibleRecords.get(curUserId));
		}
		String[] recordIds = new String[visibleRecordIds.size()];
		int j = 0;
		for (ObjectId recordId : visibleRecordIds) {
			recordIds[j++] = recordId.toString();
		}
		SearchRequestBuilder builder = client.prepareSearch(queriedIndices)
				.setQuery(QueryBuilders.matchQuery(FIELD, query))
				.setFilter(FilterBuilders.idsFilter("record").ids(recordIds));
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder addHighlighting(SearchRequestBuilder builder) {
		return builder.addHighlightedField(FIELD, 150).setHighlighterPreTags("<span class=\"text-info\"><strong>")
				.setHighlighterPostTags("</strong></span>");
	}

	private static Map<String, List<SearchResult>> getSearchResults(SearchResponse... responses) {
		Map<String, List<SearchResult>> searchResults = new HashMap<String, List<SearchResult>>();
		for (SearchResponse response : responses) {
			for (SearchHit hit : response.getHits()) {
				// add result list to map if not present for this type
				if (!searchResults.containsKey(hit.getType())) {
					searchResults.put(hit.getType(), new ArrayList<SearchResult>());
				}

				// construct search result
				SearchResult searchResult = new SearchResult();
				searchResult.id = hit.getId();
				searchResult.score = hit.getScore();
				searchResult.data = (String) hit.getSource().get(FIELD);
				if (!hit.getHighlightFields().isEmpty()) {
					for (String field : hit.getHighlightFields().keySet()) {
						Text[] fragments = hit.getHighlightFields().get(field).getFragments();
						if (fragments.length > 0) {
							searchResult.highlighted = fragments[0].toString();
							for (int k = 1; k < fragments.length; k++) {
								searchResult.highlighted += "<br>" + fragments[k].toString();
							}
						}
					}
				}

				// add search result to result list of respective type
				searchResults.get(hit.getType()).add(searchResult);
			}
		}
		return searchResults;
	}

}
