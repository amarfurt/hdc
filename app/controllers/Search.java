package controllers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.App;
import models.Message;
import models.Record;
import models.User;
import models.Visualization;

import org.apache.commons.lang3.text.WordUtils;
import org.bson.types.ObjectId;

import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.search.SearchResult;
import utils.search.TextSearch;
import views.html.search;
import views.html.details.app;
import views.html.details.message;
import views.html.details.record;
import views.html.details.user;
import views.html.details.visualization;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Security.Authenticated(Secured.class)
public class Search extends Controller {

	/**
	 * Search in all the user's accessible data.
	 */
	public static Result globalSearch() {
		String query = Form.form().bindFromRequest().get("globalSearch");
		return redirect(routes.Search.search(query));
	}

	public static Result search(String query) {
		ObjectId userId = new ObjectId(request().username());
		Map<ObjectId, Set<ObjectId>> visibleRecords = User.getVisibleRecords(userId);
		Map<String, List<SearchResult>> searchResults = TextSearch.search(userId, visibleRecords, query);
		return ok(search.render(searchResults, userId));
	}

	/**
	 * Suggests completions for the given query. Used by the auto-completion feature.
	 */
	public static Result complete(String query) {
		// TODO for now, simply complete with record keywords to test the UI feature
		List<Record> records;
		try {
			records = Record.findVisible(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<ObjectNode> jsonRecords = new ArrayList<ObjectNode>();
		for (Record record : records) {
			// check whether one of the keywords matches
			boolean matches = false;
			for (Object keyword : record.keywords) {
				String curKeyword = (String) keyword;
				if (curKeyword.startsWith(query)) {
					matches = true;
					break;
				}
			}

			// if one does, add the record to the result
			if (matches) {
				ObjectNode datum = Json.newObject();
				datum.put("value", record.data.substring(0, Math.min(40, record.data.length())));
				datum.put("tokens", Json.toJson(record.keywords));
				jsonRecords.add(datum);
			}
		}
		return ok(Json.toJson(jsonRecords));
	}

	/**
	 * Display the details for the 'model' with id 'objectId'.
	 */
	public static Result show(String model, String objectId) {
		ObjectId id = new ObjectId(objectId);
		Method method;
		try {
			method = Search.class.getMethod("show" + WordUtils.capitalize(model), ObjectId.class);
		} catch (SecurityException e) {
			return internalServerError(e.getMessage());
		} catch (NoSuchMethodException e) {
			return internalServerError(e.getMessage());
		}
		try {
			return (Result) method.invoke(Search.class, id);
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
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		return ok(record.render(recordToShow, new ObjectId(request().username())));
	}

	public static Result showMessage(ObjectId messageId) {
		Message messageToShow;
		try {
			messageToShow = Message.find(messageId);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
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
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
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
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		return ok(app.render(appToShow, new ObjectId(request().username())));
	}

	public static Result showVisualization(ObjectId visualizationId) {
		Visualization visualizationToShow;
		try {
			visualizationToShow = Visualization.find(visualizationId);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		return ok(visualization.render(visualizationToShow, new ObjectId(request().username())));
	}
}
