import controllers.database.Connection;
import play.Application;
import play.GlobalSettings;
import utils.LoadData;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Create connection to production database
		Connection.connect();
		// TODO REMOVE, only for testing the interface
		// also drops database
		LoadData.load();
	}
	
	@Override
	public void onStop(Application app) {
		// Close connection to database
		Connection.close();
	}

}
