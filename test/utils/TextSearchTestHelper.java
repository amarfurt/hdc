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
		String INDEX = (String) makeAccessible("INDEX");

		// costly operation: only used in tests
		client.admin().indices().refresh(new RefreshRequest(INDEX)).actionGet();
	}

	public static String getData(ObjectId recordId) {
		// get values of TextSearch class
		Client client = (Client) makeAccessible("client");
		String INDEX = (String) makeAccessible("INDEX");
		String TYPE = (String) makeAccessible("TYPE");
		String FIELD = (String) makeAccessible("FIELD");

		// get the data field
		GetResponse actionGet = client.prepareGet(INDEX, TYPE, recordId.toString()).execute().actionGet();
		if (actionGet.isExists()) {
			return (String) actionGet.getSource().get(FIELD);
		} else {
			return null;
		}
	}

	public static long count() {
		// get values of TextSearch class
		Client client = (Client) makeAccessible("client");
		String INDEX = (String) makeAccessible("INDEX");
		String TYPE = (String) makeAccessible("TYPE");

		// get the count of indexed items
		CountResponse countResponse = client.prepareCount(INDEX).setTypes(TYPE).setQuery(QueryBuilders.matchAllQuery())
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
