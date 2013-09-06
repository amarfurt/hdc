package controllers;

import java.util.Map;

import models.Record;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.spaces;

import com.mongodb.BasicDBList;

import controllers.forms.SpaceForm;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result add() {
		Form<SpaceForm> spaceForm = Form.form(SpaceForm.class).bindFromRequest();
		if (spaceForm.hasErrors()) {
			try {
				User user = User.find(request().username());
				return badRequest(spaces.render(spaceForm, Space.findOwnedBy(user), user));
			} catch (IllegalArgumentException e) {
				return internalServerError(e.getMessage());
			} catch (IllegalAccessException e) {
				return internalServerError(e.getMessage());
			} catch (InstantiationException e) {
				return internalServerError(e.getMessage());
			}
		} else {
			// TODO js ajax insertion, open newly added space
			// return ok(space.render(newSpace));
			return redirect(routes.Application.spaces());
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

	public static Result addRecords(String spaceId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId sId = new ObjectId(spaceId);
		if (Secured.isOwnerOfSpace(sId)) {
			Map<String, String> data = Form.form().bindFromRequest().data();
			try {
				String recordsAdded = "";
				for (String recordId : data.keySet()) {
					ObjectId rId = new ObjectId(recordId);
					String errorMessage = Space.addRecord(sId, rId);
					if (errorMessage != null) {
						// TODO remove previously added records?
						return badRequest(recordsAdded + errorMessage);
					}
					if (recordsAdded.isEmpty()) {
						recordsAdded = "Added some records, but then an error occurred: ";
					}
				}
				return redirect(routes.Application.spaces());
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				return internalServerError(e.getMessage());
			}
		} else {
			return forbidden();
		}
	}

	public static Result removeRecord(String spaceId) {
		// can't pass parameter of type ObjectId, using String
		ObjectId sId = new ObjectId(spaceId);
		if (Secured.isOwnerOfSpace(sId)) {
			String recordId = Form.form().bindFromRequest().get("id");
			ObjectId rId = new ObjectId(recordId);
			try {
				String errorMessage = Space.removeRecord(sId, rId);
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

	public static Result manuallyAddRecord() {
		Record newRecord = new Record();
		newRecord.creator = request().username();
		newRecord.owner = newRecord.creator;
		newRecord.data = Form.form().bindFromRequest().get("data");
		try {
			String errorMessage = Record.add(newRecord);
			if (errorMessage == null) {
				return redirect(routes.Application.spaces());
			} else {
				return badRequest(errorMessage);
			}
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		}
	}

	/**
	 * Validation helper for space form (we only have access to current user in controllers).
	 */
	public static String validateSpace(String name, String visualization) {
		Space newSpace = new Space();
		newSpace.name = name;
		newSpace.owner = request().username();
		newSpace.visualization = visualization;
		newSpace.records = new BasicDBList();
		try {
			return Space.add(newSpace);
			// multi-catch doesn't seem to work...
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		} catch (IllegalAccessException e) {
			return e.getMessage();
		}
	}

}
