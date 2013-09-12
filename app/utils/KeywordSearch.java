package utils;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class KeywordSearch {

	public static <T> List<T> searchByType(Class<T> modelClass, String collection, String search, int limit)
			throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		List<T> results = new ArrayList<T>();
		DBObject query = new BasicDBObject();
		query.put("tags", new BasicDBObject("$all", split(search)));
		DBCursor cursor = Connection.getCollection(collection).find(query).limit(limit);
		while (cursor.hasNext()) {
			DBObject cur = cursor.next();
			results.add(ModelConversion.mapToModel(modelClass, cur.toMap()));
		}
		return results;
	}

	private static String[] split(String search) {
		return search.toLowerCase().split(" ");
	}

}
