package controllers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Space;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.auth.AuthToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;

// not secured, accessible from visualizations server (only with valid authToken)
public class VisualizationsAPI extends Controller {

	public static Result checkPreflight() {
		// allow cross-origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result getIds() {
		// allow cross origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);

		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken and check whether space with corresponding owner exists
		AuthToken authToken = AuthToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, ObjectId> spaceProperties = new ChainedMap<String, ObjectId>().put("_id", authToken.spaceId)
				.put("owner", authToken.userId).get();
		try {
			if (!Space.exists(spaceProperties)) {
				return badRequest("Invalid authToken.");
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// return ids of records in this space
		Space space;
		try {
			space = Space.get(spaceProperties, new ChainedSet<String>().add("records").get());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(Json.toJson(space.records));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result getRecords() {
		// allow cross origin request from visualizations server
		String visualizationsServer = Play.application().configuration().getString("visualizations.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + visualizationsServer);

		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken and check whether space with corresponding owner exists
		AuthToken authToken = AuthToken.decrypt(json.get("authToken").asText());
		if (authToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, ObjectId> spaceProperties = new ChainedMap<String, ObjectId>().put("_id", authToken.spaceId)
				.put("owner", authToken.userId).get();
		try {
			if (!Space.exists(spaceProperties)) {
				return badRequest("Invalid authToken.");
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// get ids of records in this space
		Space space;
		try {
			space = Space.get(spaceProperties, new ChainedSet<String>().add("records").get());
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// filter out records that are not assigned to that space
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		Object recordIdSet = properties.get("_id");
		if (recordIdSet instanceof Set<?>) {
			Set<?> recordIds = (Set<?>) recordIdSet;
			Iterator<?> iterator = recordIds.iterator();
			while (iterator.hasNext()) {
				Object recordId = iterator.next();
				if (!space.records.contains(recordId)) {
					iterator.remove();
				}
			}
		} else {
			return badRequest("No set of record ids found.");
		}

		// get records
		return Records.getRecords(properties, fields);
	}
}
