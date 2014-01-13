package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.ModelException;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.details.visualization;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Visualizations extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Visualization> visualizations;
		try {
			visualizations = new ArrayList<Visualization>(User.findVisualizations(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(visualizations);
		return ok(Json.toJson(visualizations));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("visualizations")) {
			return badRequest("Request parameter 'visualizations' not found.");
		}
		// TODO add fields selector
		// else if (!json.has("fields")) {
		// return badRequest("Request parameter 'fields' not found.");
		// }

		// get visualizations
		List<ObjectId> visualizationIds = new ArrayList<ObjectId>();
		Iterator<JsonNode> iterator = json.get("visualizations").iterator();
		while (iterator.hasNext()) {
			visualizationIds.add(new ObjectId(iterator.next().asText()));
		}
		List<Visualization> visualizations = new ArrayList<Visualization>();
		try {
			for (ObjectId visualizationId : visualizationIds) {
				visualizations.add(Visualization.find(visualizationId));
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(visualizations);
		return ok(Json.toJson(visualizations));
	}

	public static Result getSpotlighted() {
		List<Visualization> visualizations;
		try {
			visualizations = new ArrayList<Visualization>(Visualization.findSpotlighted());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(visualizations);
		return ok(Json.toJson(visualizations));
	}

	public static Result details(String visualizationIdString) {
		return ok(visualization.render(new ObjectId(request().username())));
	}

	public static Result getUrl(String visualizationIdString) {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String visualizationServer = Play.application().configuration().getString("plugins.server");
		String url = "http://" + visualizationServer + "/visualizations/" + visualizationId + "/" + Visualization.getUrl(visualizationId);
		return ok(url);
	}

	public static Result install(String visualizationIdString) {
		try {
			User.addVisualization(new ObjectId(request().username()), new ObjectId(visualizationIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstall(String visualizationIdString) {
		try {
			User.removeVisualization(new ObjectId(request().username()), new ObjectId(visualizationIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result isInstalled(String visualizationIdString) {
		boolean isInstalled = User.hasVisualization(new ObjectId(request().username()), new ObjectId(
				visualizationIdString));
		return ok(Json.toJson(isInstalled));
	}

}
