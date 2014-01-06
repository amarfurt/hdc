package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.App;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

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

}
