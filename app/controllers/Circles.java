package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Circle;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.KeywordSearch;
import views.html.circles;
import views.html.elements.circles.userForm;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {
	
	public static Result show(String activeCircleId) {
		try {
			String user = request().username();
			List<Circle> circleList = Circle.findOwnedBy(user);
			ObjectId activeCircle = null;
			if (activeCircleId != null) {
				activeCircle = new ObjectId(activeCircleId);
			} else if (circleList.size() > 0) {
				activeCircle = circleList.get(0)._id;
			}
			return ok(circles.render(Circle.findContacts(user), User.findAllExcept(user), circleList, activeCircle, user));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result add() {
		Circle newCircle = new Circle();
		newCircle.name = Form.form().bindFromRequest().get("name");
		newCircle.owner = request().username();
		newCircle.members = new BasicDBList();
		newCircle.shared = new BasicDBList();
		try {
			String errorMessage = Circle.add(newCircle);
			if (errorMessage == null) {
				return redirect(routes.Circles.show(newCircle._id.toString()));
			} else {
				return badRequest(errorMessage);
			}

			// multi-catch doesn't seem to work...
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result rename(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			String newName = Form.form().bindFromRequest().get("name");
			String errorMessage = Circle.rename(id, newName);
			if (errorMessage == null) {
				return ok(newName);
			} else {
				return badRequest(errorMessage);
			}
		} else {
			return forbidden();
		}
	}

	public static Result delete(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			String errorMessage = Circle.delete(id);
			if (errorMessage == null) {
				return ok(routes.Application.circles().url());
			} else {
				return badRequest(errorMessage);
			}
		} else {
			return forbidden();
		}
	}

	public static Result addUsers(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			Map<String, String> data = Form.form().bindFromRequest().data();
			try {
				String usersAdded = "";
				for (String email : data.keySet()) {
					// skip search input field
					if (email.equals("userSearch")) {
						continue;
					}
					String errorMessage = Circle.addMember(id, email);
					if (errorMessage != null) {
						// TODO remove previously added users?
						return badRequest(usersAdded + errorMessage);
					}
					if (usersAdded.isEmpty()) {
						usersAdded = "Added some users, but then an error occurred: ";
					}
				}
				return redirect(routes.Circles.show(circleId));
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				return internalServerError(e.getMessage());
			}
		} else {
			return forbidden();
		}
	}

	public static Result removeMember(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			String member = Form.form().bindFromRequest().get("name");
			try {
				String errorMessage = Circle.removeMember(id, member);
				if (errorMessage == null) {
					return ok();
				} else {
					return badRequest(errorMessage);
				}
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				return internalServerError(e.getMessage());
			}
		} else {
			return forbidden();
		}
	}

	/**
	 * Return a list of users whose name or email address matches the current search term and is not in the circle
	 * already.
	 */
	public static Result searchUsers(String circleId, String search) {
		List<User> response = new ArrayList<User>();
		try {
			// TODO use caching
			ObjectId id = new ObjectId(circleId);
			if (search == null || search.isEmpty()) {
				response = User.findAllExcept(request().username());
			} else {
				response = KeywordSearch.searchByType(User.class, User.getCollection(), search, 10);
			}
			response = Circle.makeDisjoint(id, response);
			return ok(userForm.render(response));
		} catch (IllegalArgumentException e) {
			return badRequest(e.getMessage());
		} catch (IllegalAccessException e) {
			return badRequest(e.getMessage());
		} catch (InstantiationException e) {
			return badRequest(e.getMessage());
		}
	}

}
