package controllers;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import models.CacheEntry;
import models.ModelException;
import models.Record;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;

// not secured, accessible from node server
public class VisualizationsAPI extends Controller {

	/**
	 * Load the data of the cached records.
	 */
	public static Result popRecords(String cacheIdString) {
		// get the cached record ids
		ObjectId cacheId = new ObjectId(cacheIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", cacheId).get();
		Set<String> fields = new ChainedSet<String>().add("expires").add("items").get();
		try {
			if (!CacheEntry.exists(properties)) {
				return badRequest("This cache entry does not exist.");
			}
			CacheEntry entry = CacheEntry.get(properties, fields);

			// remove entry from the cache if expired
			if (entry.expires < new Date().getTime()) {
				CacheEntry.delete(cacheId);
				return badRequest("This cache entry has expired.");
			}

			// remove used entry from cache
			CacheEntry.delete(cacheId);

			// get record data
			Map<String, Set<ObjectId>> recordProperties = new ChainedMap<String, Set<ObjectId>>().put("_id", entry.items).get();
			fields = new ChainedSet<String>().add("data").get();
			Set<Record> records = Record.getAll(recordProperties, fields);
			return ok(Json.toJson(records));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
	}

}
