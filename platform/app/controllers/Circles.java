package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Circle;
import models.ModelException;
import models.User;

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
		return ok(circles.render(new ObjectId(request().username())));
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
		Set<String> fields = JsonExtraction.extractSet(json.get("fields"));
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
		circle.name = name;
		circle.owner = userId;
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

		// delete circle
		try {
			Circle.delete(userId, circleId);
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
		Set<ObjectId> newMemberIds = ObjectIdConversion.toObjectIds(JsonExtraction.extractSet(json.get("users")));
		Set<String> fields = new ChainedSet<String>().add("members").add("shared").get();
		Set<ObjectId> sharedRecords;
		try {
			Circle circle = Circle.get(new ChainedMap<String, ObjectId>().put("_id", circleId).get(), fields);
			circle.members.addAll(newMemberIds);
			Circle.set(circle._id, "members", circle.members);
			sharedRecords = circle.shared;
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// also make records of this circle visible
		Map<String, Set<ObjectId>> properties = new ChainedMap<String, Set<ObjectId>>().put("_id", newMemberIds).get();
		fields = new ChainedSet<String>().add("visible." + userId.toString()).get();
		try {
			Set<User> newMembers = User.getAll(properties, fields);
			for (User newMember : newMembers) {
				if (!newMember.visible.containsKey(userId.toString())) {
					User.set(newMember._id, "visible." + userId.toString(), sharedRecords);
				} else {
					Set<ObjectId> visibleRecords = newMember.visible.get(userId.toString());
					visibleRecords.addAll(sharedRecords);
					User.set(newMember._id, "visible." + userId.toString(), visibleRecords);
				}
			}
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
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}

		// also remove records from visible records that are no longer shared with the member
		// get all circles this user is a member of
		fields = new ChainedSet<String>().add("shared").get();
		try {
			Set<Circle> circles = Circle.getAll(new ChainedMap<String, ObjectId>().put("members", memberId).get(),
					fields);
			HashSet<ObjectId> stillShared = new HashSet<ObjectId>();
			for (Circle circle : circles) {
				stillShared.addAll(circle.shared);
			}
			User.set(memberId, "visible." + userId.toString(), stillShared);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

}
