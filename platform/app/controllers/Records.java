package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.records;
import views.html.details.record;
import views.html.dialogs.createrecords;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Records extends Controller {

	public static Result index() {
		return ok(records.render(new ObjectId(request().username())));
	}

	public static Result details(String recordIdString) {
		return ok(record.render(new ObjectId(request().username())));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// get records
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractSet(json.get("fields"));
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	public static Result getDetailsUrl(String recordIdString) {
		// get record
		ObjectId recordId = new ObjectId(recordIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", recordId).get();
		Set<String> fields = new ChainedSet<String>().add("data").add("app").get();
		Record record;
		try {
			record = Record.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// get app
		properties = new ChainedMap<String, ObjectId>().put("_id", record.app).get();
		fields = new ChainedSet<String>().add("details").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// put together url to send to iframe (which then loads the record representation)
		String appServer = Play.application().configuration().getString("plugins.server");
		String encodedData = new String(Base64.encodeBase64(record.data.getBytes()));
		String detailsUrl = app.details.replace(":record", encodedData);
		return ok("http://" + appServer + "/apps/" + record.app.toString() + "/" + detailsUrl);
	}

	public static Result create(String appIdString) {
		// get app
		ObjectId appId = new ObjectId(appIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", appId).get();
		Set<String> fields = new ChainedSet<String>().add("create").get();
		App app;
		try {
			app = App.get(properties, fields);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create reply to address and encode it with Base64
		String platformServer = Play.application().configuration().getString("platform.server");
		String replyTo = "http://" + platformServer
				+ routes.AppsAPI.createRecord(request().username(), appIdString).url();
		String encodedReplyTo = new String(new Base64().encode(replyTo.getBytes()));

		// put together url to load in iframe
		String appServer = Play.application().configuration().getString("plugins.server");
		String createUrl = app.create.replace(":replyTo", encodedReplyTo);
		String url = "http://" + appServer + "/apps/" + appIdString + "/" + createUrl;
		return ok(createrecords.render(url, new ObjectId(request().username())));
	}

	public static Result search(String query) {
		// get the visible records
		ObjectId userId = new ObjectId(request().username());
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", userId).get();
		Set<String> fields = new ChainedSet<String>().add("visible").get();
		User user;
		try {
			user = User.get(properties, fields);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// TODO use caching/incremental retrieval of results (scrolls)
		List<SearchResult> searchResults = Search.searchRecords(userId, user.visible, query);
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			recordIds.add(new ObjectId(searchResult.id));
		}

		// get records
		Map<String, Set<ObjectId>> recordProperties = new ChainedMap<String, Set<ObjectId>>().put("_id", recordIds)
				.get();
		fields = new ChainedSet<String>().add("app").add("owner").add("creator").add("created").add("name").add("data")
				.get();
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(recordProperties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	/**
	 * Updates the spaces the given record is in.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateSpaces(String recordIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "spaces");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// update spaces
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		Set<ObjectId> spaceIds = ObjectIdConversion.toObjectIds(JsonExtraction.extractSet(json.get("spaces")));
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("records").get();
		try {
			Set<Space> spaces = Space.getAll(properties, fields);
			for (Space space : spaces) {
				if (spaceIds.contains(space._id) && !space.records.contains(recordId)) {
					space.records.add(recordId);
					Space.set(space._id, "records", space.records);
				} else {
					space.records.remove(recordId);
					Space.set(space._id, "records", space.records);
				}
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Updates the circles the given record is shared with.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateSharing(String recordIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "started", "stopped");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request: record
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", recordId).put("owner", userId)
				.get();
		if (!Record.exists(properties)) {
			return badRequest("No record with this id exists.");
		}

		// extract circle ids from posted data
		Set<ObjectId> startedCircleIds = ObjectIdConversion.toObjectIds(JsonExtraction.extractSet(json.get("started")));
		Set<ObjectId> stoppedCircleIds = ObjectIdConversion.toObjectIds(JsonExtraction.extractSet(json.get("stopped")));

		// validate circles
		Iterator<ObjectId> iterator = startedCircleIds.iterator();
		while (iterator.hasNext()) {
			if (!Circle.exists(new ChainedMap<String, ObjectId>().put("_id", iterator.next()).put("owner", userId)
					.get())) {
				iterator.remove();
			}
		}
		iterator = stoppedCircleIds.iterator();
		while (iterator.hasNext()) {
			if (!Circle.exists(new ChainedMap<String, ObjectId>().put("_id", iterator.next()).put("owner", userId)
					.get())) {
				iterator.remove();
			}
		}

		// update circles (fetch all and update necessary circles)
		properties = new ChainedMap<String, ObjectId>().put("owner", userId).get();
		Set<String> fields = new ChainedSet<String>().add("shared").add("members").get();
		Set<ObjectId> startedUserIds = new HashSet<ObjectId>();
		Set<ObjectId> stoppedUserIds = new HashSet<ObjectId>();
		try {
			Set<Circle> circles = Circle.getAll(properties, fields);
			for (Circle circle : circles) {
				if (startedCircleIds.contains(circle._id)) {
					circle.shared.add(recordId);
					Circle.set(circle._id, "shared", circle.shared);
					for (ObjectId memberId : circle.members) {
						if (!isSharedWith(circles, recordId, memberId)) {
							startedUserIds.add(memberId);
						}
					}
				} else if (stoppedCircleIds.contains(circle._id)) {
					circle.shared.remove(recordId);
					Circle.set(circle._id, "shared", circle.shared);
					for (ObjectId memberId : circle.members) {
						if (!isSharedWith(circles, recordId, memberId)) {
							stoppedUserIds.add(memberId);
						}
					}
				}
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// also update visible records of users
		fields = new ChainedSet<String>().add("visible." + userId.toString()).get();
		try {
			// users that can now see the record
			Set<User> startedUsers = User.getAll(new ChainedMap<String, Set<ObjectId>>().put("_id", startedUserIds)
					.get(), fields);
			for (User startedUser : startedUsers) {
				if (!startedUser.visible.containsKey(userId.toString())) {
					User.set(startedUser._id, "visible." + userId.toString(), new ChainedSet<ObjectId>().add(recordId)
							.get());
				} else {
					Set<ObjectId> visibleRecords = startedUser.visible.get(userId.toString());
					visibleRecords.add(recordId);
					User.set(startedUser._id, "visible." + userId.toString(), visibleRecords);
				}
			}

			// users that can no longer see the record
			Set<User> stoppedUsers = User.getAll(new ChainedMap<String, Set<ObjectId>>().put("_id", stoppedUserIds)
					.get(), fields);
			for (User stoppedUser : stoppedUsers) {
				Set<ObjectId> visibleRecords = stoppedUser.visible.get(userId.toString());
				visibleRecords.remove(recordId);
				User.set(stoppedUser._id, "visible." + userId.toString(), visibleRecords);
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Check whether the given record is shared with the given user in one of the circles.
	 */
	private static boolean isSharedWith(Set<Circle> circles, ObjectId recordId, ObjectId userId) {
		for (Circle circle : circles) {
			if (circle.members.contains(userId) && circle.shared.contains(recordId)) {
				return true;
			}
		}
		return false;
	}
}
