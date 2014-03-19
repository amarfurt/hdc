package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.developers;

// not secured, accessible by anyone
public class Developers extends Controller {

	public static Result index() {
		return ok(developers.render());
	}

}
