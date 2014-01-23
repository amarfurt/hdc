package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
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
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get users
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<User> users;
		try {
			users = new ArrayList<User>(User.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

	public static Result getCurrentUser() {
		return ok(Json.toJson(new ObjectId(request().username())));
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

		// get name for ids
		Map<String, Set<ObjectId>> properties = new ChainedMap<String, Set<ObjectId>>().put("_id", userIds).get();
		Set<String> fields = new ChainedSet<String>().add("name").get();
		List<User> users;
		try {
			users = new ArrayList<User>(User.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(users);
		return ok(Json.toJson(users));
	}

	/**
	 * Make the owner's records visible to a set of users.
	 */
	static void makeVisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) throws ModelException {
		// get the visible fields for the owner from all users
		Map<String, Set<ObjectId>> properties = new ChainedMap<String, Set<ObjectId>>().put("_id", userIds).get();
		Set<String> fields = new ChainedSet<String>().add("visible." + ownerId.toString()).get();
		Set<User> users = User.getAll(properties, fields);
		for (User user : users) {
			if (user.visible.containsKey(ownerId.toString())) {
				Set<ObjectId> visibleRecords = user.visible.get(ownerId.toString());
				int originalSize = visibleRecords.size();
				visibleRecords.addAll(recordIds);

				// only update the field if some records were not visible before
				if (visibleRecords.size() > originalSize) {
					User.set(user._id, "visible." + ownerId.toString(), visibleRecords);
				}
			} else {
				User.set(user._id, "visible." + ownerId.toString(), recordIds);
			}
		}
	}

	/**
	 * Make the owner's records invisible to a set of users, if these records are not shared with them via another
	 * circle.
	 */
	static void makeInvisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds) throws ModelException {
		// get the owner's circles
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", ownerId).get();
		Set<String> fields = new ChainedSet<String>().add("members").add("shared").get();
		Set<Circle> circles = Circle.getAll(properties, fields);
		Users.makeInvisible(ownerId, recordIds, userIds, circles);
	}

	/**
	 * Use this if the owner's circles have already been loaded.
	 */
	static void makeInvisible(ObjectId ownerId, Set<ObjectId> recordIds, Set<ObjectId> userIds, Set<Circle> circles)
			throws ModelException {
		for (ObjectId userId : userIds) {
			// get the records that are still shared with this user
			Set<ObjectId> stillSharedRecords = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				if (circle.members.contains(userId)) {
					stillSharedRecords.addAll(circle.shared);
				}
			}

			// update visible field iff visible records have changed
			recordIds.removeAll(stillSharedRecords);
			if (recordIds.size() > 0) {
				User user = User.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(),
						new ChainedSet<String>().add("visible." + ownerId.toString()).get());
				Set<ObjectId> visibleRecords = user.visible.get(ownerId.toString());
				visibleRecords.removeAll(recordIds);
				User.set(userId, "visible." + ownerId.toString(), visibleRecords);
			}
		}
	}
}
