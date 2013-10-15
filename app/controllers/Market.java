package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Installed;
import models.Visualization;

import org.bson.types.ObjectId;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.KeywordSearch;
import views.html.market;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Market extends Controller {

	public static Result show() {
		ObjectId userId = new ObjectId(request().username());
		List<Visualization> spotlightedVisualizations;
		try {
			spotlightedVisualizations = Visualization.findSpotlighted();
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		return ok(market.render(spotlightedVisualizations, spotlightedVisualizations, userId));
	}

	public static Result registerVisualization(String name, String description, String url, String tags) {
		if (Visualization.visualizationWithSameNameExists(name)) {
			return badRequest("A visualization with this name already exists.");
		} else if (name.isEmpty() || description.isEmpty() || url.isEmpty() || tags.isEmpty()) {
			return badRequest("Please fill out all fields.");
		}
		Visualization newVisualization = new Visualization();
		newVisualization.creator = new ObjectId(request().username());
		newVisualization.name = name;
		newVisualization.description = description;
		newVisualization.url = url;
		newVisualization.tags = new BasicDBList();
		for (String tag : KeywordSearch.split(tags)) {
			newVisualization.tags.add(tag);
		}
		String errorMessage;
		try {
			errorMessage = Visualization.add(newVisualization);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		}
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok(routes.Market.show().url());
	}

	public static Result showApp(String appIdString) {
		// TODO
		return show();
	}

	public static Result showVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Visualization visualization;
		try {
			visualization = Visualization.find(visualizationId);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		return ok(views.html.elements.visualization.render(visualization, userId));
	}

	public static Result installVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String errorMessage = Installed.installVisualization(visualizationId, userId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	public static Result uninstallVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String errorMessage = Installed.uninstallVisualization(visualizationId, userId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	public static Result loadVisualizations() {
		ObjectId userId = new ObjectId(request().username());
		try {
			List<Visualization> visualizations = Visualization.findInstalledBy(userId);

			// format visualizations
			List<ObjectNode> json = new ArrayList<ObjectNode>(visualizations.size());
			for (Visualization visualization : visualizations) {
				ObjectNode jsonObject = Json.newObject();
				jsonObject.put("_id", visualization._id.toString());
				jsonObject.put("creator", visualization.creator.toString());
				jsonObject.put("name", visualization.name);
				jsonObject.put("description", visualization.description);
				jsonObject.put("url", visualization.url);
				json.add(jsonObject);
			}
			return ok(Json.toJson(json));
		} catch (IllegalArgumentException e) {
			return badRequest(e.getMessage());
		} catch (IllegalAccessException e) {
			return badRequest(e.getMessage());
		} catch (InstantiationException e) {
			return badRequest(e.getMessage());
		}
	}
}
