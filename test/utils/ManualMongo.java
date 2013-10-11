package utils;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;

public class ManualMongo {

	public static void main(String[] args) {
		String collection = "records";
		String field = "tags";
		String[] value = new String[0];

		start(fakeApplication(fakeGlobal()));
		Connection.connect();
		DBCollection coll = Connection.getCollection(collection);
		WriteResult wr = coll.updateMulti(new BasicDBObject(), new BasicDBObject("$set", new BasicDBObject(field, value)));
		if (wr.getLastError().getErrorMessage() == null) {
			System.out.println("Successful.");
		} else {
			System.out.println("Error: " + wr.getLastError().getErrorMessage());
		}
	}

}
