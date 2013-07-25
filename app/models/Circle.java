package models;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

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

}
