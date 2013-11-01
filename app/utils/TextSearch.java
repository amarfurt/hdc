package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;

public class TextSearch {

	private static final String CLUSTER_NAME = "elasticsearch";

	private static Node node;
	private static Client client;

	public static void connect() {
		node = NodeBuilder.nodeBuilder().clusterName(CLUSTER_NAME).node();
		client = node.client();
	}

	public static void connectToTest() {
		node = NodeBuilder.nodeBuilder().local(true).node();
		client = node.client();
	}

	public static void createIndex(ObjectId userId) throws ElasticSearchException, IOException {
		client.admin().indices().prepareCreate(userId.toString()).execute().actionGet();
	}

	public static void clearIndex(ObjectId userId) {
		IndicesExistsResponse response = client.admin().indices().prepareExists(userId.toString()).execute()
				.actionGet();
		if (response.isExists()) {
			client.admin().indices().prepareDelete(userId.toString()).execute().actionGet();
		}
		client.admin().indices().prepareClearCache().execute().actionGet();
	}

	public static void close() {
		node.close();
	}

	public static void add(ObjectId userId, String type, ObjectId modelId, String field, Object data)
			throws ElasticSearchException, IOException {
		IndexData indexData = new IndexData();
		indexData.id = modelId;
		indexData.field = field;
		indexData.data = data;
		add(userId, type, indexData);
	}

	public static void add(ObjectId userId, String type, IndexData data) throws ElasticSearchException, IOException {
		client.prepareIndex(userId.toString(), type, data.id.toString())
				.setSource(XContentFactory.jsonBuilder().startObject().field(data.field, data.data).endObject())
				.execute().actionGet();
	}

	public static void addMultiple(ObjectId userId, String type, Set<IndexData> data) throws IOException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (IndexData indexData : data) {
			bulkRequest.add(client.prepareIndex(userId.toString(), type, indexData.id.toString()).setSource(
					XContentFactory.jsonBuilder().startObject().field(indexData.field, indexData.data).endObject()));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			for (BulkItemResponse response : bulkResponse) {
				// TODO error handling
				System.out.println(response.getFailureMessage());
			}
		}
	}

	public static void delete(ObjectId userId, String type, ObjectId modelId) {
		client.prepareDelete(userId.toString(), type, modelId.toString()).execute().actionGet();
	}

	public static void deleteMultiple(ObjectId userId, String type, Set<ObjectId> modelIds) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (ObjectId modelId : modelIds) {
			bulkRequest.add(client.prepareDelete(userId.toString(), type, modelId.toString()));
		}
		bulkRequest.execute().actionGet();
	}

	public static List<SearchResult> prefixSearch() {
		// TODO
		return null;
	}

	public static Map<String, List<SearchResult>> search(ObjectId userId, Set<VisibleRecords> visibleRecords,
			String search) {
		// search in user's index
		ListenableActionFuture<SearchResponse> userQuery = client.prepareSearch(userId.toString())
				.setQuery(QueryBuilders.multiMatchQuery(search, "data", "keywords"))
				.addHighlightedField("data", 150)
				.addHighlightedField("keywords")
				.setHighlighterPreTags("<span class=\"text-info\"><strong>")
				.setHighlighterPostTags("</strong></span>")
				.execute();

		// search in visible records, i.e. in other user's indices
		String[] queriedIndices = new String[visibleRecords.size()];
		int i = 0;
		for (VisibleRecords visible : visibleRecords) {
			queriedIndices[i++] = visible.userId.toString();
		}
		ListenableActionFuture<SearchResponse> visibleQuery = client.prepareSearch()
				.setQuery(QueryBuilders.indicesQuery(QueryBuilders.matchQuery("data", search), queriedIndices))
				.addHighlightedField("data", 150)
				.setHighlighterPreTags("<span class=\"text-info\"><strong>")
				.setHighlighterPostTags("</strong></span>")
				.execute();

		// wait for the results
		SearchResponse userResponse = userQuery.actionGet();
		SearchResponse visibleResponse = visibleQuery.actionGet();
		SearchResponse[] responses = new SearchResponse[] { userResponse, visibleResponse };

		// construct response
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
				searchResult.data = (String) hit.getSource().get("data");
				if (!hit.getHighlightFields().isEmpty()) {
					for (String field : hit.getHighlightFields().keySet()) {
						Text[] fragments = hit.getHighlightFields().get(field).getFragments();
						if (fragments.length > 0) {
							searchResult.highlighted = fragments[0].toString();
							for (int j = 1; j < fragments.length; j++) {
								searchResult.highlighted += "<br>" + fragments[j].toString();
							}
						}
					}
				}

				// add search result to result list of respective type
				searchResults.get(hit.getType()).add(searchResult);
			}
		}

		// sort the results of the records according to its score
		// done automatically for the rest
		Collections.sort(searchResults.get("record"));
		return searchResults;
	}

	public static class VisibleRecords {

		public ObjectId userId;
		public Set<ObjectId> visibleRecords;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof VisibleRecords) {
				return this.userId.equals(((VisibleRecords) obj).userId);
			}
			return false;
		}

	}

	public static class IndexData {

		public ObjectId id;
		public String field;
		public Object data;

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IndexData) {
				return this.id.equals(((IndexData) obj).id);
			}
			return false;
		}

	}

	public static class SearchResult implements Comparable<SearchResult> {

		public String id;
		public float score;
		public String data;
		public String highlighted;

		@Override
		public int compareTo(SearchResult o) {
			// higher score is "less", i.e. earlier in sorted list
			return (int) -Math.signum(score - o.score);
		}

	}
}
