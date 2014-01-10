package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.details.user;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Users extends Controller {

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

	public static Result details(String userIdString) {
		return ok(user.render(new ObjectId(request().username())));
	}

}
