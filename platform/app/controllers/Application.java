package controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.Routes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.welcome;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {

	public static Result welcome() {
		return ok(welcome.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result authenticate() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "email", "password");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		String email = json.get("email").asText();
		String password = json.get("password").asText();
		Map<String, String> emailQuery = new ChainedMap<String, String>().put("email", email).get();
		User user;
		try {
			if (!User.exists(emailQuery)) {
				return badRequest("Invalid user or password.");
			} else {
				user = User.get(emailQuery, new ChainedSet<String>().add("password").get());
				if (!User.authenticationValid(password, user.password)) {
					return badRequest("Invalid user or password.");
				}
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// user authenticated
		session().clear();
		session("id", user._id.toString());
		return ok(routes.Messages.index().url());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result register() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "email", "firstName", "lastName", "password");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		String email = json.get("email").asText();
		String firstName = json.get("firstName").asText();
		String lastName = json.get("lastName").asText();
		String password = json.get("password").asText();
		if (User.exists(new ChainedMap<String, String>().put("email", email).get())) {
			return badRequest("A user with this email address already exists.");
		}

		// create the user
		User user = new User();
		user._id = new ObjectId();
		user.email = email;
		user.name = firstName + " " + lastName;
		try {
			user.password = User.encrypt(password);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		user.visible = new HashMap<String, Set<ObjectId>>();
		user.apps = new HashSet<ObjectId>();
		user.visualizations = new HashSet<ObjectId>();
		try {
			User.add(user);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		session().clear();
		session("id", user._id.toString());
		return ok(routes.Messages.index().url());
	}

	public static Result logout() {
		session().clear();
		return redirect(routes.Application.welcome());
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"jsRoutes",
				// Application
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				// Apps
				controllers.routes.javascript.Apps.details(),
				controllers.routes.javascript.Apps.get(),
				controllers.routes.javascript.Apps.install(),
				controllers.routes.javascript.Apps.uninstall(),
				controllers.routes.javascript.Apps.isInstalled(),
				controllers.routes.javascript.Apps.getCreateUrl(),
				// Visualizations
				controllers.routes.javascript.Visualizations.details(),
				controllers.routes.javascript.Visualizations.get(),
				controllers.routes.javascript.Visualizations.install(),
				controllers.routes.javascript.Visualizations.uninstall(),
				controllers.routes.javascript.Visualizations.isInstalled(),
				controllers.routes.javascript.Visualizations.getUrl(),
				// Messages
				controllers.routes.javascript.Messages.details(),
				controllers.routes.javascript.Messages.get(),
				// Records
				controllers.routes.javascript.Records.details(),
				controllers.routes.javascript.Records.create(),
				controllers.routes.javascript.Records.get(),
				controllers.routes.javascript.Records.getVisibleRecords(),
				controllers.routes.javascript.Records.getDetailsUrl(),
				controllers.routes.javascript.Records.search(),
				controllers.routes.javascript.Records.updateSpaces(),
				controllers.routes.javascript.Records.updateSharing(),
				// Circles
				controllers.routes.javascript.Circles.get(),
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addUsers(),
				controllers.routes.javascript.Circles.removeMember(),
				// Spaces
				controllers.routes.javascript.Spaces.get(), controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				// Users
				controllers.routes.javascript.Users.get(),
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				// Market
				controllers.routes.javascript.Market.registerApp(),
				controllers.routes.javascript.Market.registerVisualization(),
				// Global search
				controllers.routes.javascript.GlobalSearch.index(),
				controllers.routes.javascript.GlobalSearch.search(),
				controllers.routes.javascript.GlobalSearch.complete()));
	}

}
