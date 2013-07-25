package utils;

import com.mongodb.DBCollection;

import controllers.database.Connection;

/**
 * Make certain operations available to test classes.
 * @author amarfurt
 *
 */
public class TestConnection extends Connection {

	public static DBCollection getCollection(String collection) {
		return Connection.getCollection(collection);
	}
	
	public static void dropDatabase() {
		Connection.dropDatabase();
	}

}
