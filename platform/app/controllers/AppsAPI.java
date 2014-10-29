package controllers;

import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Record;
import models.User;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.auth.AppToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

// Not secured, accessible from app server
public class AppsAPI extends Controller {

	public static Result checkPreflight() {
		// allow cross-origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result createRecord() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "data", "name", "description");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}
		Map<String, ObjectId> userProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.userId).put("apps", appToken.appId)
				.get();
		try {
			if (!User.exists(userProperties)) {
				return badRequest("Invalid authToken.");
			}
		} catch (ModelException e) {
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
		record.app = appToken.appId;
		record.owner = appToken.userId;
		record.creator = appToken.userId;
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
	 * Helper method for OAuth apps: API calls can sometimes only be done from the backend.
	 */
	public static Promise<Result> oAuthCall() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (final JsonValidationException e) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return badRequest(e.getMessage());
				}
			});
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return badRequest("Invalid authToken.");
				}
			});
		}
		Map<String, ObjectId> userProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.userId).put("apps", appToken.appId)
				.get();
		Set<String> fields = new ChainedSet<String>().add("tokens." + appToken.appId.toString()).get();
		String accessToken;
		try {
			if (!User.exists(userProperties)) {
				return Promise.promise(new Function0<Result>() {
					public Result apply() {
						return badRequest("Invalid authToken.");
					}
				});
			} else {
				User user = User.get(userProperties, fields);
				accessToken = user.tokens.get(appToken.appId.toString()).get("accessToken");
			}
		} catch (final ModelException e) {
			return Promise.promise(new Function0<Result>() {
				public Result apply() {
					return badRequest(e.getMessage());
				}
			});
		}

		// perform OAuth API call on behalf of the app
		WSRequestHolder holder = WS.url(json.get("url").asText());
		holder.setHeader("Authorization", "Bearer " + accessToken);
		Promise<Result> promise = holder.get().map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});
		return promise;
	}

}
