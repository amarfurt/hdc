package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.ModelException;
import models.Record;
import models.Space;
import models.User;
import models.Visualization;

import org.bson.types.ObjectId;

import play.Play;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.elements.recordsearchresults;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBList;

import controllers.forms.SpaceForm;

@Security.Authenticated(Secured.class)
public class Spaces extends Controller {

	public static Result fetch() {
		ObjectId userId = new ObjectId(request().username());
		List<Space> spaces;
		try {
			spaces = new ArrayList<Space>(Space.findOwnedBy(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(spaces);
		return ok(Json.toJson(spaces));
	}

	public static Result index() {
		return show(Form.form(SpaceForm.class), null);
	}

	public static Result show(String activeSpaceId) {
		return show(Form.form(SpaceForm.class), new ObjectId(activeSpaceId));
	}

	public static Result show(Form<SpaceForm> spaceForm, ObjectId activeSpace) {
		ObjectId userId = new ObjectId(request().username());
		List<Record> records;
		List<Space> spaces;
		try {
			records = new ArrayList<Record>(Record.findVisible(userId));
			spaces = new ArrayList<Space>(Space.findOwnedBy(userId));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(records);
		Collections.sort(spaces);
		return ok(views.html.spaces.render(spaceForm, records, spaces, activeSpace, userId));
	}

	/**
	 * Displays a note when a visualization is loading.
	 */
	public static Result loading() {
		return ok("Loading space...");
	}

	public static Result add() {
		// check whether validation failed
		Form<SpaceForm> spaceForm = Form.form(SpaceForm.class).bindFromRequest();
		if (spaceForm.hasErrors()) {
			return show(spaceForm, null);
		}

		// construct new space
		SpaceForm form = spaceForm.get();
		Space newSpace = new Space();
		newSpace.name = form.name;
		newSpace.owner = new ObjectId(request().username());
		newSpace.visualization = new ObjectId(form.visualization);
		newSpace.records = new BasicDBList();
		try {
			Space.add(newSpace);
		} catch (ModelException e) {
			spaceForm.reject(e.getMessage());
			return show(spaceForm, null);
		}

		// TODO (?) js ajax insertion, open newly added space
		// return ok(space.render(newSpace));
		return redirect(routes.Spaces.show(newSpace._id.toString()));
	}

	public static Result rename(String spaceIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		String newName = Form.form().bindFromRequest().get("name");
		if (!Space.exists(userId, spaceId)) {
			return badRequest("No space with this id exists.");
		} else if (Space.exists(userId, newName)) {
			return badRequest("A space with this name already exists.");
		}

		// rename space
		try {
			Space.rename(userId, spaceId, newName);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result delete(String spaceIdString) {
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		if (!Space.exists(userId, spaceId)) {
			return badRequest("No space with this id exists.");
		}

		// delete space
		try {
			Space.delete(userId, spaceId);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	public static Result addRecords(String spaceIdString) {
		// TODO pass data with ajax (same as updating spaces of a single record)
		// validate request
		ObjectId userId = new ObjectId(request().username());
		ObjectId spaceId = new ObjectId(spaceIdString);
		if (!Space.exists(userId, spaceId)) {
			return badRequest("No space with this id exists.");
		}

		// add records to space (implicit: if not already present)
		Map<String, String> data = Form.form().bindFromRequest().data();
		for (String recordId : data.keySet()) {
			// skip search input field
			if (recordId.equals("recordSearch")) {
				continue;
			}
			try {
				Space.addRecord(spaceId, new ObjectId(recordId));
			} catch (ModelException e) {
				return badRequest(e.getMessage());
			}
		}
		// TODO return ok();
		return redirect(routes.Spaces.show(spaceIdString));
	}

	/**
	 * Return a list of records whose data contains the current search term and is not in the space already.
	 */
	public static Result searchRecords(String spaceIdString, String query) {
		List<Record> records = new ArrayList<Record>();
		int limit = 10;
		ObjectId userId = new ObjectId(request().username());
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userId);
		ObjectId spaceId = new ObjectId(spaceIdString);
		Set<ObjectId> recordsAlreadyInSpace = Space.getRecords(spaceId);
		while (records.size() < limit) {
			// TODO use caching/incremental retrieval of results (scrolls)
			List<SearchResult> searchResults = Search.searchRecords(userId, visibleRecords, query);
			Set<ObjectId> recordIds = new HashSet<ObjectId>();
			for (SearchResult searchResult : searchResults) {
				recordIds.add(new ObjectId(searchResult.id));
			}
			recordIds.removeAll(recordsAlreadyInSpace);
			ObjectId[] targetArray = new ObjectId[recordIds.size()];
			try {
				records.addAll(Record.findAll(recordIds.toArray(targetArray)));
			} catch (ModelException e) {
				return internalServerError(e.getMessage());
			}

			// TODO break if scrolling finds no more results
			break;
		}
		Collections.sort(records);
		return ok(recordsearchresults.render(records));
	}

	public static Result loadAllRecords() {
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.findVisible(new ObjectId(request().username())));
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		Collections.sort(records);

		// format records
		List<ObjectNode> jsonRecords = new ArrayList<ObjectNode>(records.size());
		for (Record record : records) {
			ObjectNode jsonRecord = Json.newObject();
			jsonRecord.put("_id", record._id.toString());
			jsonRecord.put("app", record.app.toString());
			jsonRecord.put("owner", record.owner.toString());
			jsonRecord.put("creator", record.creator.toString());
			jsonRecord.put("created", record.created);
			jsonRecord.put("data", record.data);
			jsonRecord.put("name", record.name);
			jsonRecord.put("description", record.description);
			jsonRecords.add(jsonRecord);
		}
		return ok(Json.toJson(jsonRecords));
	}

	public static Result loadRecords(String spaceIdString) {
		Set<ObjectId> records = Space.getRecords(new ObjectId(spaceIdString));
		List<String> recordIds = new ArrayList<String>(records.size());
		for (ObjectId recordId : records) {
			recordIds.add(recordId.toString());
		}
		return ok(Json.toJson(recordIds));
	}

	public static Result getVisualizationUrl(String spaceIdString) {
		ObjectId visualizationId = Space.getVisualizationId(new ObjectId(spaceIdString), new ObjectId(request()
				.username()));
		String externalServer = Play.application().configuration().getString("external.server");
		String url = "http://" + externalServer + "/" + visualizationId + "/" + Visualization.getUrl(visualizationId);
		return ok(url);
	}

}
