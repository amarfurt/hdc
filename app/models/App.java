package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class App extends Model implements Comparable<App> {

	private static final String collection = "apps";

	public ObjectId creator;
	public String name;
	public String description;

	@Override
	public int compareTo(App other) {
		return this.name.compareTo(other.name);
	}

	public static boolean exists(ObjectId creatorId, String name) {
		DBObject query = new BasicDBObject("creator", creatorId);
		query.put("name", name);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
	}

	public static App find(ObjectId appId) throws ConversionException {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject result = Database.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(App.class, result.toMap());
	}

	public static Set<App> findInstalledBy(ObjectId userId) throws ConversionException {
		Set<ObjectId> appIds = new HashSet<ObjectId>();
		// TODO: User.findAppsInstalledBy(userId);
		Set<App> apps = new HashSet<App>();
		for (ObjectId appId : appIds) {
			apps.add(find(appId));
		}
		return apps;
	}

}
