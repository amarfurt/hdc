package controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import models.Record;
import models.User;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.elements.spaces.recordForm;

@Security.Authenticated(Secured.class)
public class Search extends Controller {
	
	public static Result searchAll() {
		return null;
	}
	
	public static Result searchByType(String collection, String search) {
		DBObject query = new BasicDBObject();
		query.put("tags", new BasicDBObject("$in", split(search)));
		switch (collection) {
		case "users":
			// distinguish between "users in your circles" and other
			break;
			
		case "records":
			break;

		default:
			break;
		}
		return null;
	}
	
	private static String[] split(String search) {
		return search.toLowerCase().split(" ");
	}

	/**
	 * Return a list of records whose data contains the current search term.
	 */
	public static Result searchRecords(String spaceId, String search) {
		List<Record> response = new ArrayList<Record>();
		try {
			// TODO use caching
			User user = User.find(request().username());
			ObjectId sId = new ObjectId(spaceId);
			response = Record.findNotInSpace(user, sId);
			if (search != null && !search.isEmpty()) {
				Iterator<Record> iterator = response.iterator();
				while (iterator.hasNext()) {
					Record cur = iterator.next();
					if (!cur.data.toLowerCase().contains(search.toLowerCase())) {
						iterator.remove();
					}
				}
			}
			return ok(recordForm.render(response, user, sId));
		} catch (IllegalArgumentException e) {
			return badRequest(e.getMessage());
		} catch (IllegalAccessException e) {
			return badRequest(e.getMessage());
		} catch (InstantiationException e) {
			return badRequest(e.getMessage());
		}
	}
}
