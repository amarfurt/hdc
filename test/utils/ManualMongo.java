package utils;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class ManualMongo {

	public static void main(String[] args) {
		String collection = "circles";
		String field = "order";
		int value = 1;

		start(fakeApplication(fakeGlobal()));
		Connection.connect();
		DBCollection coll = Connection.getCollection(collection);
		WriteResult wr = coll.updateMulti(new BasicDBObject("_id", new ObjectId("5229ad72e4b0ab9711afb0af")), new BasicDBObject("$set", new BasicDBObject(field, value)));
		if (wr.getLastError().getErrorMessage() == null) {
			System.out.println("Successful.");
		} else {
			System.out.println("Error: " + wr.getLastError().getErrorMessage());
		}
	}

}
