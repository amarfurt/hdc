package controllers;

import java.net.UnknownHostException;

import models.User;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.index;
import views.html.welcome;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Access;

public class Application extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result index() {
		try {
			DBCursor cursor = Access.getUsers();
			String names = "";
			while (cursor.hasNext()) {
				DBObject object = cursor.next();
				names += object.get("name") + ", ";
			}
			cursor.close();
			return ok(index.render(names.substring(0, names.length() - 2), User.find(request().username())));
		} catch (UnknownHostException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result welcome() {
		return ok(welcome.render(Form.form(Login.class)));
	}

	public static Result authenticate() {
		Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
		if (loginForm.hasErrors()) {
			return badRequest(welcome.render(loginForm));
		} else {
			session().clear();
			session("email", loginForm.get().email);
			return redirect(routes.Application.index());
		}
	}

	public static Result logout() {
		session().clear();
		flash("success", "You've been logged out");
		return redirect(routes.Application.welcome());
	}

}
