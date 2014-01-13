package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchResult;
import views.html.details.user;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Users extends Controller {

	public static Result details(String userIdString) {
		return ok(user.render(new ObjectId(request().username())));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("users")) {
			return badRequest("Request parameter 'users' not found.");
		}
		// TODO add fields selector
		// else if (!json.has("fields")) {
		// return badRequest("Request parameter 'fields' not found.");
		// }

		// get users
		List<ObjectId> userIds = new ArrayList<ObjectId>();
		Iterator<JsonNode> iterator = json.get("users").iterator();
		while (iterator.hasNext()) {
			userIds.add(new ObjectId(iterator.next().asText()));
		}
		List<User> users = new ArrayList<User>();
		try {
			for (ObjectId userId : userIds) {
				users.add(User.find(userId));
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

	public static Result search(String query) {
		// TODO use caching/incremental retrieval of results (scrolls)
		List<SearchResult> searchResults = Search.searchPublic(Type.USER, query);
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			userIds.add(new ObjectId(searchResult.id));
		}

		// remove own entry, if present
		userIds.remove(new ObjectId(request().username()));

		// TODO get name for ids, not whole user objects
		List<User> users = new ArrayList<User>();
		try {
			users.addAll(User.find(userIds));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

}
