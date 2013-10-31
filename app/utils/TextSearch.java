package utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
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

	private static final String CLUSTER_NAME = "elasticsearch";
	private static final String INDEX = "records";
	private static final String TYPE = "record";
	private static final String FIELD = "data";

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

	public static void createIndex() {
		client.admin().indices().prepareCreate(INDEX).execute().actionGet();
	}

	public static void clearIndex() {
		IndicesExistsResponse response = client.admin().indices().prepareExists(INDEX).execute().actionGet();
		if (response.isExists()) {
			client.admin().indices().prepareDelete(INDEX).execute().actionGet();
		}
		client.admin().indices().prepareClearCache().execute().actionGet();
	}

	public static void close() {
		node.close();
	}

	public static void add(ObjectId recordId, String data) throws ElasticSearchException, IOException {
		client.prepareIndex(INDEX, TYPE, recordId.toString())
				.setSource(XContentFactory.jsonBuilder().startObject().field(FIELD, data).endObject()).execute()
				.actionGet();
	}

	public static void addMultiple(Map<ObjectId, String> data) throws IOException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (ObjectId recordId : data.keySet()) {
			bulkRequest.add(client.prepareIndex(INDEX, TYPE, recordId.toString()).setSource(
					XContentFactory.jsonBuilder().startObject().field(FIELD, data.get(recordId)).endObject()));
		}
		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		if (bulkResponse.hasFailures()) {
			for (BulkItemResponse response : bulkResponse) {
				// TODO error handling
				System.out.println(response.getFailureMessage());
			}
		}
	}

	public static void delete(ObjectId recordId) {
		client.prepareDelete(INDEX, TYPE, recordId.toString()).execute().actionGet();
	}

	public static void deleteMultiple(List<ObjectId> recordIds) {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (ObjectId recordId : recordIds) {
			bulkRequest.add(client.prepareDelete(INDEX, TYPE, recordId.toString()));
		}
		bulkRequest.execute().actionGet();
	}

	public static List<SearchResult> search(String search, Set<ObjectId> visibleRecordIds) {
		String[] visibleIds = new String[visibleRecordIds.size()];
		int i = 0;
		for (ObjectId recordId : visibleRecordIds) {
			visibleIds[i++] = recordId.toString();
		}
		SearchResponse response = client.prepareSearch(INDEX).setTypes(TYPE)
				.setQuery(QueryBuilders.matchQuery(FIELD, search))
				.setFilter(FilterBuilders.idsFilter(TYPE).ids(visibleIds)).addHighlightedField(FIELD, 150)
				.setHighlighterPreTags("<span class=\"text-info\"><strong>").setHighlighterPostTags("</strong></span>")
				.execute().actionGet();
		List<SearchResult> searchResults = new ArrayList<SearchResult>();
		for (SearchHit hit : response.getHits()) {
			SearchResult searchResult = new SearchResult();
			searchResult.id = hit.getId();
			searchResult.score = hit.getScore();
			searchResult.data = (String) hit.getSource().get(FIELD);
			if (!hit.getHighlightFields().isEmpty()) {
				Text[] fragments = hit.getHighlightFields().get(FIELD).getFragments();
				if (fragments.length > 0) {
					searchResult.highlighted = fragments[0].toString();
					for (int j = 1; j < fragments.length; j++) {
						searchResult.highlighted += "<br>" + fragments[j].toString();
					}
				}
			}
			searchResults.add(searchResult);
		}
		Collections.sort(searchResults);
		return searchResults;
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
