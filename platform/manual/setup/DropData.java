package setup;

import utils.search.Search;

public class DropData {

	/**
	 * Drop ElasticSearch data.
	 */
	public static void main(String[] args) {
		// connecting
		System.out.print("Connecting to ElasticSearch...");
		Search.connect();
		System.out.println("done.");

		// dropping old content
		System.out.print("Deleting existing ElasticSearch indices...");
		Search.destroy();
		System.out.println("done.");

		// shutting down
		System.out.println("Shutting down...");
		Search.close();
		System.out.println("Finished.");
	}

}
