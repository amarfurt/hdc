package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import models.Message;
import models.ModelException;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.index;
import views.html.details.message;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Messages extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Message> messages;
		try {
			messages = new ArrayList<Message>(Message.findSentTo(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(messages);
		return ok(Json.toJson(messages));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result get() {
		// validate json
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (!json.has("messages")) {
			return badRequest("Request parameter 'messages' not found.");
		}
		// TODO add fields selector
		// else if (!json.has("fields")) {
		// return badRequest("Request parameter 'fields' not found.");
		// }

		// get messages
		List<ObjectId> messageIds = new ArrayList<ObjectId>();
		Iterator<JsonNode> iterator = json.get("messages").iterator();
		while (iterator.hasNext()) {
			messageIds.add(new ObjectId(iterator.next().asText()));
		}
		List<Message> messages = new ArrayList<Message>();
		try {
			for (ObjectId messageId : messageIds) {
				messages.add(Message.find(messageId));
			}
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(messages);
		return ok(Json.toJson(messages));
	}

	public static Result index() {
		return ok(index.render(new ObjectId(request().username())));
	}
	
	public static Result details(String messageIdString) {
		return ok(message.render(new ObjectId(request().username())));
	}
}
