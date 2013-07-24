import controllers.database.Connection;
import play.Application;
import play.GlobalSettings;

public class Global extends GlobalSettings {

	@Override
	public void onStart(Application app) {
		// Create connection to production database
		Connection.connect();
	}
	
	@Override
	public void onStop(Application app) {
		// Close connection to database
		Connection.close();
	}

}
