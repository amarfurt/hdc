package models;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.ModelConversion;
import utils.ModelConversion.ConversionException;
import utils.db.Database;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.Application;

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

	/**
	 * Validate form input data for registering a new visualization.
	 */
	public String validate() {
		if (name.isEmpty() || description.isEmpty() || url.isEmpty()) {
			return "Please fill in all fields.";
		} else if (Visualization.exists(Application.getCurrentUserId(), name)) {
			return "A visualization with the same name already exists.";
		}
		return null;
	}

	public static String getDefaultVisualization() {
		return DEFAULT_VISUALIZATION;
	}

	public static boolean exists(ObjectId creatorId, String name) {
		DBObject query = new BasicDBObject("creator", creatorId);
		query.put("name", name);
		DBObject projection = new BasicDBObject("_id", 1);
		return Database.getCollection(collection).findOne(query, projection) != null;
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

	public static Visualization find(ObjectId visualizationId) throws ModelException {
		DBObject query = new BasicDBObject("_id", visualizationId);
		DBObject result = Database.getCollection(collection).findOne(query);
		try {
			return ModelConversion.mapToModel(Visualization.class, result.toMap());
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
	}

	public static Set<Visualization> findSpotlighted() throws ModelException {
		Set<Visualization> visualizations = new HashSet<Visualization>();
		// TODO return only spotlighted visualizations
		// for now: return all visualizations
		DBCursor result = Database.getCollection(collection).find();
		while (result.hasNext()) {
			DBObject cur = result.next();
			try {
				visualizations.add(ModelConversion.mapToModel(Visualization.class, cur.toMap()));
			} catch (ConversionException e) {
				throw new ModelException(e);
			}
		}
		return visualizations;
	}

	public static void add(Visualization newVisualization) throws ModelException {
		DBObject insert;
		try {
			insert = new BasicDBObject(ModelConversion.modelToMap(newVisualization));
		} catch (ConversionException e) {
			throw new ModelException(e);
		}
		WriteResult result = Database.getCollection(collection).insert(insert);
		newVisualization._id = (ObjectId) insert.get("_id");
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());

		// add to search index
		try {
			Search.addPublic(Type.VISUALIZATION, newVisualization._id, newVisualization.name,
					newVisualization.description);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId creatorId, ObjectId visualizationId) throws ModelException {
		// remove from search index
		Search.deletePublic(Type.VISUALIZATION, visualizationId);

		// TODO only hide or remove from all users (including deleting their spaces associated with it)?

		// remove from visualizations
		DBObject remove = new BasicDBObject("_id", visualizationId);
		WriteResult result = Database.getCollection(collection).remove(remove);
		ModelException.throwIfPresent(result.getLastError().getErrorMessage());
	}

}
