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
import views.html.visualizations.list;
import views.html.visualizations.runaggregator;

@Security.Authenticated(Secured.class)
public class Visualizations extends Controller {

	public static Result loading() {
		return ok("Loading space...");
	}

	public static Result show(String requestId) {
		return (Result) Cache.get(requestId + ":" + request().username());
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result list() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		String requestComplete = requestComplete(json);
		if (requestComplete != null) {
			return badRequest(requestComplete);
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
	
	@BodyParser.Of(BodyParser.Json.class)
	public static Result runAggregator() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		String requestComplete = requestComplete(json);
		if (requestComplete != null) {
			return badRequest(requestComplete);
		}
		
		double distance = 0;
		double time = 0;
		Iterator<JsonNode> elements = json.get("records").getElements();
		while (elements.hasNext()) {
			JsonNode cur = elements.next();
			if (cur.has("data")) {
				// assume the format "... {distance}[ ]km in {time}[ ]h ..."
				String data = cur.get("data").asText().toLowerCase();
				if (!data.matches(".+km in .+h.*")) {
					continue;
				}
				String curDistance = data.substring(0, data.lastIndexOf("km")).trim();
				curDistance = curDistance.substring(curDistance.lastIndexOf(" ") + 1);
				distance += Double.parseDouble(curDistance);
				
				String curTime = data.substring(0, data.lastIndexOf("h")).trim();
				curTime = curTime.substring(curTime.lastIndexOf(" ") + 1);
				time += Double.parseDouble(curTime);
			}
		}
		double speed = 0;
		if (time > 0) {
			speed = distance / time;
		}
		String distanceString = String.format("%.2f", distance);
		String timeString = String.format("%.2f", time);
		String speedString = String.format("%.2f", speed);
		Result response = ok(runaggregator.render(distanceString, timeString, speedString));
		
		// cache the response and return the url to retrieve it
		ObjectId requestId = new ObjectId();
		Cache.set(requestId.toString() + ":" + request().username(), response);
		return ok(routes.Visualizations.show(requestId.toString()).url());
	}
	
	private static String requestComplete(JsonNode json) {
		if (json == null) {
			return "No json found.";
		} else if (json.get("spaceId") == null) {
			return "No space id found.";
		} else if (json.get("records") == null) {
			return "No records found.";
		} else {
			return null;
		}
	}

}
