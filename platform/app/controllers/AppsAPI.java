package controllers;

import models.ModelException;
import models.Record;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

// Not secured, accessible from app server
public class AppsAPI extends Controller {

	public static Result checkPreflight(String userIdString, String appIdString) {
		// allow cross-origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result createRecord(String userIdString, String appIdString) {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "data", "name", "description");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// save new record with additional metadata
		if (!json.get("data").isTextual() || !json.get("name").isTextual() || !json.get("description").isTextual()) {
			return badRequest("At least one request parameter is of the wrong type.");
		}
		String data = json.get("data").asText();
		String name = json.get("name").asText();
		String description = json.get("description").asText();
		Record record = new Record();
		record._id = new ObjectId();
		record.app = new ObjectId(appIdString);
		record.owner = new ObjectId(userIdString);
		record.creator = new ObjectId(userIdString);
		record.created = DateTimeUtils.now();
		try {
			record.data = (DBObject) JSON.parse(data);
		} catch (JSONParseException e) {
			return badRequest("Record data is invalid JSON.");
		}
		record.name = name;
		record.description = description;
		try {
			Record.add(record);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Helper method for node server to retrieve tokens from the database.
	 */
	public static Result getTokens(String userIdString, String appIdString) {
		try {
			return ok(Json.toJson(Users.getTokens(new ObjectId(userIdString), new ObjectId(appIdString))));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
	}

	/**
	 * Helper method for node server to save tokens to the database.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result setTokens(String userIdString, String appIdString) {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "accessToken", "refreshToken");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// save the tokens to the database
		try {
			Users.setTokens(new ObjectId(userIdString), new ObjectId(appIdString), json.get("accessToken").asText(),
					json.get("refreshToken").asText());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(Json.newObject());
	}

	/**
	 * Get app details (duplicate code fragment from Apps class for now).
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result getAppDetails(String appIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "properties", "fields");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}
		return Apps.getApps(json);
	}
}
