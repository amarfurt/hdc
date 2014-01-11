package controllers;

import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.Routes;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.welcome;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBList;

public class Application extends Controller {

	public static Result welcome() {
		return ok(welcome.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result authenticate() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("email")) {
			return badRequest("Request parameter 'email' not found.");
		} else if (!json.has("password")) {
			return badRequest("Request parameter 'password' not found.");
		}

		// validate request
		String email = json.get("email").asText();
		String password = json.get("password").asText();
		try {
			if (!User.exists(email) || !User.authenticationValid(email, password)) {
				return badRequest("Invalid user or password.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// user authenticated
		session().clear();
		session("id", User.getId(email).toString());
		return ok(routes.Messages.index().url());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result register() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("email")) {
			return badRequest("Request parameter 'email' not found.");
		} else if (!json.has("firstName")) {
			return badRequest("Request parameter 'firstName' not found.");
		} else if (!json.has("lastName")) {
			return badRequest("Request parameter 'lastName' not found.");
		} else if (!json.has("password")) {
			return badRequest("Request parameter 'password' not found.");
		}

		// validate request
		String email = json.get("email").asText();
		String firstName = json.get("firstName").asText();
		String lastName = json.get("lastName").asText();
		String password = json.get("password").asText();
		if (User.exists(email)) {
			return badRequest("A user with this email address already exists.");
		}

		// create the user
		User newUser = new User();
		newUser.email = email;
		newUser.name = firstName + " " + lastName;
		try {
			newUser.password = User.encrypt(password);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		newUser.visible = new BasicDBList();
		newUser.apps = new BasicDBList();
		newUser.visualizations = new BasicDBList();
		try {
			User.add(newUser);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		session().clear();
		session("id", newUser._id.toString());
		return ok(routes.Messages.index().url());
	}

	public static Result logout() {
		session().clear();
		return redirect(routes.Application.welcome());
	}

	@Security.Authenticated(Secured.class)
	public static ObjectId getCurrentUserId() {
		return new ObjectId(request().username());
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter(
				"jsRoutes",
				// Application
				controllers.routes.javascript.Application.authenticate(),
				controllers.routes.javascript.Application.register(),
				// Apps
				controllers.routes.javascript.Apps.fetch(),
				controllers.routes.javascript.Apps.get(),
				controllers.routes.javascript.Apps.getSpotlighted(),
				controllers.routes.javascript.Apps.details(),
				controllers.routes.javascript.Apps.install(),
				controllers.routes.javascript.Apps.uninstall(),
				controllers.routes.javascript.Apps.isInstalled(),
				// Visualizations
				controllers.routes.javascript.Visualizations.fetch(),
				controllers.routes.javascript.Visualizations.get(),
				controllers.routes.javascript.Visualizations.getSpotlighted(),
				controllers.routes.javascript.Visualizations.details(),
				controllers.routes.javascript.Visualizations.install(),
				controllers.routes.javascript.Visualizations.uninstall(),
				controllers.routes.javascript.Visualizations.isInstalled(),
				controllers.routes.javascript.Visualizations.getUrl(),
				// Messages
				controllers.routes.javascript.Messages.fetch(),
				controllers.routes.javascript.Messages.get(),
				controllers.routes.javascript.Messages.index(),
				controllers.routes.javascript.Messages.details(),
				// Records
				controllers.routes.javascript.Records.fetch(),
				controllers.routes.javascript.Records.get(),
				controllers.routes.javascript.Records.details(),
				controllers.routes.javascript.Records.getDetailsUrl(),
				controllers.routes.javascript.Records.create(),
				controllers.routes.javascript.Records.search(),
				controllers.routes.javascript.Records.updateSpaces(),
				controllers.routes.javascript.Records.updateSharing(),
				// Circles
				controllers.routes.javascript.Circles.fetch(),
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addUsers(),
				controllers.routes.javascript.Circles.removeMember(),
				controllers.routes.javascript.Circles.loadContacts(),
				// Spaces
				controllers.routes.javascript.Spaces.fetch(),
				controllers.routes.javascript.Spaces.add(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.addRecords(),
				// Users
				controllers.routes.javascript.Users.get(),
				controllers.routes.javascript.Users.details(),
				controllers.routes.javascript.Users.search(),
				// Global search
				controllers.routes.javascript.GlobalSearch.index(),
				controllers.routes.javascript.GlobalSearch.search(),
				controllers.routes.javascript.GlobalSearch.complete()));
	}

}
