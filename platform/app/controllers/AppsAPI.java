package controllers;

import java.io.File;
import java.util.Map;
import java.util.Set;

import models.App;
import models.ModelException;
import models.Record;
import models.User;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.oauth.OAuth.ConsumerKey;
import play.libs.oauth.OAuth.OAuthCalculator;
import play.libs.oauth.OAuth.RequestToken;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.auth.AppToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.DatabaseException;
import utils.db.FileStorage;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
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
	 * Accepts and stores files up to sizes of 2GB.
	 */
	public static Result uploadFile() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check meta data
		MultipartFormData formData = request().body().asMultipartFormData();
		Map<String, String[]> metaData = formData.asFormUrlEncoded();
		if (!metaData.containsKey("authToken") || !metaData.containsKey("name") || !metaData.containsKey("description")) {
			return badRequest("At least one request parameter is missing.");
		}

		// decrypt authToken and check whether a user exists who has the app installed
		if (metaData.get("authToken").length != 1) {
			return badRequest("Invalid authToken.");
		}
		AppToken appToken = AppToken.decrypt(metaData.get("authToken")[0]);
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

		// extract file from data
		FilePart fileData = formData.getFile("file");
		if (fileData == null) {
			return badRequest("No file found.");
		}
		File file = fileData.getFile();
		String filename = fileData.getFilename();
		String contentType = fileData.getContentType();

		// create record
		Record record = new Record();
		record._id = new ObjectId();
		record.app = appToken.appId;
		record.owner = appToken.userId;
		record.creator = appToken.userId;
		record.created = DateTimeUtils.now();
		record.name = metaData.get("name")[0];
		record.description = metaData.get("description")[0];
		record.data = new BasicDBObject(new ChainedMap<String, String>().put("type", "file").put("name", filename)
				.put("contentType", contentType).get());

		// save file with file storage utility
		try {
			Record.add(record);
			FileStorage.store(file, record._id, filename, contentType);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		} catch (DatabaseException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Helper method for OAuth 2.0 apps: API calls can sometimes only be done from the backend. Uses the
	 * "Authorization: Bearer [accessToken]" header.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Promise<Result> oAuth2Call() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequestPromise("Invalid authToken.");
		}
		Map<String, ObjectId> userProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.userId).put("apps", appToken.appId)
				.get();
		Set<String> fields = new ChainedSet<String>().add("tokens." + appToken.appId.toString()).get();
		String accessToken;
		try {
			if (!User.exists(userProperties)) {
				return badRequestPromise("Invalid authToken.");
			} else {
				User user = User.get(userProperties, fields);
				accessToken = user.tokens.get(appToken.appId.toString()).get("accessToken");
			}
		} catch (ModelException e) {
			return badRequestPromise(e.getMessage());
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

	/**
	 * Helper method for OAuth 1.0 apps: Need to compute signature based on consumer secret, which should stay in the backend.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Promise<Result> oAuth1Call() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "url");
		} catch (JsonValidationException e) {
			return badRequestPromise(e.getMessage());
		}

		// decrypt authToken and check whether a user exists who has the app installed
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequestPromise("Invalid authToken.");
		}
		Map<String, ObjectId> userProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.userId).put("apps", appToken.appId)
				.get();
		Set<String> fields = new ChainedSet<String>().add("tokens." + appToken.appId.toString()).get();
		String oauthToken, oauthTokenSecret;
		try {
			if (!User.exists(userProperties)) {
				return badRequestPromise("Invalid authToken.");
			} else {
				User user = User.get(userProperties, fields);
				oauthToken = user.tokens.get(appToken.appId.toString()).get("oauthToken");
				oauthTokenSecret = user.tokens.get(appToken.appId.toString()).get("oauthTokenSecret");
			}
		} catch (ModelException e) {
			return badRequestPromise(e.getMessage());
		}

		// also get the consumer key and secret
		Map<String, ObjectId> appProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.appId).get();
		fields = new ChainedSet<String>().add("consumerKey").add("consumerSecret").get();
		App app;
		try {
			if (!App.exists(appProperties)) {
				return badRequestPromise("Invalid authToken");
			} else {
				app = App.get(appProperties, fields);
			}
		} catch (ModelException e) {
			return badRequestPromise(e.getMessage());
		}

		// perform the api call
		ConsumerKey key = new ConsumerKey(app.consumerKey, app.consumerSecret);
		RequestToken token = new RequestToken(oauthToken, oauthTokenSecret);
		return WS.url(json.get("url").asText()).sign(new OAuthCalculator(key, token)).get().map(new Function<WSResponse, Result>() {
			public Result apply(WSResponse response) {
				return ok(response.asJson());
			}
		});
	}

	private static Promise<Result> badRequestPromise(final String errorMessage) {
		return Promise.promise(new Function0<Result>() {
			public Result apply() {
				return badRequest(errorMessage);
			}
		});
	}

}
