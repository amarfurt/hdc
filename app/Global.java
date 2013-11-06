import play.Application;
import play.GlobalSettings;
import utils.Connection;
import utils.search.TextSearch;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Create connection to production database
		Connection.connect();

		// Start up search cluster and connect to it
		TextSearch.start();
		TextSearch.connect();
	}

	@Override
	public void onStop(Application app) {
		// Close connection to database
		Connection.close();

		// Close connection to search cluster and shut it down
		TextSearch.close();
		TextSearch.shutdown();
	}

}
