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
import utils.DateTimeUtils;
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
		return ok(routes.News.index().url());
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
		try {
			if (User.exists(new ChainedMap<String, String>().put("email", email).get())) {
				return badRequest("A user with this email address already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
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
		user.tokens = new HashMap<String, Map<String, String>>();
		user.visualizations = new HashSet<ObjectId>();
		user.messages = new HashMap<String, Set<ObjectId>>();
		user.messages.put("inbox", new HashSet<ObjectId>());
		user.messages.put("archive", new HashSet<ObjectId>());
		user.messages.put("trash", new HashSet<ObjectId>());
		user.login = DateTimeUtils.now();
		user.news = new HashSet<ObjectId>();
		user.pushed = new HashSet<ObjectId>();
		user.shared = new HashSet<ObjectId>();
		try {
			User.add(user);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		session().clear();
		session("id", user._id.toString());
		return ok(routes.News.index().url());
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
				controllers.routes.javascript.Apps.getUrl(),
				controllers.routes.javascript.Apps.requestTokensOAuth2(),
				// Visualizations
				controllers.routes.javascript.Visualizations.details(),
				controllers.routes.javascript.Visualizations.get(),
				controllers.routes.javascript.Visualizations.install(),
				controllers.routes.javascript.Visualizations.uninstall(),
				controllers.routes.javascript.Visualizations.isInstalled(),
				controllers.routes.javascript.Visualizations.getUrl(),
				// News
				controllers.routes.javascript.News.get(),
				controllers.routes.javascript.News.hide(),
				// Messages
				controllers.routes.javascript.Messages.details(),
				controllers.routes.javascript.Messages.get(),
				controllers.routes.javascript.Messages.send(),
				controllers.routes.javascript.Messages.move(),
				controllers.routes.javascript.Messages.remove(),
				controllers.routes.javascript.Messages.delete(),
				// Records
				controllers.routes.javascript.Records.filter(),
				controllers.routes.javascript.Records.details(),
				controllers.routes.javascript.Records.create(),
				controllers.routes.javascript.Records.importRecords(),
				controllers.routes.javascript.Records.get(),
				controllers.routes.javascript.Records.getVisibleRecords(),
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
				controllers.routes.javascript.Spaces.get(),
				controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				controllers.routes.javascript.Spaces.getToken(),
				// Users
				controllers.routes.javascript.Users.get(),
				controllers.routes.javascript.Users.getCurrentUser(),
				controllers.routes.javascript.Users.search(),
				controllers.routes.javascript.Users.loadContacts(),
				controllers.routes.javascript.Users.complete(),
				controllers.routes.javascript.Users.clearPushed(),
				controllers.routes.javascript.Users.clearShared(),
				// Market
				controllers.routes.javascript.Market.registerApp(),
				controllers.routes.javascript.Market.registerVisualization(),
				// Global search
				controllers.routes.javascript.GlobalSearch.index(),
				controllers.routes.javascript.GlobalSearch.search(),
				controllers.routes.javascript.GlobalSearch.complete()));
	}

}
