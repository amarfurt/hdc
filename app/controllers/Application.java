package controllers;

import models.Circle;
import models.Message;
import models.User;
import play.Routes;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.circles;
import views.html.index;
import views.html.welcome;

public class Application extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result index() {
		try {
			User user = User.find(request().username());
			return ok(index.render(Message.findSentTo(user), user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result welcome() {
		return ok(welcome.render(Form.form(Login.class)));
	}
	
	@Security.Authenticated(Secured.class)
	public static Result circles() {
		try {
			User user = User.find(request().username());
			return ok(circles.render(Circle.findOwnedBy(user), user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
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

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutes", 
				controllers.routes.javascript.Circles.add(),
				controllers.routes.javascript.Circles.rename(), 
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.addMember(),
				controllers.routes.javascript.Circles.removeMember()));
	}

}
