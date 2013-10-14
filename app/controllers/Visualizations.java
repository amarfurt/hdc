package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import models.Record;

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonNode;

import play.cache.Cache;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.visualization.list.list;

@Security.Authenticated(Secured.class)
public class Visualizations extends Controller {

	public static Result loading() {
		return ok("Loading space...");
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result list() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (json.get("spaceId") == null) {
			return badRequest("No space id found.");
		} else if (json.get("records") == null) {
			return badRequest("No records found.");
		}

		// parse the space id and the records
		String spaceId = json.get("spaceId").asText();
		List<Record> records = new ArrayList<Record>();
		Iterator<JsonNode> elements = json.get("records").getElements();
		while (elements.hasNext()) {
			JsonNode cur = elements.next();
			Record newRecord = new Record();
			Iterator<Entry<String, JsonNode>> fields = cur.getFields();
			while (fields.hasNext()) {
				Entry<String, JsonNode> curField = fields.next();
				try {
					if (Record.class.getField(curField.getKey()).getType().equals(ObjectId.class)) {
						Record.class.getField(curField.getKey()).set(newRecord,
								new ObjectId(curField.getValue().asText()));
					} else {
						Record.class.getField(curField.getKey()).set(newRecord, curField.getValue().asText());
					}
				} catch (IllegalArgumentException e) {
					return internalServerError(e.getMessage());
				} catch (IllegalAccessException e) {
					return internalServerError(e.getMessage());
				} catch (NoSuchFieldException e) {
					return internalServerError(e.getMessage());
				} catch (SecurityException e) {
					return internalServerError(e.getMessage());
				}
			}
			records.add(newRecord);
		}

		// sort the records and create the response
		Collections.sort(records);
		Result response = ok(list.render(spaceId, records, new ObjectId(request().username())));

		// cache the response and return the url to retrieve it (will be loaded in iframe)
		ObjectId requestId = new ObjectId();
		Cache.set(requestId.toString() + ":" + request().username(), response);
		return ok(routes.Visualizations.show(requestId.toString()).url());
	}

	public static Result show(String requestId) {
		return (Result) Cache.get(requestId + ":" + request().username());
	}

}
