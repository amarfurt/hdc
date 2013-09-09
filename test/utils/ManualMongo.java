package utils;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class ManualMongo {

	public static void main(String[] args) {
		String collection = "spaces";
		String field = "order";
		int value = 3;

		start(fakeApplication(fakeGlobal()));
		Connection.connect();
		DBCollection coll = Connection.getCollection(collection);
		WriteResult wr = coll.updateMulti(new BasicDBObject("name", "New space"), new BasicDBObject("$set", new BasicDBObject(field, value)));
		if (wr.getLastError().getErrorMessage() == null) {
			System.out.println("Successful.");
		} else {
			System.out.println("Error: " + wr.getLastError().getErrorMessage());
		}
	}

}
