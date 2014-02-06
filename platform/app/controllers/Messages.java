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
import utils.search.Search;
import utils.search.SearchException;
import views.html.messages;
import views.html.details.message;
import views.html.dialogs.createmessage;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Messages extends Controller {

	public static Result index() {
		return ok(messages.render());
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
					new ChainedSet<String>().add("messages.inbox").get());
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
		message.created = DateTimeUtils.now();
		message.title = json.get("title").asText();
		message.content = json.get("content").asText();
		try {
			Message.add(message);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// add to inbox and search index of receivers
		for (User user : users) {
			user.messages.get("inbox").add(message._id);
			try {
				User.set(user._id, "messages.inbox", user.messages.get("inbox"));
				Search.add(user._id, "message", message._id, message.title, message.content);
			} catch (ModelException e) {
				return badRequest(e.getMessage());
			} catch (SearchException e) {
				return badRequest(e.getMessage());
			}
		}
		return ok();
	}

	public static Result move(String messageIdString, String from, String to) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		if (!User
				.exists(new ChainedMap<String, ObjectId>().put("_id", userId).put("messages." + from, messageId).get())) {
			return badRequest("No message with this id exists.");
		}

		// update the respective message folders
		try {
			User user = User.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>()
					.add("messages." + from).add("messages." + to).get());
			user.messages.get(from).remove(messageId);
			user.messages.get(to).add(messageId);
			User.set(userId, "messages." + from, user.messages.get(from));
			User.set(userId, "messages." + to, user.messages.get(to));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result remove(String messageIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		if (!User.exists(new ChainedMap<String, ObjectId>().put("_id", userId).put("messages.trash", messageId).get())) {
			return badRequest("No message with this id exists.");
		}

		// remove message from trash folder and user's search index
		try {
			User user = User.get(new ChainedMap<String, ObjectId>().put("_id", userId).get(), new ChainedSet<String>()
					.add("messages.trash").get());
			user.messages.get("trash").remove(messageId);
			User.set(userId, "messages.trash", user.messages.get("trash"));
			Search.delete(userId, "message", messageId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result delete(String messageIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId messageId = new ObjectId(messageIdString);
		if (!Message.exists(new ChainedMap<String, ObjectId>().put("_id", messageId).get())) {
			return badRequest("No message with this id exists.");
		}

		// remove the message from the user's inbox
		try {
			Message.delete(userId, messageId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}
}
