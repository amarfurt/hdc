package controllers.database;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class Access {

	public static DBCursor getUsers() throws UnknownHostException {
		DB db = Connection.getDB();
		DBCollection collection = db.getCollection("users");
		return collection.find();
	}

}
