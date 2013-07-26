package controllers;

import models.Circle;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.circle;

import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result add() {
		Circle newCircle = new Circle();
		newCircle.name = Form.form().bindFromRequest().get("name");
		newCircle.owner = request().username();
		newCircle.members = new BasicDBList();
		newCircle.members.add(newCircle.owner);
		try {
			Circle.add(newCircle);
			return ok(circle.render(newCircle));

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
		if (Secured.isOwnerOf(id)) {
			String newName = Form.form().bindFromRequest().get("name");
			if (Circle.rename(id, newName) == 1) {
				return ok(newName);
			} else {
				return internalServerError("Couldn't rename the circle.");
			}
		} else {
			return forbidden();
		}
	}
	
	public static Result delete(String circleId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(circleId);
		if (Secured.isOwnerOf(id)) {
			if (Circle.delete(id) == 1) {
				return ok();
			} else {
				return internalServerError("Couldn't delete the circle.");
			}
		} else {
			return forbidden();
		}
	}

}
