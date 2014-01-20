package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Space;

import org.bson.types.ObjectId;

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
import views.html.spaces;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result index() {
		return ok(spaces.render(new ObjectId(request().username())));
	}

	public static Result details(String spaceIdString) {
		return index();
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

		// get spaces
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Space> spaces;
		try {
			spaces = new ArrayList<Space>(Space.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(spaces);
		return ok(Json.toJson(spaces));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result add() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "name", "visualization");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		String visualizationIdString = json.get("visualization").asText();
		if (Space.exists(new ChainedMap<String, Object>().put("owner", userId).put("name", name).get())) {
			return badRequest("A space with this name already exists.");
		}

		// create new space
		Space space = new Space();
		space._id = new ObjectId();
		space.name = name;
		space.owner = userId;
		space.visualization = new ObjectId(visualizationIdString);
		space.records = new HashSet<ObjectId>();
		try {
			Space.add(space);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(Json.toJson(space));
	}

	public static Result delete(String spaceIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		if (!Space.exists(new ChainedMap<String, ObjectId>().put("_id", spaceId).put("owner", userId).get())) {
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
		try {
			JsonValidation.validate(json, "records");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		Map<String, ObjectId> properties = new ChainedMap<String, ObjectId>().put("_id", spaceId).put("owner", userId)
				.get();
		if (!Space.exists(properties)) {
			return badRequest("No space with this id exists.");
		}

		// add records to space (implicit: if not already present)
		Set<ObjectId> recordIds = ObjectIdConversion.toObjectIds(JsonExtraction.extractStringSet(json.get("records")));
		Set<String> fields = new ChainedSet<String>().add("records").get();
		try {
			Space space = Space.get(properties, fields);
			space.records.addAll(recordIds);
			Space.set(space._id, "records", space.records);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}
}
