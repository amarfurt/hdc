package controllers.api;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;

public class UserInfo extends Controller {

	public static Result getName(String id) {
		return ok(User.getName(id));
	}

}
