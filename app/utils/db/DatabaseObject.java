package utils.db;

import models.App;
import models.Circle;
import models.Message;
import models.Model;
import models.Record;
import models.Space;
import models.User;
import models.Visualization;
import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public abstract class DatabaseObject {

	public static enum Type {
		USER, RECORD, MESSAGE, SPACE, CIRCLE, APP, VISUALIZATION
	}

	protected Type type;
	protected DBObject query;

	public DatabaseObject(Type type) {
		this.type = type;
		query = new BasicDBObject();
	}

	/**
	 * Add an equality selection to the query.
	 */
	public void query(String key, Object value) {
		query.put(key, value);
	}

	/**
	 * Get the name of the type's collection.
	 */
	public DBCollection getCollection() {
		switch (type) {
		case USER:
			return Database.getCollection("users");
		case RECORD:
			return Database.getCollection("records");
		case MESSAGE:
			return Database.getCollection("messages");
		case SPACE:
			return Database.getCollection("spaces");
		case CIRCLE:
			return Database.getCollection("circles");
		case APP:
			return Database.getCollection("apps");
		case VISUALIZATION:
			return Database.getCollection("visualizations");
		default:
			return null;
		}
	}

	/**
	 * Convert the db object to its corresponding class.
	 */
	public Model toModel(DBObject dbObject) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		switch (type) {
		case USER:
			return ModelConversion.mapToModel(User.class, dbObject.toMap());
		case RECORD:
			return ModelConversion.mapToModel(Record.class, dbObject.toMap());
		case MESSAGE:
			return ModelConversion.mapToModel(Message.class, dbObject.toMap());
		case SPACE:
			return ModelConversion.mapToModel(Space.class, dbObject.toMap());
		case CIRCLE:
			return ModelConversion.mapToModel(Circle.class, dbObject.toMap());
		case APP:
			return ModelConversion.mapToModel(App.class, dbObject.toMap());
		case VISUALIZATION:
			return ModelConversion.mapToModel(Visualization.class, dbObject.toMap());
		default:
			return null;
		}
	}

}
