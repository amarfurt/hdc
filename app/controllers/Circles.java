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
		return ok(circle.render(newCircle));
	}
	
	public static Result rename(ObjectId circleId) {
		if (Secured.isOwnerOf(circleId)) {
			String newName = Form.form().bindFromRequest().get("name");
			if (Circle.rename(circleId, newName) == 1) {
				return ok(newName);
			} else {
				return internalServerError("Couldn't rename the circle.");
			}
		} else {
			return forbidden();
		}
	}

}
