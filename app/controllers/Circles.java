package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Circle;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchResult;
import views.html.circles;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Circle> circles;
		try {
			circles = new ArrayList<Circle>(Circle.findOwnedBy(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(circles);
		return ok(Json.toJson(circles));
	}

	public static Result index() {
		return ok(circles.render(new ObjectId(request().username())));
	}

	public static Result details(String circleIdString) {
		return index();
	}

	public static Result add() {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = Form.form().bindFromRequest().get("name");
		if (Circle.exists(userId, name)) {
			return badRequest("A circle with this name already exists.");
		}

		// construct new circle
		Circle newCircle = new Circle();
		newCircle.name = name;
		newCircle.owner = userId;
		newCircle.members = new BasicDBList();
		newCircle.shared = new BasicDBList();
		try {
			Circle.add(newCircle);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(Json.toJson(newCircle));
	}

	public static Result delete(String circleIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// delete circle
		try {
			Circle.delete(userId, circleId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result addUsers(String circleIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("users")) {
			return badRequest("Request parameter 'users' not found.");
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// add users to circle (implicit: if not already present)
		Set<ObjectId> userIds = new HashSet<ObjectId>();
		for (JsonNode user : json.get("users")) {
			userIds.add(new ObjectId(user.asText()));
		}
		try {
			Circle.addMembers(userId, circleId, userIds);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result removeMember(String circleIdString, String memberIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// remove member from circle (implicit: if present)
		try {
			Circle.removeMember(userId, circleId, new ObjectId(memberIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Load a user's contacts.
	 */
	public static Result loadContacts() {
		ObjectId userId = new ObjectId(request().username());
		List<User> contacts;
		try {
			contacts = new ArrayList<User>(Circle.findContacts(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(contacts);
		return ok(Json.toJson(contacts));
	}

	/**
	 * Search for users matching the given query.
	 */
	public static Result searchUsers(String query) {
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
