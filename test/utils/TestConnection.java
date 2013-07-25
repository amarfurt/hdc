package utils;

import controllers.database.Connection;

/**
 * Make certain operations available to test classes.
 * @author amarfurt
 * 
 */
public class TestConnection extends Connection {

	public static void dropDatabase() {
		Connection.dropDatabase();
	}

}
