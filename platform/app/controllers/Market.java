package controllers;

import models.App;
import models.ModelException;
import models.Visualization;

import org.bson.types.ObjectId;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.market;
import views.html.dialogs.registerapp;
import views.html.dialogs.registervisualization;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Market extends Controller {

	public static Result index() {
		return ok(market.render());
	}

	public static Result registerAppForm() {
		return ok(registerapp.render());
	}

	public static Result registerVisualizationForm() {
		return ok(registervisualization.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result registerApp(String type) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			if (type.equals("create")) {
				JsonValidation.validate(json, "name", "description", "createUrl", "detailsUrl");
			} else if (type.equals("oauth1")) {
				JsonValidation.validate(json, "name", "description", "authorizationUrl", "accessTokenUrl", "consumerKey", "detailsUrl");
			} else if (type.equals("oauth2")) {
				JsonValidation.validate(json, "name", "description", "authorizationUrl", "accessTokenUrl", "consumerKey", "consumerSecret", "scopeParameters", "detailsUrl");
			} else {
				return badRequest("Unknown app type.");
			}
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		try {
			if (App.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("An app with the same name already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create new app
		App app = new App();
		app._id = new ObjectId();
		app.creator = userId;
		app.name = name;
		app.description = json.get("description").asText();
		app.spotlighted = false;
		app.detailsUrl = json.get("detailsUrl").asText();
		app.type = type;

		// fill in specific fields
		if (type.equals("create")) {
			app.createUrl = json.get("createUrl").asText();
		} else if (type.equals("oauth1")) {
			app.authorizationUrl = json.get("authorizationUrl").asText();
			app.accessTokenUrl = json.get("accessTokenUrl").asText();
			app.consumerKey = json.get("consumerKey").asText();
		} else if (type.equals("oauth2")) {
			app.authorizationUrl = json.get("authorizationUrl").asText();
			app.accessTokenUrl = json.get("accessTokenUrl").asText();
			app.consumerKey = json.get("consumerKey").asText();
			app.consumerSecret = json.get("consumerSecret").asText();
			app.scopeParameters = json.get("scopeParameters").asText();
		}

		// add app to the platform
		try {
			App.add(app);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(routes.Market.index().url());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result registerVisualization() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "name", "description", "url");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		try {
			if (Visualization.exists(new ChainedMap<String, Object>().put("creator", userId).put("name", name).get())) {
				return badRequest("A visualization with the same name already exists.");
			}
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create new visualization
		Visualization visualization = new Visualization();
		visualization._id = new ObjectId();
		visualization.creator = userId;
		visualization.name = name;
		visualization.description = json.get("description").asText();
		visualization.spotlighted = false;
		visualization.url = json.get("url").asText();
		try {
			Visualization.add(visualization);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(routes.Market.index().url());
	}

}
