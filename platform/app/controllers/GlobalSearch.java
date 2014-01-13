package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.search;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated(Secured.class)
public class GlobalSearch extends Controller {

	/**
	 * Load site and give control to JS controller.
	 */
	public static Result index(String query) {
		return ok(search.render(new ObjectId(request().username())));
	}

	/**
	 * Search in all the user's accessible data.
	 */
	public static Result search(String query) {
		ObjectId userId = new ObjectId(request().username());
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userId);
		Map<String, List<SearchResult>> searchResults = Search.search(userId, visibleRecords, query);
		return ok(Json.toJson(searchResults));
	}

	/**
	 * Suggests completions for the given query. Used by the auto-completion feature.
	 */
	public static Result complete(String query) {
		Map<String, List<SearchResult>> completions = Search.complete(new ObjectId(request().username()), query);
		List<ObjectNode> jsonRecords = new ArrayList<ObjectNode>();
		for (String type : completions.keySet()) {
			for (SearchResult completion : completions.get(type)) {
				ObjectNode datum = Json.newObject();
				datum.put("value", completion.title);
				datum.put("tokens", Json.toJson(completion.title.split("[ ,\\.]+")));
				datum.put("type", type);
				datum.put("id", completion.id);
				jsonRecords.add(datum);
			}
		}
		return ok(Json.toJson(jsonRecords));
	}
}
