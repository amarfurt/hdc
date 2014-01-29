package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Message;
import models.ModelException;
import models.User;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.DateTimeUtils;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.index;
import views.html.details.message;
import views.html.dialogs.createmessage;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Messages extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	public static Result details(String messageIdString) {
		return ok(message.render());
	}

	public static Result create() {
		return ok(createmessage.render());
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

		// get messages
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Message> messages;
		try {
			messages = new ArrayList<Message>(Message.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(messages);
		return ok(Json.toJson(messages));
	}

	public static Result send() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "receivers", "title", "content");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate receivers
		Set<ObjectId> receiverIds = ObjectIdConversion
				.castToObjectIds(JsonExtraction.extractSet(json.get("receivers")));
		Set<User> users;
		try {
			users = User.getAll(new ChainedMap<String, Set<ObjectId>>().put("_id", receiverIds).get(),
					new ChainedSet<String>().add("_id").get());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		if (receiverIds.size() != users.size()) {
			return badRequest("One or more users could not be found.");
		}

		// create message
		Message message = new Message();
		message._id = new ObjectId();
		message.sender = new ObjectId(request().username());
		message.receivers = receiverIds;
		message.inbox = receiverIds;
		message.created = DateTimeUtils.now();
		message.title = json.get("title").asText();
		message.content = json.get("content").asText();
		try {
			Message.add(message);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}
}
