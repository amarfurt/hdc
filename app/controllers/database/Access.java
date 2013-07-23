package controllers.database;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Access {
	
	public static String findUsers() throws UnknownHostException {
		Connection con = new Connection();
		DB db = con.connect();
		DBCollection collection = db.getCollection("users");
		DBCursor cursor = collection.find();
		String names = "";
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			names += object.get("name") + ", ";
		}
		return names.substring(0, names.length() - 2);
	}

}
