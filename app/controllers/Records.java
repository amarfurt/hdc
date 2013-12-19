package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.App;
import models.Circle;
import models.ModelException;
import models.Record;
import models.Space;
import models.User;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.dialogs.createrecords;

@Security.Authenticated(Secured.class)
public class Records extends Controller {

	public static Result index() {
		ObjectId userId = new ObjectId(request().username());
		List<App> apps;
		List<Record> records;
		try {
			apps = new ArrayList<App>(User.findApps(userId));
			records = new ArrayList<Record>(Record.findVisible(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(records);
		Collections.sort(apps);
		return ok(views.html.records.render(records, apps, userId));
	}

	public static Result create(String appIdString) {
		ObjectId appId = new ObjectId(appIdString);
		App app;
		try {
			app = App.find(appId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create reply to address and encode it with Base64
		String applicationServer = Play.application().configuration().getString("application.server");
		String replyTo = "http://" + applicationServer
				+ routes.Apps.createRecord(appIdString, request().username()).url();
		String encodedReplyTo = new String(new Base64().encode(replyTo.getBytes()));

		// put together url to load in iframe
		String externalServer = Play.application().configuration().getString("external.server");
		String createUrl = app.create.replace(":replyTo", encodedReplyTo);
		String url = "http://" + externalServer + "/" + appIdString + "/" + createUrl;
		return ok(createrecords.render(url, new ObjectId(request().username())));
	}

	/**
	 * Find the spaces that contain the given record.
	 */
	public static Result findSpacesWith(String recordId) {
		Set<ObjectId> spaceIds = Space.findWithRecord(new ObjectId(recordId), new ObjectId(request().username()));
		List<String> spaces = new ArrayList<String>();
		for (ObjectId id : spaceIds) {
			spaces.add(id.toString());
		}

		// TODO also fetch order/names of spaces for meaningful sorting?
		Collections.sort(spaces);
		return ok(Json.toJson(spaces));
	}

	/**
	 * Find the circles the given record is shared with.
	 */
	public static Result findCirclesWith(String recordId) {
		Set<ObjectId> circleIds = Circle.findWithRecord(new ObjectId(recordId), new ObjectId(request().username()));
		List<String> circles = new ArrayList<String>();
		for (ObjectId id : circleIds) {
			circles.add(id.toString());
		}

		// TODO also fetch order/names of spaces for meaningful sorting?
		Collections.sort(circles);
		return ok(Json.toJson(circles));
	}

	/**
	 * Updates the spaces the given record is in.
	 */
	public static Result updateSpaces(String recordId, List<String> spaces) {
		Set<ObjectId> spaceIds = new HashSet<ObjectId>();
		for (String id : spaces) {
			spaceIds.add(new ObjectId(id));
		}
		try {
			Space.updateRecords(spaceIds, new ObjectId(recordId), new ObjectId(request().username()));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Updates the circles the given record is shared with.
	 */
	public static Result updateSharing(String record, List<String> circlesStarted, List<String> circlesStopped) {
		ObjectId recordId = new ObjectId(record);
		Set<ObjectId> circleIdsStarted = new HashSet<ObjectId>();
		for (String id : circlesStarted) {
			circleIdsStarted.add(new ObjectId(id));
		}
		Set<ObjectId> circleIdsStopped = new HashSet<ObjectId>();
		for (String id : circlesStopped) {
			circleIdsStopped.add(new ObjectId(id));
		}

		// validate circles
		ObjectId userId = new ObjectId(request().username());
		Iterator<ObjectId> iterator = circleIdsStarted.iterator();
		while (iterator.hasNext()) {
			if (!Circle.exists(userId, iterator.next())) {
				iterator.remove();
			}
		}
		iterator = circleIdsStopped.iterator();
		while (iterator.hasNext()) {
			if (!Circle.exists(userId, iterator.next())) {
				iterator.remove();
			}
		}

		// update circles
		try {
			Circle.startSharingWith(userId, recordId, circleIdsStarted);
			Circle.stopSharingWith(userId, recordId, circleIdsStopped);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
