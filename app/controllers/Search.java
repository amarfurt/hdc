package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@Security.Authenticated(Secured.class)
public class Search extends Controller {

	public static Result search() {
		// TODO
		return null;
	}

}
