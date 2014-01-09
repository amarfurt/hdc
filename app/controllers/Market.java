package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.App;
import models.ModelException;
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

}
