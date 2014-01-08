package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.ModelException;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

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

	public static Result getUrl(String visualizationIdString) {
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		String externalServer = Play.application().configuration().getString("external.server");
		String url = "http://" + externalServer + "/" + visualizationId + "/" + Visualization.getUrl(visualizationId);
		return ok(url);
	}

}
