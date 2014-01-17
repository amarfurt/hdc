package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.App;
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
import views.html.details.app;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Apps extends Controller {

	public static Result details(String appIdString) {
		return ok(app.render(new ObjectId(request().username())));
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

		// get apps
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractSet(json.get("fields"));
		List<App> apps;
		try {
			apps = new ArrayList<App>(App.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(apps);
		return ok(Json.toJson(apps));
	}

	public static Result install(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("apps").get();
		try {
			User user = User.get(properties, fields);
			user.apps.add(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstall(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("apps").get();
		try {
			User user = User.get(properties, fields);
			user.apps.remove(new ObjectId(appIdString));
			User.set(userId, "apps", user.apps);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result isInstalled(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		Map<String, Object> properties = new ChainedMap<String, Object>().put("_id", userId).put("apps", appId).get();
		boolean isInstalled = User.exists(properties);
		return ok(Json.toJson(isInstalled));
	}

}
