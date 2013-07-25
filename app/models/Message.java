package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class Message {

	// public Long id; // set own id?
	public ObjectId _id; // use MongoDB id?
	public String sender; // email of sender
	public String receiver; // email of receiver
	public String datetime;
	public String title;
	public String content;

	public static List<Message> findSentTo(User user) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<Message> messages = new ArrayList<Message>();
		DBObject cur = null;
		DBCursor cursor = Connection.getCursor("messages");
		while (cursor.hasNext()) {
			cur = cursor.next();
			if (cur.get("receiver").equals(user.email)) {
				messages.add(ModelConversion.mapToModel(Message.class, cur.toMap()));
			}
		}
		return messages;
	}

}
