package utils.search;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

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
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.Suggest.Suggestion;

public class TextSearch {

	public static enum Type {
		USER, APP, VISUALIZATION
	}

	private static final String CLUSTER_NAME = "healthdata";
	private static final String PUBLIC = "public"; // public index visible to all users
	private static final String TITLE = "title";
	private static final String SUGGEST = "suggest"; // used for autocompletion
	private static final String CONTENT = "content";

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
		if (node != null) {
			node.close();
		}
	}

	/**
	 * Create global indices.
	 */
	public static void initialize() {
		// return if not connected
		if (client == null) {
			return;
		}

		// check whether the public index exists and create it otherwise
		if (!client.admin().indices().prepareExists(PUBLIC).execute().actionGet().isExists()) {
			client.admin().indices().prepareCreate(PUBLIC).execute().actionGet();
		}
	}

	/**
	 * Delete all indices.
	 */
	public static void destroy() {
		// return if not connected
		if (client == null) {
			return;
		}

		client.admin().indices().prepareDelete().execute().actionGet();
		client.admin().indices().prepareClearCache().execute().actionGet();
	}

	/**
	 * Create a user's index.
	 */
	private static void createIndex(ObjectId userId) {
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

	@Deprecated
	public static void add(ObjectId userId, String type, ObjectId modelId, String title) throws SearchException {
		add(userId, type, modelId, title, null);
	}

	/**
	 * Add document to search index. Title is used for autocompletion, content for full-text search.
	 */
	public static void add(ObjectId userId, String type, ObjectId modelId, String title, String content)
			throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		String[] split = title.split("[ ,\\.]+");
		try {
			client.prepareIndex(userId.toString(), type, modelId.toString())
					.setSource(
							jsonBuilder().startObject().field(TITLE, title).startObject(SUGGEST).array("input", split)
									.field("output", title).endObject().field(CONTENT, content).endObject()).execute()
					.actionGet();
		} catch (ElasticSearchException e) {
			throw new SearchException(e);
		} catch (IOException e) {
			throw new SearchException(e);
		}
	}

	@Deprecated
	public static void addMultiple(ObjectId userId, String type, Map<ObjectId, String> data) throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		// return if no data is passed
		if (data.size() == 0) {
			return;
		}

		// BulkRequestBuilder bulkRequest = client.prepareBulk();
		// for (ObjectId modelId : data.keySet()) {
		// try {
		// bulkRequest.add(client.prepareIndex(userId.toString(), type, modelId.toString()).setSource(
		// jsonBuilder().startObject().field(FIELD, data.get(modelId)).endObject()));
		// } catch (IOException e) {
		// throw new SearchException(e);
		// }
		// }
		// BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		// if (bulkResponse.hasFailures()) {
		// for (BulkItemResponse response : bulkResponse) {
		// // TODO error handling
		// System.out.println(response.getFailureMessage());
		// }
		// }
	}

	@Deprecated
	public static void addPublic(Type type, ObjectId documentId, String data) throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		// switch (type) {
		// case USER:
		// // add the user to the global user index and create an own index for the user
		// try {
		// client.prepareIndex(PUBLIC, getType(Type.USER), documentId.toString())
		// .setSource(jsonBuilder().startObject().field(FIELD, data).endObject()).execute().actionGet();
		// } catch (ElasticSearchException e) {
		// throw new SearchException(e);
		// } catch (IOException e) {
		// throw new SearchException(e);
		// }
		// createIndex(documentId);
		// break;
		// case APP:
		// try {
		// client.prepareIndex(PUBLIC, getType(Type.APP), documentId.toString())
		// .setSource(jsonBuilder().startObject().field(FIELD, data).endObject()).execute().actionGet();
		// } catch (ElasticSearchException e) {
		// throw new SearchException(e);
		// } catch (IOException e) {
		// throw new SearchException(e);
		// }
		// break;
		// case VISUALIZATION:
		// try {
		// client.prepareIndex(PUBLIC, getType(Type.VISUALIZATION), documentId.toString())
		// .setSource(jsonBuilder().startObject().field(FIELD, data).endObject()).execute().actionGet();
		// } catch (ElasticSearchException e) {
		// throw new SearchException(e);
		// } catch (IOException e) {
		// throw new SearchException(e);
		// }
		// break;
		// default:
		// throw new NoSuchElementException("There is no such type.");
		// }
	}

	public static void addPublic(Type type, ObjectId documentId, String title, String content) throws SearchException {
		// return if not connected
		if (client == null) {
			return;
		}

		switch (type) {
		case USER:
			// add the user to the global user index and create an own index for the user
			try {
				client.prepareIndex(PUBLIC, getType(Type.USER), documentId.toString())
						.setSource(jsonBuilder().startObject().field(TITLE, title).field(CONTENT, content).endObject())
						.execute().actionGet();
			} catch (ElasticSearchException e) {
				throw new SearchException(e);
			} catch (IOException e) {
				throw new SearchException(e);
			}
			createIndex(documentId);
			break;
		case APP:
			try {
				client.prepareIndex(PUBLIC, getType(Type.APP), documentId.toString())
						.setSource(jsonBuilder().startObject().field(TITLE, title).field(CONTENT, content).endObject())
						.execute().actionGet();
			} catch (ElasticSearchException e) {
				throw new SearchException(e);
			} catch (IOException e) {
				throw new SearchException(e);
			}
			break;
		case VISUALIZATION:
			try {
				client.prepareIndex(PUBLIC, getType(Type.VISUALIZATION), documentId.toString())
						.setSource(jsonBuilder().startObject().field(TITLE, title).field(CONTENT, content).endObject())
						.execute().actionGet();
			} catch (ElasticSearchException e) {
				throw new SearchException(e);
			} catch (IOException e) {
				throw new SearchException(e);
			}
			break;
		default:
			throw new NoSuchElementException("There is no such type.");
		}
	}

	public static void delete(ObjectId userId, String type, ObjectId modelId) {
		// return if not connected
		if (client == null) {
			return;
		}

		client.prepareDelete(userId.toString(), type, modelId.toString()).execute().actionGet();
	}

	public static void deleteMultiple(ObjectId userId, String type, Set<ObjectId> modelIds) {
		// return if not connected
		if (client == null) {
			return;
		}

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
		// return if not connected
		if (client == null) {
			return;
		}

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

	public static List<String> complete(ObjectId userId, String query) {
		List<String> results = new ArrayList<String>();

		// return if not connected
		if (client == null) {
			return results;
		}

		// only autocomplete in public and user's own index
		ListenableActionFuture<SuggestResponse> publicSuggest = client.prepareSuggest(PUBLIC).setSuggestText(query)
				.execute();
		ListenableActionFuture<SuggestResponse> userSuggest = client.prepareSuggest(userId.toString())
				.setSuggestText(query).execute();
		SuggestResponse publicResponse = publicSuggest.actionGet();
		SuggestResponse userResponse = userSuggest.actionGet();
		System.out.println("User response: " + userResponse.toString());
		for (Suggestion suggestion : publicResponse.getSuggest()) {
			System.out.println(suggestion.toString());
			results.add(suggestion.toString());
		}
		for (Suggestion suggestion : userResponse.getSuggest()) {
			System.out.println(suggestion.toString());
			results.add(suggestion.toString());
		}
		return results;
	}

	public static List<SearchResult> searchPublic(Type type, String query) {
		// return if not connected
		if (client == null) {
			return null;
		}

		SearchResponse response = client.prepareSearch(PUBLIC).setTypes(getType(type))
				.setQuery(QueryBuilders.multiMatchQuery(query, TITLE, CONTENT)).execute().actionGet();
		Map<String, List<SearchResult>> searchResults = getSearchResults(response);
		if (!searchResults.containsKey(getType(type))) {
			return new ArrayList<SearchResult>();
		}
		return searchResults.get(getType(type));
	}

	public static List<SearchResult> searchRecords(ObjectId userId, Map<ObjectId, Set<ObjectId>> visibleRecords,
			String query) {
		// return if not connected
		if (client == null) {
			return null;
		}

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
		// return if not connected
		if (client == null) {
			return null;
		}

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
				QueryBuilders.multiMatchQuery(query, TITLE, CONTENT));
		if (types != null) {
			builder.setTypes(types);
		}
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder searchPublicIndex(String query) {
		SearchRequestBuilder builder = client.prepareSearch(PUBLIC).setQuery(
				QueryBuilders.multiMatchQuery(query, TITLE, CONTENT));
		return addHighlighting(builder);
	}

	/**
	 * Builds the search request for other users' visible records.
	 * 
	 * @param visibleRecords Key: User id, Value: Set of record ids of records that are visible
	 */
	private static SearchRequestBuilder searchVisibleRecords(Map<ObjectId, Set<ObjectId>> visibleRecords, String query) {
		List<String> queriedIndices = new ArrayList<String>();
		Set<ObjectId> visibleRecordIds = new HashSet<ObjectId>();
		for (ObjectId curUserId : visibleRecords.keySet()) {
			if (visibleRecords.get(curUserId).size() > 0) {
				queriedIndices.add(curUserId.toString());
				visibleRecordIds.addAll(visibleRecords.get(curUserId));
			}
		}
		String[] recordIds = new String[visibleRecordIds.size()];
		int j = 0;
		for (ObjectId recordId : visibleRecordIds) {
			recordIds[j++] = recordId.toString();
		}
		String[] indicesArray = new String[queriedIndices.size()];
		SearchRequestBuilder builder = client.prepareSearch(queriedIndices.toArray(indicesArray))
				.setQuery(QueryBuilders.multiMatchQuery(query, TITLE, CONTENT))
				.setFilter(FilterBuilders.idsFilter("record").ids(recordIds));
		return addHighlighting(builder);
	}

	private static SearchRequestBuilder addHighlighting(SearchRequestBuilder builder) {
		return builder.addHighlightedField(CONTENT, 150).setHighlighterPreTags("<span class=\"text-info\"><strong>")
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
				searchResult.title = (String) hit.getSource().get("title");
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
