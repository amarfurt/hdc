package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.App;
import models.ModelException;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.market;
import views.html.dialogs.registerapp;
import views.html.dialogs.registervisualization;

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
		} catch (ModelException e) {
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
		try {
			App.add(newApp);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return redirect(routes.Market.index());
	}

	public static Result showApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		App app;
		try {
			app = App.find(appId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(views.html.details.app.render(app, userId));
	}

	public static Result installApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		try {
			User.addApp(userId, appId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstallApp(String appIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId appId = new ObjectId(appIdString);
		try {
			User.removeApp(userId, appId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
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
		try {
			Visualization.add(newVisualization);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return index();
	}

	public static Result showVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		Visualization visualization;
		try {
			visualization = Visualization.find(visualizationId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(views.html.details.visualization.render(visualization, userId));
	}

	public static Result installVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		try {
			User.addVisualization(userId, visualizationId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result uninstallVisualization(String visualizationIdString) {
		ObjectId userId = new ObjectId(request().username());
		ObjectId visualizationId = new ObjectId(visualizationIdString);
		try {
			User.removeVisualization(userId, visualizationId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
