package utils.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import models.Model;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@Deprecated
public class KeywordSearch {

	public static <T extends Model> Set<ObjectId> boundedSearch(Set<ObjectId> recordIds, String collection,
			String search, int limit) {
		Set<ObjectId> resultIds = new HashSet<ObjectId>();
		String[] terms = split(search);
		DBObject[] union = new DBObject[terms.length + 1];
		union[0] = new BasicDBObject("_id", new BasicDBObject("$in", recordIds.toArray()));
		for (int i = 0; i < terms.length; i++) {
			union[i + 1] = new BasicDBObject("tags", Pattern.compile("^" + terms[i]));
		}
		DBObject query = new BasicDBObject("$and", union);
		DBObject projection = new BasicDBObject("_id", 1);
		DBCursor result = Connection.getCollection(collection).find(query, projection).limit(limit);
		while (result.hasNext()) {
			resultIds.add((ObjectId) result.next().get("_id"));
		}
		return resultIds;
	}

	/**
	 * Return matches on prefix of keywords. Intersects the results from each search term.
	 */
	public static <T extends Model> List<T> searchByType(Class<T> modelClass, String collection, String search,
			int limit) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<T> results = new ArrayList<T>();
		String[] terms = split(search);
		DBObject[] union = new DBObject[terms.length];
		for (int i = 0; i < terms.length; i++) {
			union[i] = new BasicDBObject("tags", Pattern.compile("^" + terms[i]));
		}
		DBObject query = new BasicDBObject("$and", union);
		DBCursor cursor = Connection.getCollection(collection).find(query).limit(limit);
		while (cursor.hasNext()) {
			DBObject cur = cursor.next();
			results.add(ModelConversion.mapToModel(modelClass, cur.toMap()));
		}
		return results;
	}

	public static <T extends Model> List<T> searchInList(List<T> list, String search, int limit) {
		String[] terms = split(search);
		List<T> result = new ArrayList<T>();
		for (T cur : list) {
			boolean allFound = true;
			for (String term : terms) {
				boolean termFound = false;
//				for (Object tag : cur.tags) {
//					if (((String) tag).startsWith(term)) {
//						termFound = true;
//						break;
//					}
//				}
				if (!termFound) {
					allFound = false;
					break;
				}
			}
			if (allFound) {
				result.add(cur);
			}
		}
		return result;
	}

	/**
	 * Returns only the exact matches of search term and prefix. Intersects the results from each search term.
	 */
	public static <T extends Model> List<T> searchByTypeFullMatch(Class<T> modelClass, String collection,
			String search, int limit) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<T> results = new ArrayList<T>();
		DBObject query = new BasicDBObject("tags", new BasicDBObject("$all", split(search)));
		DBCursor cursor = Connection.getCollection(collection).find(query).limit(limit);
		while (cursor.hasNext()) {
			DBObject cur = cursor.next();
			results.add(ModelConversion.mapToModel(modelClass, cur.toMap()));
		}
		return results;
	}

	public static String[] split(String search) {
		return search.toLowerCase().split("[ ,\\+]+");
	}

}
