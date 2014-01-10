package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Record;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.spaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBList;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Space> spaces;
		try {
			spaces = new ArrayList<Space>(Space.findOwnedBy(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(spaces);
		return ok(Json.toJson(spaces));
	}

	public static Result index() {
		return ok(spaces.render(new ObjectId(request().username())));
	}

	public static Result details(String spaceIdString) {
		return index();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result add() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("name")) {
			return badRequest("Request parameter 'name' not found.");
		} else if (!json.has("visualization")) {
			return badRequest("Request parameter 'visualization' not found.");
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		String visualizationIdString = json.get("visualization").asText();
		if (Space.exists(userId, name)) {
			return badRequest("A space with this name already exists.");
		}

		// construct new space
		Space newSpace = new Space();
		newSpace.name = name;
		newSpace.owner = new ObjectId(request().username());
		newSpace.visualization = new ObjectId(visualizationIdString);
		newSpace.records = new BasicDBList();
		try {
			Space.add(newSpace);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(Json.toJson(newSpace));
	}

	public static Result delete(String spaceIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		if (!Space.exists(userId, spaceId)) {
			return badRequest("No space with this id exists.");
		}

		// delete space
		try {
			Space.delete(userId, spaceId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result addRecords(String spaceIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("records")) {
			return badRequest("Request parameter 'records' not found.");
		}

		// validate request
		ObjectId recordId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		if (!Space.exists(recordId, spaceId)) {
			return badRequest("No space with this id exists.");
		}

		// add records to space (implicit: if not already present)
		Set<ObjectId> recordIds = new HashSet<ObjectId>();
		for (JsonNode record : json.get("records")) {
			recordIds.add(new ObjectId(record.asText()));
		}
		try {
			Space.addRecords(spaceId, recordIds);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Return a list of records which match the given query.
	 */
	public static Result searchRecords(String query) {
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

}
