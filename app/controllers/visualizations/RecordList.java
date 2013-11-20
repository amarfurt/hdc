package controllers.visualizations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import models.Circle;
import models.Record;
import models.Space;
import models.User;

import org.bson.types.ObjectId;

import play.cache.Cache;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.ModelConversion.ConversionException;
import views.html.visualizations.recordlist;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Secured;

@Security.Authenticated(Secured.class)
public class RecordList extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	public static Result load() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		String requestComplete = Visualizations.requestComplete(json);
		if (requestComplete != null) {
			return badRequest(requestComplete);
		}

		// parse the space id and the records
		String spaceId = json.get("spaceId").asText();
		List<Record> records = new ArrayList<Record>();
		for (JsonNode cur : json.get("records")) {
			Record newRecord = new Record();
			Iterator<Entry<String, JsonNode>> fields = cur.fields();
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
		Result response = ok(recordlist.render(spaceId, records, new ObjectId(request().username())));

		// cache the response and return the url to retrieve it (will be loaded in iframe)
		ObjectId requestId = new ObjectId();
		Cache.set(requestId.toString() + ":" + request().username(), response);
		return ok(routes.Visualizations.show(requestId.toString()).url());
	}

	/**
	 * Find the spaces that contain the given record.
	 */
	public static Result findSpacesWith(String recordId) {
		Set<ObjectId> spaceIds = Space.findWithRecord(new ObjectId(recordId), new ObjectId(request().username()));
		Set<String> spaces = new HashSet<String>();
		for (ObjectId id : spaceIds) {
			spaces.add(id.toString());
		}
		return ok(Json.toJson(spaces));
	}

	/**
	 * Find the circles the given record is shared with.
	 */
	public static Result findCirclesWith(String recordId) {
		Set<ObjectId> circleIds = Circle.findWithRecord(new ObjectId(recordId), new ObjectId(request().username()));
		Set<String> circles = new HashSet<String>();
		for (ObjectId id : circleIds) {
			circles.add(id.toString());
		}
		return ok(Json.toJson(circles));
	}

	/**
	 * Updates the spaces the given record is in.
	 */
	public static Result updateSpaces(String recordId, List<String> spaces) {
		Set<ObjectId> spaceIds = new HashSet<ObjectId>();
		for (String id : spaces) {
			spaceIds.add(new ObjectId(id));
		}
		try {
			String errorMessage = Space.updateRecords(spaceIds, new ObjectId(recordId), new ObjectId(request()
					.username()));
			if (errorMessage == null) {
				return ok();
			} else {
				return badRequest(errorMessage);
			}
		} catch (ConversionException e) {
			return internalServerError(e.getMessage());
		}
	}

	/**
	 * Updates the circles the given record is shared with.
	 */
	public static Result updateSharing(String record, List<String> circlesStarted, List<String> circlesStopped) {
		ObjectId recordId = new ObjectId(record);
		Set<ObjectId> circleIdsStarted = new HashSet<ObjectId>();
		for (String id : circlesStarted) {
			circleIdsStarted.add(new ObjectId(id));
		}
		Set<ObjectId> circleIdsStopped = new HashSet<ObjectId>();
		for (String id : circlesStopped) {
			circleIdsStopped.add(new ObjectId(id));
		}

		// TODO Security checks here?

		// update circles
		ObjectId userId = new ObjectId(request().username());
		String errorMessage = Circle.startSharingWith(userId, recordId, circleIdsStarted);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}

		errorMessage = Circle.stopSharingWith(userId, recordId, circleIdsStopped);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}

		// update visible field of all involved users
		HashSet<ObjectId> recordIds = new HashSet<ObjectId>();
		recordIds.add(recordId);
		HashSet<ObjectId> userIdsStarted = new HashSet<ObjectId>();
		for (ObjectId circleId : circleIdsStarted) {
			userIdsStarted.addAll(Circle.getMembers(circleId));
		}
		errorMessage = User.makeRecordsVisible(userId, recordIds, userIdsStarted);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		// TODO don't remove from users that are part of another circle of the owner that this record is also shared
		// with
		// TODO solve with pushing records so that there are duplicates in visible field? retrieve and add to set to
		// avoid duplicates in the application
		HashSet<ObjectId> userIdsStopped = new HashSet<ObjectId>();
		for (ObjectId circleId : circleIdsStopped) {
			userIdsStopped.addAll(Circle.getMembers(circleId));
		}
		errorMessage = User.makeRecordsInvisible(userId, recordIds, userIdsStopped);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}
		return ok();
	}

}
