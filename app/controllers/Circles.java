package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ModelConversion.ConversionException;
import utils.search.SearchException;
import utils.search.SearchResult;
import utils.search.Search;
import utils.search.Search.Type;
import views.html.circles;
import views.html.elements.usersearchresults;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result index() {
		return show(null);
	}

	public static Result show(String activeCircleId) {
		try {
			ObjectId user = new ObjectId(request().username());
			List<Circle> circleList = new ArrayList<Circle>(Circle.findOwnedBy(user));
			Collections.sort(circleList);
			ObjectId activeCircle = null;
			if (activeCircleId != null) {
				activeCircle = new ObjectId(activeCircleId);
			} else if (circleList.size() > 0) {
				activeCircle = circleList.get(0)._id;
			}
			List<User> contacts = new ArrayList<User>(Circle.findContacts(user));
			Collections.sort(contacts);
			List<User> users = new ArrayList<User>();
			return ok(circles.render(contacts, users, circleList, activeCircle, user));
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result add() {
		Circle newCircle = new Circle();
		newCircle.name = Form.form().bindFromRequest().get("name");
		newCircle.owner = new ObjectId(request().username());
		newCircle.members = new BasicDBList();
		newCircle.shared = new BasicDBList();
		try {
			String errorMessage = Circle.add(newCircle);
			if (errorMessage == null) {
				return redirect(routes.Circles.show(newCircle._id.toString()));
			} else {
				return badRequest(errorMessage);
			}
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		} catch (SearchException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result rename(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			String newName = Form.form().bindFromRequest().get("name");
			try {
				String errorMessage = Circle.rename(id, newName);
				if (errorMessage == null) {
					return ok(newName);
				} else {
					return badRequest(errorMessage);
				}
			} catch (SearchException e) {
				return internalServerError(e.getMessage());
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
				return ok(routes.Circles.index().url());
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
				for (String user : data.keySet()) {
					// skip search input field
					if (user.equals("userSearch")) {
						continue;
					}
					String errorMessage = Circle.addMember(id, new ObjectId(user));
					if (errorMessage != null) {
						// TODO remove previously added users?
						return badRequest(usersAdded + errorMessage);
					}
					if (usersAdded.isEmpty()) {
						usersAdded = "Added some users, but then an error occurred: ";
					}
				}
				return redirect(routes.Circles.show(circleId));
			} catch (ConversionException e) {
				return internalServerError(e.getMessage());
			}
		} else {
			return forbidden();
		}
	}

	public static Result removeMember(String circleIdString, String memberIdString) {
		// can't pass parameter of type ObjectId, using String
		ObjectId circleId = new ObjectId(circleIdString);
		if (Secured.isOwnerOfCircle(circleId)) {
			try {
				String errorMessage = Circle.removeMember(circleId, new ObjectId(memberIdString));
				if (errorMessage == null) {
					return ok();
				} else {
					return badRequest(errorMessage);
				}
			} catch (ConversionException e) {
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
	public static Result searchUsers(String circleIdString, String query) {
		List<User> users = new ArrayList<User>();
		int limit = 10;
		ObjectId circleId = new ObjectId(circleIdString);
		Set<ObjectId> members = Circle.getMembers(circleId);
		members.add(new ObjectId(request().username()));
		while (users.size() < limit) {
			// TODO use caching/incremental retrieval of results (scrolls)
			List<SearchResult> searchResults = Search.searchPublic(Type.USER, query);
			Set<ObjectId> userIds = new HashSet<ObjectId>();
			for (SearchResult searchResult : searchResults) {
				userIds.add(new ObjectId(searchResult.id));
			}
			userIds.removeAll(members);
			try {
				users.addAll(User.find(userIds));
			} catch (ConversionException e) {
				return internalServerError(e.getMessage());
			}

			// TODO break if scrolling finds no more results
			break;
		}
		Collections.sort(users);
		return ok(usersearchresults.render(users));
	}

}
