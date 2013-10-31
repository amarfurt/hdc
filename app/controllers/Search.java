package controllers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import models.App;
import models.Circle;
import models.Message;
import models.Record;
import models.Space;
import models.User;
import models.Visualization;

import org.apache.commons.lang3.text.WordUtils;
import org.bson.types.ObjectId;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import utils.KeywordSearch;
import utils.TextSearch;
import utils.TextSearch.SearchResult;
import views.html.details.app;
import views.html.details.message;
import views.html.details.record;
import views.html.details.user;
import views.html.details.visualization;
import views.html.elements.searchresults;

@Security.Authenticated(Secured.class)
public class Search extends Controller {

	private static final int LIMIT = 10;

	/**
	 * Search for keywords matching 'search' in a user's 'domain'.
	 */
	public static Result find(String search, String domain) {
		Method method;
		try {
			method = Search.class.getMethod("find" + WordUtils.capitalize(domain), String.class);
		} catch (SecurityException e) {
			return internalServerError(e.getMessage());
		} catch (NoSuchMethodException e) {
			return internalServerError(e.getMessage());
		}
		try {
			return (Result) method.invoke(Search.class, search);
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InvocationTargetException e) {
			return internalServerError(e.getMessage());
		}
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
	 * FIND methods - show the result of the search in a list
	 */
	public static Result findRecords(String search) {
		Set<ObjectId> visibleRecordIds = Record.getVisible(new ObjectId(request().username()));
		Set<ObjectId> foundRecordIds = KeywordSearch.boundedSearch(visibleRecordIds, Record.getCollection(), search,
				LIMIT);
		ObjectId[] recordIds = new ObjectId[foundRecordIds.size()];
		Set<Record> recordSet;
		try {
			recordSet = Record.findAll(foundRecordIds.toArray(recordIds));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<Record> recordList = new ArrayList<Record>(recordSet);
		if (recordList.size() > 0) {
			Collections.sort(recordList);
			return ok(searchresults.render(recordList, "No record matched your search.", "record"));
		} else {
			List<SearchResult> textResults = TextSearch.search(search, visibleRecordIds);
			List<Record> textList = new ArrayList<Record>();
			for (SearchResult result : textResults) {
				Record record = new Record();
				record._id = new ObjectId(result.id);
				if (result.highlighted != null && !result.highlighted.isEmpty()) {
					record.data = result.highlighted;
				} else {
					record.data = result.data;
				}
				textList.add(record);
			}
			return ok(searchresults.render(textList, "No record matched your search.", "record"));
		}
	}

	public static Result findMessages(String search) {
		List<Message> receivedMessages;
		try {
			receivedMessages = Message.findSentTo(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<Message> foundMessages = KeywordSearch.searchInList(receivedMessages, search, LIMIT);
		Collections.sort(foundMessages);
		return ok(searchresults.render(foundMessages, "No message matched your search.", "message"));
	}

	public static Result findSpaces(String search) {
		List<Space> ownedSpaces;
		try {
			ownedSpaces = Space.findOwnedBy(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<Space> foundSpaces = KeywordSearch.searchInList(ownedSpaces, search, LIMIT);
		Collections.sort(foundSpaces);
		return ok(searchresults.render(foundSpaces, "No space matched your search.", "space"));
	}

	public static Result findContacts(String search) {
		List<User> contacts;
		try {
			contacts = Circle.findContacts(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<User> foundContacts = KeywordSearch.searchInList(contacts, search, LIMIT);
		Collections.sort(foundContacts);
		return ok(searchresults.render(foundContacts, "No contact matched your search.", "user"));
	}

	public static Result findCircles(String search) {
		List<Circle> ownedCircles;
		try {
			ownedCircles = Circle.findOwnedBy(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<Circle> foundCircles = KeywordSearch.searchInList(ownedCircles, search, LIMIT);
		Collections.sort(foundCircles);
		return ok(searchresults.render(foundCircles, "No circle matched your search.", "circle"));
	}

	public static Result findApps(String search) {
		List<App> installedApps;
		try {
			installedApps = App.findInstalledBy(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<App> foundApps = KeywordSearch.searchInList(installedApps, search, LIMIT);
		Collections.sort(foundApps);
		return ok(searchresults.render(foundApps, "None of your installed apps matched your search.", "app"));
	}

	public static Result findVisualizations(String search) {
		List<Visualization> installedVisualizations;
		try {
			installedVisualizations = Visualization.findInstalledBy(new ObjectId(request().username()));
		} catch (IllegalArgumentException e) {
			return internalServerError(e.getMessage());
		} catch (IllegalAccessException e) {
			return internalServerError(e.getMessage());
		} catch (InstantiationException e) {
			return internalServerError(e.getMessage());
		}
		List<Visualization> foundVisualizations = KeywordSearch.searchInList(installedVisualizations, search, LIMIT);
		Collections.sort(foundVisualizations);
		return ok(searchresults.render(foundVisualizations,
				"None of your installed visualizations matched your search.", "visualization"));
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
