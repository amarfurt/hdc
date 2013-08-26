package controllers;

import models.Space;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.space;

import com.mongodb.BasicDBList;

import controllers.forms.SpaceForm;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result add() {
		Space newSpace = new Space();
		SpaceForm spaceForm = Form.form(SpaceForm.class).bindFromRequest().get();
		newSpace.name = spaceForm.name;
		newSpace.owner = request().username();
		newSpace.visualization = spaceForm.visualization;
		newSpace.records = new BasicDBList();
		try {
			String errorMessage = Space.add(newSpace);
			if (errorMessage == null) {
				// TODO js ajax insertion
//				return ok(space.render(newSpace));
				return Application.spaces();
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

	public static Result rename(String spaceId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(spaceId);
		if (Secured.isOwnerOfSpace(id)) {
			String newName = Form.form().bindFromRequest().get("name");
			String errorMessage = Space.rename(id, newName);
			if (errorMessage == null) {
				return ok(newName);
			} else {
				return badRequest(errorMessage);
			}
		} else {
			return forbidden();
		}
	}

	public static Result delete(String spaceId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId id = new ObjectId(spaceId);
		if (Secured.isOwnerOfSpace(id)) {
			String errorMessage = Space.delete(id);
			if (errorMessage == null) {
				return ok();
			} else {
				return internalServerError(errorMessage);
			}
		} else {
			return forbidden();
		}
	}

}
