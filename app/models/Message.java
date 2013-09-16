package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class Message {

	// public Long id;		// TODO set own id ...
	// public String id;	// ... or create an id like a composite key
	public ObjectId _id;	// use MongoDB id?
	public String sender; 	// email of sender
	public String receiver;	// email of receiver
	public String datetime;
	public String title;
	public String content;
	public BasicDBList tags;

	public static List<Message> findSentTo(String email) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<Message> messages = new ArrayList<Message>();
		DBObject query = new BasicDBObject("receiver", email);
		DBCursor result = Connection.getCollection("messages").find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			messages.add(ModelConversion.mapToModel(Message.class, cur.toMap()));
		}
		return messages;
	}

}
