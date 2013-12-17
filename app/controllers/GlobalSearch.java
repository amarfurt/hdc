package controllers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.App;
import models.Message;
import models.ModelException;
import models.Record;
import models.User;
import models.Visualization;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.text.WordUtils;
import org.bson.types.ObjectId;

import play.Play;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.Search;
import utils.search.SearchResult;
import views.html.search;
import views.html.details.app;
import views.html.details.message;
import views.html.details.record;
import views.html.details.user;
import views.html.details.visualization;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated(Secured.class)
public class GlobalSearch extends Controller {

	/**
	 * Search in all the user's accessible data.
	 */
	public static Result submit() {
		String query = Form.form().bindFromRequest().get("globalSearch");
		return redirect(routes.GlobalSearch.search(query));
	}

	public static Result search(String query) {
		ObjectId userId = new ObjectId(request().username());
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userId);
		Map<String, List<SearchResult>> searchResults = Search.search(userId, visibleRecords, query);
		return ok(search.render(searchResults, userId));
	}

	/**
	 * Suggests completions for the given query. Used by the auto-completion feature.
	 */
	public static Result complete(String query) {
		Map<String, List<SearchResult>> completions = Search.complete(new ObjectId(request().username()), query);
		List<ObjectNode> jsonRecords = new ArrayList<ObjectNode>();
		for (String type : completions.keySet()) {
			for (SearchResult completion : completions.get(type)) {
				ObjectNode datum = Json.newObject();
				datum.put("value", completion.title);
				datum.put("tokens", Json.toJson(completion.title.split("[ ,\\.]+")));
				datum.put("type", type);
				datum.put("id", completion.id);
				jsonRecords.add(datum);
			}
		}
		return ok(Json.toJson(jsonRecords));
	}

	/**
	 * Display the details for the 'type' with id 'objectId'.
	 */
	public static Result show(String type, String objectId) {
		ObjectId id = new ObjectId(objectId);
		Method method;
		try {
			method = GlobalSearch.class.getMethod("show" + WordUtils.capitalize(type), ObjectId.class);
		} catch (SecurityException e) {
			return internalServerError(e.getMessage());
		} catch (NoSuchMethodException e) {
			return internalServerError(e.getMessage());
		}
		try {
			return (Result) method.invoke(GlobalSearch.class, id);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InvocationTargetException e) {
			return internalServerError(e.getMessage());
		}
	}

	/*
	 * SHOW methods - display the details of the selected search result
	 */
	public static Result showRecord(ObjectId recordId) {
		Record recordToShow;
		try {
			recordToShow = Record.find(recordId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// put together url to
		String externalServer = Play.application().configuration().getString("external.server");
		String encodedData = new String(Base64.encodeBase64(recordToShow.data.getBytes()));
		String detailsUrl = App.getDetails(recordToShow.app).replace(":record", encodedData);
		String url = "http://" + externalServer + "/" + recordToShow.app.toString() + "/" + detailsUrl;
		Result result = ok(record.render(recordToShow, url, new ObjectId(request().username())));
		return result;
	}

	public static Result showMessage(ObjectId messageId) {
		Message messageToShow;
		try {
			messageToShow = Message.find(messageId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(message.render(messageToShow, new ObjectId(request().username())));
	}

	public static Result showSpace(ObjectId spaceId) {
		return redirect(routes.Spaces.show(spaceId.toString()));
	}

	public static Result showUser(ObjectId userId) {
		User userToShow;
		try {
			userToShow = User.find(userId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(user.render(userToShow, new ObjectId(request().username())));
	}

	public static Result showCircle(ObjectId circleId) {
		return redirect(routes.Circles.show(circleId.toString()));
	}

	public static Result showApp(ObjectId appId) {
		App appToShow;
		try {
			appToShow = App.find(appId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(app.render(appToShow, new ObjectId(request().username())));
	}

	public static Result showVisualization(ObjectId visualizationId) {
		Visualization visualizationToShow;
		try {
			visualizationToShow = Visualization.find(visualizationId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}
		return ok(visualization.render(visualizationToShow, new ObjectId(request().username())));
	}
}
