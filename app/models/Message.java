package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class Message extends SearchableModel {

	public ObjectId sender;
	public ObjectId receiver;
	public String datetime;
	public String title;
	public String content;

	public static List<Message> findSentTo(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Message> messages = new ArrayList<Message>();
		DBObject query = new BasicDBObject("receiver", userId);
		DBCursor result = Connection.getCollection("messages").find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			messages.add(ModelConversion.mapToModel(Message.class, cur.toMap()));
		}
		return messages;
	}

}
