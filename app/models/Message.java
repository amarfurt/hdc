package models;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class Message extends Model implements Comparable<Message> {

	private static final String collection = "messages";

	public ObjectId sender;
	public ObjectId receiver;
	public String created;
	public String title;
	public String content;

	@Override
	public int compareTo(Message o) {
		// newest first
		return -this.created.compareTo(o.created);
	}

	@Override
	public String toString() {
		return User.getName(sender) + ": " + title;
	}

	public static Message find(ObjectId messageId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("_id", messageId);
		DBObject result = Database.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(Message.class, result.toMap());
	}

	public static List<Message> findSentTo(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Message> messages = new ArrayList<Message>();
		DBObject query = new BasicDBObject("receiver", userId);
		DBCursor result = Database.getCollection(collection).find(query);
		while (result.hasNext()) {
			DBObject cur = result.next();
			messages.add(ModelConversion.mapToModel(Message.class, cur.toMap()));
		}
		return messages;
	}

}
