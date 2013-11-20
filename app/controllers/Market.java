package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ModelConversion.ConversionException;
import utils.search.SearchException;
import views.html.market;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated(Secured.class)
public class Market extends Controller {

	public static Result show() {
		ObjectId userId = new ObjectId(request().username());
		List<Visualization> spotlightedVisualizations;
		try {
			spotlightedVisualizations = new ArrayList<Visualization>(Visualization.findSpotlighted());
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(spotlightedVisualizations);
		return ok(market.render(spotlightedVisualizations, spotlightedVisualizations, userId));
	}

	public static Result registerVisualization(String name, String description, String url) {
		if (Visualization.visualizationWithSameNameExists(name)) {
			return badRequest("A visualization with this name already exists.");
		} else if (name.isEmpty() || description.isEmpty() || url.isEmpty()) {
			return badRequest("Please fill out all fields.");
		}
		Visualization newVisualization = new Visualization();
		newVisualization.creator = new ObjectId(request().username());
		newVisualization.name = name;
		newVisualization.description = description;
		newVisualization.url = url;
		String errorMessage;
		try {
			errorMessage = Visualization.add(newVisualization);
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		} catch (SearchException e) {
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
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
		return ok(views.html.details.visualization.render(visualization, userId));
	}

	public static Result installVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String errorMessage = User.addVisualization(userId, visualizationId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	public static Result uninstallVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String errorMessage = User.removeVisualization(userId, visualizationId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	public static Result loadVisualizations() {
		ObjectId userId = new ObjectId(request().username());
		List<Visualization> visualizations;
		try {
			visualizations = new ArrayList<Visualization>(User.findVisualizations(userId));
		} catch (ConversionException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(visualizations);

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
	}
}
