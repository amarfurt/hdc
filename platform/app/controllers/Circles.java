package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;

import org.bson.types.ObjectId;

import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.ObjectIdConversion;
import utils.json.JsonExtraction;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;
import views.html.circles;

import com.fasterxml.jackson.databind.JsonNode;

@Security.Authenticated(Secured.class)
public class Circles extends Controller {

	public static Result index() {
		return ok(circles.render());
	}

	public static Result details(String circleIdString) {
		return index();
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

		// get circles
		Map<String, Object> properties = JsonExtraction.extractMap(json.get("properties"));
		Set<String> fields = JsonExtraction.extractStringSet(json.get("fields"));
		List<Circle> circles;
		try {
			circles = new ArrayList<Circle>(Circle.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(circles);
		return ok(Json.toJson(circles));
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result add() {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "name");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		String name = json.get("name").asText();
		if (Circle.exists(new ChainedMap<String, Object>().put("owner", userId).put("name", name).get())) {
			return badRequest("A circle with this name already exists.");
		}

		// create new circle
		Circle circle = new Circle();
		circle._id = new ObjectId();
		circle.owner = userId;
		circle.name = name;
		circle.order = Circle.getMaxOrder(userId) + 1;
		circle.members = new HashSet<ObjectId>();
		circle.shared = new HashSet<ObjectId>();
		try {
			Circle.add(circle);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok(Json.toJson(circle));
	}

	public static Result delete(String circleIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(new ChainedMap<String, ObjectId>().put("_id", circleId).put("owner", userId).get())) {
			return badRequest("No circle with this id exists.");
		}

		// get the circle's members and shared records
		Circle circle;
		try {
			circle = Circle.get(new ChainedMap<String, ObjectId>().put("_id", circleId).get(), new ChainedSet<String>()
					.add("members").add("shared").get());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// delete circle
		try {
			Circle.delete(userId, circleId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// make the records of the deleted circle invisible to its former members
		try {
			Users.makeInvisible(userId, circle.shared, circle.members);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public static Result addUsers(String circleIdString) {
		// validate json
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "users");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(new ChainedMap<String, ObjectId>().put("_id", circleId).put("owner", userId).get())) {
			return badRequest("No circle with this id exists.");
		}

		// add users to circle (implicit: if not already present)
		Set<ObjectId> newMemberIds = ObjectIdConversion.castToObjectIds(JsonExtraction.extractSet(json.get("users")));
		Set<String> fields = new ChainedSet<String>().add("members").add("shared").get();
		Circle circle;
		try {
			circle = Circle.get(new ChainedMap<String, ObjectId>().put("_id", circleId).get(), fields);
			circle.members.addAll(newMemberIds);
			Circle.set(circle._id, "members", circle.members);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// also make records of this circle visible
		try {
			Users.makeVisible(userId, circle.shared, newMemberIds);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result removeMember(String circleIdString, String memberIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId circleId = new ObjectId(circleIdString);
		if (!Circle.exists(new ChainedMap<String, ObjectId>().put("_id", circleId).put("owner", userId).get())) {
			return badRequest("No circle with this id exists.");
		}

		// remove member from circle (implicit: if present)
		ObjectId memberId = new ObjectId(memberIdString);
		Set<String> fields = new ChainedSet<String>().add("members").add("shared").get();
		try {
			Circle circle = Circle.get(new ChainedMap<String, ObjectId>().put("_id", circleId).get(), fields);
			circle.members.remove(memberId);
			Circle.set(circle._id, "members", circle.members);

			// also remove records from visible records that are no longer shared with the member
			Users.makeInvisible(userId, circle.shared, new ChainedSet<ObjectId>().add(memberId).get());
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
