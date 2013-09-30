package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				Record.class.getField(field).set(recordMap.get(id), data.get(key));
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
		return ok(list.render(spaceId, records, request().username()));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result jsonList() {
		System.out.println(request().toString());
		System.out.println(request().body().asJson());
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		}
		JsonNode node = json.findPath("record");
		if (node == null) {
			return badRequest("No message node found.");
		}
		return ok(node.getTextValue());
	}

}
