package controllers;

import models.App;
import models.ModelException;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

import play.Play;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.dialogs.createrecords;

@Security.Authenticated(Secured.class)
public class Records extends Controller {

	public static Result create(String appIdString) {
		ObjectId appId = new ObjectId(appIdString);
		App app;
		try {
			app = App.find(appId);
		} catch (ModelException e) {
			return internalServerError(e.getMessage());
		}

		// create reply to address and encode it with Base64
		String applicationServer = Play.application().configuration().getString("application.server");
		String replyTo = "http://" + applicationServer
				+ routes.Apps.createRecord(appIdString, request().username()).url();
		String encodedReplyTo = new String(new Base64().encode(replyTo.getBytes()));

		// put together url to load in iframe
		String externalServer = Play.application().configuration().getString("external.server");
		String createUrl = app.create.replace(":replyTo", encodedReplyTo);
		String url = "http://" + externalServer + "/" + appIdString + "/" + createUrl;
		return ok(createrecords.render(url, new ObjectId(request().username())));
	}
}
