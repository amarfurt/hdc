package models;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class Circle {

	public ObjectId _id;
	public String name;
	public String owner;
	public BasicDBList members;

	public static boolean isOwner(ObjectId circleId, String email) {
		DBObject query = new BasicDBObjectBuilder().append("_id", circleId).append("owner", email).get();
		DBObject result = Connection.getCollection("circles").findOne(query);
		return (result != null);
	}

	public static boolean rename(ObjectId circleId, String newName) {
		DBObject query = new BasicDBObject("_id", circleId);
		DBObject update = new BasicDBObject("name", newName);
		WriteResult result = Connection.getCollection("circles").update(query, update);
		return (result.getLastError().getErrorMessage() == null);
	}

}
