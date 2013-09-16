package controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import models.Circle;
import models.Message;
import models.Record;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import play.Routes;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.circles;
import views.html.index;
import views.html.share;
import views.html.spaces;
import views.html.welcome;
import controllers.forms.Login;
import controllers.forms.Registration;
import controllers.forms.SpaceForm;

public class Application extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result index() {
		try {
			String user = request().username();
			return ok(index.render(Message.findSentTo(user), user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result welcome() {
		return ok(welcome.render(Form.form(Login.class), Form.form(Registration.class)));
	}
	
	@Security.Authenticated(Secured.class)
	public static Result circles() {
		try {
			String user = request().username();
			List<Circle> circleList = Circle.findOwnedBy(user);
			ObjectId activeCircle = null;
			if (circleList.size() > 0) {
				activeCircle = circleList.get(0)._id;
			}
			return ok(circles.render(User.findAllExcept(user), circleList, activeCircle, user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}
	
	@Security.Authenticated(Secured.class)
	public static Result spaces() {
		try {
			String user = request().username();
			return ok(spaces.render(Form.form(SpaceForm.class), Record.findSharedWith(user), Space.findOwnedBy(user), user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}
	
	@Security.Authenticated(Secured.class)
	public static Result share() {
		try {
			String user = request().username();
			Set<ObjectId> emptySet = Collections.emptySet();
			return ok(share.render(Record.findOwnedBy(user), emptySet, Circle.findOwnedBy(user), user));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result authenticate() {
		Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
		if (loginForm.hasErrors()) {
			return badRequest(welcome.render(loginForm, Form.form(Registration.class)));
		} else {
			session().clear();
			session("email", loginForm.get().email);
			return redirect(routes.Application.index());
		}
	}

	public static Result register() {
		Form<Registration> registrationForm = Form.form(Registration.class).bindFromRequest();
		if (registrationForm.hasErrors()) {
			return badRequest(welcome.render(Form.form(Login.class), registrationForm));
		} else {
			Registration registration = registrationForm.get();
			User newUser = new User();
			newUser.email = registration.email;
			newUser.name = registration.firstName + " " + registration.lastName;
			newUser.password = registration.password;
			try {
				String errorMessage = User.add(newUser);
				if (errorMessage != null) {
					return badRequest(errorMessage);
				}
				session().clear();
				session("email", registration.email);
				return redirect(routes.Application.index());
			} catch (IllegalAccessException e) {
				return internalServerError(e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				return internalServerError(e.getMessage());
			} catch (InvalidKeySpecException e) {
				return internalServerError(e.getMessage());
			}
		}
	}

	public static Result logout() {
		session().clear();
		flash("success", "You've been logged out.");
		return redirect(routes.Application.welcome());
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutes", 
				controllers.routes.javascript.Circles.rename(), 
				controllers.routes.javascript.Circles.delete(),
				controllers.routes.javascript.Circles.removeMember(),
				controllers.routes.javascript.Circles.searchUsers(),
				controllers.routes.javascript.Spaces.rename(),
				controllers.routes.javascript.Spaces.delete(),
				controllers.routes.javascript.Spaces.removeRecord(),
				controllers.routes.javascript.Spaces.searchRecords(),
				controllers.routes.javascript.Share.sharedRecords()));
	}

}
