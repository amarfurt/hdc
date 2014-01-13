package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.App;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.details.app;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Apps extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<App> apps;
		try {
			apps = new ArrayList<App>(User.findApps(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(apps);
		return ok(Json.toJson(apps));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("apps")) {
			return badRequest("Request parameter 'apps' not found.");
		}
		// TODO add fields selector
		// else if (!json.has("fields")) {
		// return badRequest("Request parameter 'fields' not found.");
		// }

		// get apps
		List<ObjectId> appIds = new ArrayList<ObjectId>();
		Iterator<JsonNode> iterator = json.get("apps").iterator();
		while (iterator.hasNext()) {
			appIds.add(new ObjectId(iterator.next().asText()));
		}
		List<App> apps = new ArrayList<App>();
		try {
			for (ObjectId appId : appIds) {
				apps.add(App.find(appId));
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(apps);
		return ok(Json.toJson(apps));
	}

	public static Result getSpotlighted() {
		List<App> apps;
		try {
			apps = new ArrayList<App>(App.findSpotlighted());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(apps);
		return ok(Json.toJson(apps));
	}

	public static Result details(String appIdString) {
		return ok(app.render(new ObjectId(request().username())));
	}

	public static Result install(String appIdString) {
		try {
			User.addApp(new ObjectId(request().username()), new ObjectId(appIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstall(String appIdString) {
		try {
			User.removeApp(new ObjectId(request().username()), new ObjectId(appIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result isInstalled(String appIdString) {
		boolean isInstalled = User.hasApp(new ObjectId(request().username()), new ObjectId(appIdString));
		return ok(Json.toJson(isInstalled));
	}

}
