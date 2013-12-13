package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchResult;
import views.html.circles;
import views.html.elements.usersearchresults;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result index() {
		return show(null);
	}

	public static Result show(String activeCircleId) {
		ObjectId user = new ObjectId(request().username());
		List<Circle> circleList;
		try {
			circleList = new ArrayList<Circle>(Circle.findOwnedBy(user));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(circleList);
		ObjectId activeCircle = null;
		if (activeCircleId != null) {
			activeCircle = new ObjectId(activeCircleId);
		} else if (circleList.size() > 0) {
			activeCircle = circleList.get(0)._id;
		}
		List<User> contacts;
		try {
			contacts = new ArrayList<User>(Circle.findContacts(user));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(contacts);
		List<User> users = new ArrayList<User>();
		return ok(circles.render(contacts, users, circleList, activeCircle, user));
	}

	public static Result add() {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = Form.form().bindFromRequest().get("name");
		if (Circle.exists(userId, name)) {
			return badRequest("A circle with this name already exists.");
		}

		// construct new circle
		Circle newCircle = new Circle();
		newCircle.name = name;
		newCircle.owner = userId;
		newCircle.members = new BasicDBList();
		newCircle.shared = new BasicDBList();
		try {
			Circle.add(newCircle);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return redirect(routes.Circles.show(newCircle._id.toString()));
	}

	public static Result rename(String circleIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		String newName = Form.form().bindFromRequest().get("newName");
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		} else if (Circle.exists(userId, newName)) {
			return badRequest("A circle with this name already exists.");
		}

		// rename circle
		try {
			Circle.rename(userId, circleId, newName);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result delete(String circleIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// delete circle
		try {
			Circle.delete(userId, circleId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result addUsers(String circleIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// add users to circle (implicit: if not already present)
		Map<String, String> data = Form.form().bindFromRequest().data();
		for (String user : data.keySet()) {
			// skip search input field
			if (user.equals("userSearch")) {
				continue;
			}
			try {
				Circle.addMember(userId, circleId, new ObjectId(user));
			} catch (ModelException e) {
				return badRequest(e.getMessage());
			}
		}
		return redirect(routes.Circles.show(circleIdString));
	}

	public static Result removeMember(String circleIdString, String memberIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(userId, circleId)) {
			return badRequest("No circle with this id exists.");
		}

		// remove member from circle (implicit: if present)
		try {
			Circle.removeMember(userId, circleId, new ObjectId(memberIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
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
			} catch (ModelException e) {
				return badRequest(e.getMessage());
			}

			// TODO break if scrolling finds no more results
			break;
		}
		Collections.sort(users);
		return ok(usersearchresults.render(users));
	}

}
