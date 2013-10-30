package elasticsearch;

import java.lang.reflect.Field;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import utils.TextSearch;

/**
 * Displays all the data currently indexed by elasticsearch.
 */
public class DisplayAll {

	public static void main(String[] args) throws Exception {
		// connect to elasticsearch and make the client accessible
		TextSearch.connect();
		Field field = TextSearch.class.getDeclaredField("client");
		field.setAccessible(true);
		Client client = (Client) field.get(null);

		// waiting for previous operations to finish...
		Thread.sleep(1000);

		// scan all records and return them in chunks (scrolls)
		SearchResponse scrollResponse = client.prepareSearch("records").setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(10000)).setQuery(QueryBuilders.matchAllQuery()).setSize(100).execute()
				.actionGet(); // 100 hits per shard will be returned for each scroll
		System.out.println("Total records in index: " + scrollResponse.getHits().getTotalHits());

		// scroll until no hits are returned
		while (true) {
			scrollResponse = client.prepareSearchScroll(scrollResponse.getScrollId()).setScroll(new TimeValue(600000))
					.execute().actionGet();
			for (SearchHit hit : scrollResponse.getHits()) {
				System.out.println(hit.getId() + "\t" + hit.getSource().get("data"));
			}

			// break if no hits are returned anymore
			if (scrollResponse.getHits().getHits().length == 0) {
				break;
			}
		}

		// close connection
		TextSearch.close();
	}

}
