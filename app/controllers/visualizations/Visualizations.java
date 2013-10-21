package controllers.visualizations;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Secured;

@Security.Authenticated(Secured.class)
public class Visualizations extends Controller {

	public static Result loading() {
		return ok("Loading space...");
	}

	public static Result show(String requestId) {
		return (Result) Cache.get(requestId + ":" + request().username());
	}

	protected static String requestComplete(JsonNode json) {
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
