package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.App;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ModelConversion.ConversionException;
import utils.search.SearchException;
import views.html.market;
import views.html.dialogs.registerapp;
import views.html.dialogs.registervisualization;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated(Secured.class)
public class Market extends Controller {

	public static Result index() {
		// TODO display correct lists
		ObjectId userId = new ObjectId(request().username());
		List<App> spotlightedApps;
		List<Visualization> spotlightedVisualizations;
		try {
			spotlightedApps = new ArrayList<App>(App.findSpotlighted());
			spotlightedVisualizations = new ArrayList<Visualization>(Visualization.findSpotlighted());
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(spotlightedApps);
		Collections.sort(spotlightedVisualizations);
		return ok(market.render(spotlightedApps, spotlightedVisualizations, spotlightedVisualizations, userId));
	}

	// Apps
	public static Result registerAppForm() {
		return ok(registerapp.render(Form.form(App.class), new ObjectId(request().username())));
	}

	public static Result registerApp() {
		ObjectId userId = new ObjectId(request().username());
		Form<App> appForm = Form.form(App.class).bindFromRequest();
		if (appForm.hasGlobalErrors()) {
			return badRequest(registerapp.render(appForm, userId));
		}

		// create new app
		App newApp = appForm.get();
		newApp.creator = userId;
		String errorMessage;
		try {
			errorMessage = App.add(newApp);
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		} catch (SearchException e) {
			return internalServerError(e.getMessage());
		}
		if (errorMessage != null) {
			appForm.reject(errorMessage);
			return badRequest(registerapp.render(appForm, userId));
		}
		return redirect(routes.Market.index());
	}

	public static Result showApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		App app;
		try {
			app = App.find(appId);
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
		return ok(views.html.details.app.render(app, userId));
	}

	public static Result installApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		String errorMessage = User.addApp(userId, appId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	public static Result uninstallApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		String errorMessage = User.removeApp(userId, appId);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

	// Visualizations
	public static Result registerVisualizationForm() {
		return ok(registervisualization.render(Form.form(Visualization.class), new ObjectId(request().username())));
	}

	public static Result registerVisualization() {
		ObjectId userId = new ObjectId(request().username());
		Form<Visualization> visualizationForm = Form.form(Visualization.class).bindFromRequest();
		if (visualizationForm.hasErrors()) {
			return badRequest(registervisualization.render(visualizationForm, userId));
		}

		// create new visualization
		Visualization newVisualization = visualizationForm.get();
		newVisualization.creator = userId;
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
		return index();
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

	@Deprecated
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
