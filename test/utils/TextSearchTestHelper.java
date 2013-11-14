package utils;

import java.lang.reflect.Field;

import org.bson.types.ObjectId;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

import utils.search.TextSearch;

public class TextSearchTestHelper extends TextSearch {

	/**
	 * Refresh the index, e.g. before counting.
	 */
	public static void refreshIndex() {
		Client client = (Client) makeAccessible("client");

		// costly operation: only used in tests
		client.admin().indices().refresh(new RefreshRequest()).actionGet();
	}

	public static String getData(ObjectId userId, String type, ObjectId documentId) {
		// decide which index to use
		String index = "public";
		if (userId != null) {
			index = userId.toString();
		}

		// get values of TextSearch class
		Client client = (Client) makeAccessible("client");
		String FIELD = (String) makeAccessible("FIELD");
		GetResponse response = client.prepareGet(index, type, documentId.toString()).execute().actionGet();
		if (!response.isExists()) {
			return null;
		}
		return (String) response.getSource().get(FIELD);
	}

	public static long count(ObjectId userId, String type) {
		// decide which index to use
		String index = "public";
		if (userId != null) {
			index = userId.toString();
		}

		// get values of TextSearch class
		Client client = (Client) makeAccessible("client");

		// get the count of indexed items
		CountResponse countResponse = client.prepareCount(index).setTypes(type).setQuery(QueryBuilders.matchAllQuery())
				.execute().actionGet();
		return countResponse.getCount();
	}

	private static Object makeAccessible(String fieldName) {
		try {
			Field field = TextSearch.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(null);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

}
