package setup;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import utils.db.Database;
import utils.search.TextSearch;

public class DropData {

	public static void main(String[] args) {
		// connecting
		System.out.print("Connecting to MongoDB...");
		start(fakeApplication(fakeGlobal()));
		Database.connect();
		System.out.println("done.");
		System.out.print("Connecting to ElasticSearch...");
		TextSearch.connect();
		System.out.println("done.");

		// dropping old content
		System.out.print("Dropping existing MongoDB database...");
		Database.destroy();
		System.out.println("done.");
		System.out.print("Deleting existing ElasticSearch indices...");
		TextSearch.destroy();
		System.out.println("done.");

		// shutting down
		System.out.println("Shutting down...");
		Database.close();
		TextSearch.close();
		System.out.println("Finished.");
	}

}
