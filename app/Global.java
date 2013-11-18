import play.Application;
import play.GlobalSettings;
import utils.db.Database;
import utils.search.TextSearch;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Connect to production database
		Database.connect();

		// Connect to search cluster
		TextSearch.connect();
	}

	@Override
	public void onStop(Application app) {
		// Close connection to database
		Database.close();

		// Close connection to search cluster
		TextSearch.close();
	}

}
