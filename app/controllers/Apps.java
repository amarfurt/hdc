package controllers;

import models.Record;

import org.bson.types.ObjectId;

import play.cache.Cache;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.ModelConversion.ConversionException;

import com.fasterxml.jackson.databind.JsonNode;

// TODO Security
public class Apps extends Controller {

	public static Result getRecord(String recordId) {
//		Record record;
//		try {
//			record = Record.find(new ObjectId(recordId));
//		} catch (ConversionException e) {
//			return badRequest(e.getMessage());
//		}

		Record record = new Record();
		record.data = (String) Cache.get("data");
		if (record.data == null) {
			record.data = "No data cached.";
		}
		
		// allow cross origin request from app server
		response().setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
		return ok(record.data);
	}

	public static Result checkPreFlight() {
		// allow cross origin request from app server
		response().setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result createRecord() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		if (json == null) {
			return badRequest("No json found.");
		} else if (json.get("data") == null) {
			return badRequest("No data found.");
		} else if (json.get("description") == null) {
			return badRequest("No description found.");
		}

		// check whether the data is in the correct format and not empty
		String data = json.get("data").toString();
		String description = json.get("description").asText();
		if (data == null || data.isEmpty()) {
			return badRequest("Wrong data format.");
		} else if (description == null || description.isEmpty()) {
			return badRequest("Wrong description format.");
		}
		
		Cache.set("data", data);

		// allow cross origin request from app server
		response().setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
		return ok();
	}

}
