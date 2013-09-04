package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Record;
import models.User;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.records;

@Security.Authenticated(Secured.class)
public class Share extends Controller {

	public static Result sharedRecords(List<String> circleIds) {
		Iterator<String> iterator = circleIds.iterator();
		Set<ObjectId> checkedRecords;
		if (iterator.hasNext()) {
			ObjectId circleId = new ObjectId(iterator.next());
			checkedRecords = new HashSet<ObjectId>(Circle.getShared(circleId, request().username()));
			while (iterator.hasNext()) {
				circleId = new ObjectId(iterator.next());
				checkedRecords.retainAll(Circle.getShared(circleId, request().username()));
			}
		} else {
			checkedRecords = Collections.emptySet();
		}
		try {
			User user = User.find(request().username());
			return ok(records.render(Record.findOwnedBy(user), checkedRecords));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	public static Result share() {
		Map<String, String> data = Form.form().bindFromRequest().data();
		List<ObjectId> circleIds = new ArrayList<ObjectId>();
		List<ObjectId> recordIds = new ArrayList<ObjectId>();
		for (String id : data.keySet()) {
			if (data.get(id).equals("circle")) {
				circleIds.add(new ObjectId(id));
			} else {
				recordIds.add(new ObjectId(id));
			}
		}
		try {
			for (ObjectId circleId : circleIds) {
				for (ObjectId recordId : recordIds) {
					String errorMessage = Circle.shareRecord(circleId, recordId);
					// TODO "unshare" previously shared records?
					return badRequest(errorMessage);
				}
			}
			return ok();
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		}
	}

}
