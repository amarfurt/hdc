package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.App;
import models.ModelException;
import models.User;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import play.Play;
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
		return ok(app.render());
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
		return getApps(json);
	}

	static Result getApps(JsonNode json) {
		// get apps
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
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

	public static Result getCreateUrl(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("createUrl").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create reply to address and encode it with Base64
		String platformServer = Play.application().configuration().getString("platform.server");
		String replyTo = "https://" + platformServer + routes.AppsAPI.createRecord(request().username(), appIdString).url();
		String encodedReplyTo = new String(new Base64().encode(replyTo.getBytes()));

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("apps.server");
		String createUrl = app.createUrl.replace(":replyTo", encodedReplyTo);
		String url = "https://" + appServer + "/" + appIdString + "/" + createUrl;
		return ok(url);
	}

}
