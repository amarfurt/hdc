package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.Record;

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonNode;

import play.data.Form;
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

	public static Result list() {
		// build the list of records from the data
		Map<String, String> data = Form.form().bindFromRequest().data();
		Map<String, Record> recordMap = new HashMap<String, Record>();
		String spaceId = null;
		for (String key : data.keySet()) {
			// space id is passed once
			if ("spaceId".equals(key)) {
				spaceId = data.get(key);
				continue;
			}

			// rest of the data are record objects
			String[] split = key.split(" ");
			String id = split[0];
			if (!recordMap.containsKey(id)) {
				Record newRecord = new Record();
				newRecord._id = new ObjectId(id);
				recordMap.put(id, newRecord);
			}
			String field = split[1];
			try {
				if (Record.class.getField(field).getType().equals(ObjectId.class)) {
					Record.class.getField(field).set(recordMap.get(id), new ObjectId(data.get(key)));
				} else {
					Record.class.getField(field).set(recordMap.get(id), data.get(key));
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
		List<Record> records = new ArrayList<Record>(recordMap.values());
		Collections.sort(records);
		return ok(list.render(spaceId, records, new ObjectId(request().username())));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result jsonList() {
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
				if (curField.getKey().equals("_id")) {
					newRecord._id = new ObjectId(curField.getValue().asText());
				} else {
					try {
						Record.class.getField(curField.getKey()).set(newRecord, curField.getValue().asText());
					} catch (Exception e) {
						return internalServerError(e.getMessage());
					}
				}
			}
			records.add(newRecord);
		}
		return ok(list.render(spaceId, records, new ObjectId(request().username())));
	}

}
