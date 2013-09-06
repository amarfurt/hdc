package controllers;

import models.Circle;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.circle;
import views.html.elements.circles.member;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result add() {
		Circle newCircle = new Circle();
		newCircle.name = Form.form().bindFromRequest().get("name");
		newCircle.owner = request().username();
		newCircle.members = new BasicDBList();
		newCircle.members.add(newCircle.owner);
		newCircle.shared = new BasicDBList();
		try {
			String errorMessage = Circle.add(newCircle);
			if (errorMessage == null) {
				return ok(circle.render(newCircle));
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
				return ok();
			} else {
				return internalServerError(errorMessage);
			}
		} else {
			return forbidden();
		}
	}

	public static Result addMember(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOfCircle(id)) {
			String newMember = Form.form().bindFromRequest().get("name");
			try {
				String errorMessage = Circle.addMember(id, newMember);
				if (errorMessage == null) {
					return ok(member.render(circleId, newMember));
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

}
