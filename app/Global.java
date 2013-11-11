import play.Application;
import play.GlobalSettings;
import utils.Connection;
import utils.search.TextSearch;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Connect to production database
		Connection.connect();

		// Connect to search cluster
		TextSearch.connect();
	}

	@Override
	public void onStop(Application app) {
		// Close connection to database
		Connection.close();

		// Close connection to search cluster
		TextSearch.close();
	}

}
