package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.elasticsearch.ElasticSearchException;

import utils.ModelConversion;
import utils.db.Database;
import utils.search.TextSearch;
import utils.search.TextSearch.Type;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class Visualization extends Model implements Comparable<Visualization> {

	private static final String collection = "visualizations";
	private static final String DEFAULT_VISUALIZATION = "Record List";

	public ObjectId creator;
	public String name;
	public String description;
	public String url;

	@Override
	public int compareTo(Visualization o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public String toString() {
		return name;
	}

	public static String getDefaultVisualization() {
		return DEFAULT_VISUALIZATION;
	}

	public static String getName(ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", visualizationId);
		DBObject projection = new BasicDBObject("name", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("name");
	}

	public static ObjectId getId(String visualizationName) {
		DBObject query = new BasicDBObject("name", visualizationName);
		DBObject projection = new BasicDBObject("_id", 1);
		return (ObjectId) Database.getCollection(collection).findOne(query, projection).get("_id");
	}

	public static String getURL(ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", visualizationId);
		DBObject projection = new BasicDBObject("url", 1);
		return (String) Database.getCollection(collection).findOne(query, projection).get("url");
	}

	public static Visualization find(ObjectId visualizationId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("_id", visualizationId);
		DBObject result = Database.getCollection(collection).findOne(query);
		return ModelConversion.mapToModel(Visualization.class, result.toMap());
	}

	public static List<Visualization> findSpotlighted() throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<Visualization> visualizations = new ArrayList<Visualization>();
		// TODO return only spotlighted visualizations
		// for now: return all visualizations
		DBCursor result = Database.getCollection(collection).find();
		while (result.hasNext()) {
			DBObject cur = result.next();
			visualizations.add(ModelConversion.mapToModel(Visualization.class, cur.toMap()));
		}
		// TODO sort
		return visualizations;
	}

	public static String add(Visualization newVisualization) throws IllegalArgumentException, IllegalAccessException,
			ElasticSearchException, IOException {
		if (visualizationWithSameNameExists(newVisualization.name)) {
			return "A visualization with this name already exists.";
		}
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newVisualization));
		WriteResult result = Database.getCollection(collection).insert(insert);
		newVisualization._id = (ObjectId) insert.get("_id");
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}

		// add to search index (concatenate name and description)
		TextSearch.addPublic(Type.VISUALIZATION, newVisualization._id, newVisualization.name + " "
				+ newVisualization.description);
		return null;
	}

	public static String delete(ObjectId visualizationId) {
		if (!visualizationExists(visualizationId)) {
			return "No visualizations with this id exists.";
		}

		// remove from search index
		TextSearch.deletePublic(Type.VISUALIZATION, visualizationId);

		// TODO only hide or remove from all users (including deleting their spaces associated with it)?

		// remove from visualizations
		DBObject remove = new BasicDBObject("_id", visualizationId);
		WriteResult result = Database.getCollection(collection).remove(remove);
		return result.getLastError().getErrorMessage();
	}

	private static boolean visualizationExists(ObjectId visualizationId) {
		DBObject query = new BasicDBObject("_id", visualizationId);
		return Database.getCollection(collection).findOne(query) != null;
	}

	public static boolean visualizationWithSameNameExists(String name) {
		DBObject query = new BasicDBObject("name", name);
		return Database.getCollection(collection).findOne(query) != null;
	}

}
