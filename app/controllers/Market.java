package controllers;

import java.util.List;

import models.Visualization;

import org.bson.types.ObjectId;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.market;

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
		return ok(market.render(spotlightedVisualizations, userId));
	}
	
	public static Result showApp(String appIdString) {
		// TODO
		return show();
	}
	
	public static Result showVisualization(String visualizationIdString) {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		return show();
	}
}
