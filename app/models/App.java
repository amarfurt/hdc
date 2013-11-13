package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class App extends Model implements Comparable<App> {

	private static final String collection = "apps";

	public ObjectId creator;
	public String name;
	public String description;

	@Override
	public int compareTo(App o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return name;
	}

	public static App find(ObjectId appId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("_id", appId);
		DBObject result = Connection.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(App.class, result.toMap());
	}

	public static List<App> findInstalledBy(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		Set<ObjectId> appIds = new HashSet<ObjectId>();
		// TODO: User.findAppsInstalledBy(userId);
		List<App> apps = new ArrayList<App>();
		for (ObjectId appId : appIds) {
			apps.add(find(appId));
		}
		Collections.sort(apps);
		return apps;
	}

}
