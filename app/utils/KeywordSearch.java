package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import models.Model;
import models.SearchableModel;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class KeywordSearch {

	/**
	 * Return matches on prefix of keywords. Intersects the results from each search term.
	 */
	public static <T extends Model> List<T> searchByType(Class<T> modelClass, String collection, String search, int limit)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
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
	
	public static <T extends SearchableModel> List<T> searchInList(List<T> list, String search, int limit) {
		String[] terms = split(search);
		List<T> result = new ArrayList<T>();
		for (T cur : list) {
			boolean allFound = true;
			for (String term : terms) {
				boolean termFound = false;
				for (Object tag : cur.tags) {
					if (((String) tag).startsWith(term)) {
						termFound = true;
						break;
					}
				}
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
	public static <T extends Model> List<T> searchByTypeFullMatch(Class<T> modelClass, String collection, String search, int limit)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
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
