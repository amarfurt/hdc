package controllers;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.Message;
import models.User;

import org.bson.types.ObjectId;

import play.Routes;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ModelConversion.ConversionException;
import utils.search.SearchException;
import views.html.index;
import views.html.welcome;
import controllers.forms.Login;
import controllers.forms.Registration;

public class Application extends Controller {

	@Security.Authenticated(Secured.class)
	public static Result index() {
		try {
			ObjectId user = new ObjectId(request().username());
			List<Message> messages = new ArrayList<Message>(Message.findSentTo(user));
			Collections.sort(messages);
			return ok(index.render(messages, user));
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result welcome() {
		return ok(welcome.render(Form.form(Login.class), Form.form(Registration.class)));
	}

	@Security.Authenticated(Secured.class)
	public static Result circles() {
		return Circles.show(null);
	}

	@Security.Authenticated(Secured.class)
	public static Result spaces() {
		return Spaces.show();
	}

	public static Result authenticate() {
		Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
		if (loginForm.hasErrors()) {
			return badRequest(welcome.render(loginForm, Form.form(Registration.class)));
		} else {
			session().clear();
			session("id", User.getId(loginForm.get().email).toString());
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
				session("id", newUser._id.toString());
				return redirect(routes.Application.index());
			} catch (ConversionException e) {
				return internalServerError(e.getMessage());
			} catch (InvalidKeySpecException e) {
				return internalServerError(e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				return internalServerError(e.getMessage());
			} catch (SearchException e) {
				return internalServerError(e.getMessage());
			}
		}
	}

	public static Result logout() {
		session().clear();
		flash("success", "You've been logged out.");
		return redirect(routes.Application.welcome());
	}

	@Security.Authenticated(Secured.class)
	public static ObjectId getCurrentUserId() {
		return new ObjectId(request().username());
	}

	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutes", controllers.routes.javascript.Circles.rename(),
				controllers.routes.javascript.Circles.delete(), controllers.routes.javascript.Circles.removeMember(),
				controllers.routes.javascript.Circles.searchUsers(), controllers.routes.javascript.Spaces.rename(),
				controllers.routes.javascript.Spaces.delete(), controllers.routes.javascript.Spaces.searchRecords(),
				controllers.routes.javascript.Spaces.loadAllRecords(),
				controllers.routes.javascript.Spaces.loadRecords(),
				controllers.routes.javascript.Spaces.getVisualizationURL(),
				controllers.routes.javascript.Market.installApp(),
				controllers.routes.javascript.Market.uninstallApp(),
				controllers.routes.javascript.Market.installVisualization(),
				controllers.routes.javascript.Market.uninstallVisualization(),
				controllers.routes.javascript.Market.loadVisualizations(),
				controllers.routes.javascript.Users.getName(),
				controllers.visualizations.routes.javascript.RecordList.load(),
				controllers.visualizations.routes.javascript.RecordList.findSpacesWith(),
				controllers.visualizations.routes.javascript.RecordList.findCirclesWith(),
				controllers.visualizations.routes.javascript.RecordList.updateSpaces(),
				controllers.visualizations.routes.javascript.RecordList.updateSharing(),
				controllers.routes.javascript.GlobalSearch.complete(),
				controllers.routes.javascript.GlobalSearch.show()));
	}

}
