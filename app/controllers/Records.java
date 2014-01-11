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

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.findVisible(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("records")) {
			return badRequest("Request parameter 'records' not found.");
		}
		// TODO add fields selector
		// else if (!json.has("fields")) {
		// return badRequest("Request parameter 'fields' not found.");
		// }

		// get records
		List<ObjectId> recordIds = new ArrayList<ObjectId>();
		Iterator<JsonNode> iterator = json.get("records").iterator();
		while (iterator.hasNext()) {
			recordIds.add(new ObjectId(iterator.next().asText()));
		}
		ObjectId[] recordIdArray = new ObjectId[recordIds.size()];
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.findAll(recordIds.toArray(recordIdArray)));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	public static Result getDetailsUrl(String recordIdString) {
		Record record;
		try {
			record = Record.find(new ObjectId(recordIdString));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// put together url to send to iframe (which then loads the record representation)
		String externalServer = Play.application().configuration().getString("external.server");
		String encodedData = new String(Base64.encodeBase64(record.data.getBytes()));
		String detailsUrl = App.getDetails(record.app).replace(":record", encodedData);
		return ok("http://" + externalServer + "/" + record.app.toString() + "/" + detailsUrl);
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
				+ routes.AppsAPI.createRecord(appIdString, request().username()).url();
		String encodedReplyTo = new String(new Base64().encode(replyTo.getBytes()));

		// put together url to load in iframe
		String externalServer = Play.application().configuration().getString("external.server");
		String createUrl = app.create.replace(":replyTo", encodedReplyTo);
		String url = "http://" + externalServer + "/" + appIdString + "/" + createUrl;
		return ok(createrecords.render(url, new ObjectId(request().username())));
	}

	public static Result search(String query) {
		// TODO use caching/incremental retrieval of results (scrolls)
		ObjectId userId = new ObjectId(request().username());
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userId);
		List<SearchResult> searchResults = Search.searchRecords(userId, visibleRecords, query);
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (SearchResult searchResult : searchResults) {
			recordIds.add(new ObjectId(searchResult.id));
		}

		// TODO get only required fields, not whole record objects
		List<Record> records = new ArrayList<Record>(recordIds.size());
		ObjectId[] recordIdArray = new ObjectId[recordIds.size()];
		try {
			records.addAll(Record.findAll(recordIds.toArray(recordIdArray)));
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
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("spaces")) {
			return badRequest("Request parameter 'spaces' not found.");
		}

		// update spaces
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		Set<ObjectId> spaceIds = new HashSet<ObjectId>();
		for (JsonNode spaceId : json.get("spaces")) {
			spaceIds.add(new ObjectId(spaceId.asText()));
		}
		try {
			Space.updateRecords(spaceIds, recordId, userId);
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
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("started")) {
			return badRequest("Request parameter 'started' not found.");
		} else if (!json.has("stopped")) {
			return badRequest("Request parameter 'stopped' not found.");
		}

		// validate request: record
		ObjectId userId = new ObjectId(request().username());
		ObjectId recordId = new ObjectId(recordIdString);
		if (!Record.exists(userId, recordId)) {
			return badRequest("No record with this id exists.");
		}

		// extract circle ids from posted data
		Set<ObjectId> circleIdsStarted = new HashSet<ObjectId>();
		for (JsonNode started : json.get("started")) {
			circleIdsStarted.add(new ObjectId(started.asText()));
		}
		Set<ObjectId> circleIdsStopped = new HashSet<ObjectId>();
		for (JsonNode stopped : json.get("stopped")) {
			circleIdsStopped.add(new ObjectId(stopped.asText()));
		}

		// validate circles
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
