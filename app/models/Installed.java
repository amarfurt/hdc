package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.ModelConversion;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Installed extends Model {

	private static final String collection = "installed";

	// _id is the id of the user
	public BasicDBList apps;
	public BasicDBList visualizations;

	public static boolean isAppInstalledBy(ObjectId appId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		query.put("apps", appId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Connection.getCollection(collection).findOne(query, projection) != null;
	}

	public static boolean isVisualizationInstalledBy(ObjectId visualizationId, ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		query.put("visualizations", visualizationId);
		DBObject projection = new BasicDBObject("_id", 1);
		return Connection.getCollection(collection).findOne(query, projection) != null;
	}

	public static Set<ObjectId> findAppsInstalledBy(ObjectId userId) {
		Set<ObjectId> appIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("apps", 1);
		DBObject result = Connection.getCollection(collection).findOne(query, projection);
		if (result != null) {
			BasicDBList installedAppIds = (BasicDBList) result.get("apps");
			for (Object appId : installedAppIds) {
				appIds.add((ObjectId) appId);
			}
		}
		return appIds;
	}

	public static Set<ObjectId> findVisualizationsInstalledBy(ObjectId userId) {
		Set<ObjectId> visualizationIds = new HashSet<ObjectId>();
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("visualizations", 1);
		DBObject result = Connection.getCollection(collection).findOne(query, projection);
		if (result != null) {
			BasicDBList installedVisualizationIds = (BasicDBList) result.get("visualizations");
			for (Object visualizationId : installedVisualizationIds) {
				visualizationIds.add((ObjectId) visualizationId);
			}
		}
		return visualizationIds;
	}

	public static String addUser(ObjectId userId) throws IllegalArgumentException, IllegalAccessException {
		Installed installed = new Installed();
		installed._id = userId;
		installed.apps = new BasicDBList();
		installed.visualizations = new BasicDBList();
		installed.visualizations.add(Visualization.getId("List"));
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(installed));
		WriteResult result = Connection.getCollection(collection).insert(insert);
		return result.getLastError().getErrorMessage();
	}

	public static String installApp(ObjectId appId, ObjectId userId) {
		return update(collection, userId, "$addToSet", "apps", appId, false);
	}

	public static String installVisualization(ObjectId visualizationId, ObjectId userId) {
		return update(collection, userId, "$addToSet", "visualizations", visualizationId, false);
	}

	public static String uninstallApp(ObjectId appId, ObjectId userId) {
		return update(collection, userId, "$pull", "apps", appId, false);
	}

	public static String uninstallVisualization(ObjectId visualizationId, ObjectId userId) {
		return update(collection, userId, "$pull", "visualizations", visualizationId, false);
	}

	public static String deleteApp(ObjectId appId) {
		return update(collection, null, "$pull", "apps", appId, true);
	}

	public static String deleteVisualization(ObjectId visualizationId) {
		return update(collection, null, "$pull", "visualizations", visualizationId, true);
	}

	private static String update(String collection, ObjectId queryId, String operation, String updateList,
			ObjectId updateId, boolean multi) {
		DBObject query = new BasicDBObject();
		if (queryId != null) {
			query.put("_id", queryId);
		}
		DBObject update = new BasicDBObject(operation, new BasicDBObject(updateList, updateId));
		WriteResult result;
		if (!multi) {
			result = Connection.getCollection(collection).update(query, update);
		} else {
			result = Connection.getCollection(collection).updateMulti(query, update);
		}
		return result.getLastError().getErrorMessage();
	}

}
