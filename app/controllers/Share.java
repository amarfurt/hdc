package controllers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.Record;

import org.bson.types.ObjectId;

import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.records;

@Security.Authenticated(Secured.class)
public class Share extends Controller {

	public static Result sharedRecords(List<String> circleIds) {
		Set<ObjectId> circleIdSet = new HashSet<ObjectId>();
		for (String circleId : circleIds) {
			circleIdSet.add(new ObjectId(circleId));
		}
		Set<ObjectId> recordsToCheck = findSharedRecords(circleIdSet);
		try {
			String user = request().username();
			return ok(records.render(Record.findOwnedBy(user), recordsToCheck));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
	}

	/**
	 * Returns a set of records that are shared with all given circles.
	 */
	private static Set<ObjectId> findSharedRecords(Set<ObjectId> circleIds) {
		Iterator<ObjectId> iterator = circleIds.iterator();
		Set<ObjectId> sharedRecords = Collections.emptySet();
		if (iterator.hasNext()) {
			sharedRecords = new HashSet<ObjectId>(Circle.getShared(iterator.next(), request().username()));
			while (iterator.hasNext()) {
				sharedRecords.retainAll(Circle.getShared(iterator.next(), request().username()));
			}
		}
		return sharedRecords;
	}

	public static Result share() {
		// get ids of circles and records
		Map<String, String> data = Form.form().bindFromRequest().data();
		Set<ObjectId> circleIds = new HashSet<ObjectId>();
		Set<ObjectId> recordsToShare = new HashSet<ObjectId>();
		for (String id : data.keySet()) {
			if (data.get(id).equals("circle")) {
				circleIds.add(new ObjectId(id));
			} else {
				recordsToShare.add(new ObjectId(id));
			}
		}

		// get records previously shared with all circles
		Set<ObjectId> recordsToPull = findSharedRecords(circleIds);

		// get the intersection of the checked records and the previously shared records
		HashSet<ObjectId> intersection = new HashSet<ObjectId>(recordsToPull);
		intersection.retainAll(recordsToShare);

		// remove the intersection from both sets (these records remain shared with all circles)
		recordsToPull.removeAll(intersection);
		recordsToShare.removeAll(intersection);

		// pull records that are no longer shared with all circles
		for (ObjectId circleId : circleIds) {
			String errorMessage = Circle.pullRecords(circleId, recordsToPull);
			if (errorMessage != null) {
				// TODO roll back changes until here?
				return badRequest(errorMessage);
			}
		}

		// share records that are now shared with all circles
		for (ObjectId circleId : circleIds) {
			String errorMessage = Circle.shareRecords(circleId, recordsToShare);
			if (errorMessage != null) {
				// TODO roll back changes until here?
				return badRequest(errorMessage);
			}
		}
		flash("success", "Sharing settings updated.");
		return redirect(routes.Application.share());
	}

}
