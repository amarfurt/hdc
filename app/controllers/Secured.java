package controllers;

import models.Circle;
import models.Record;
import models.Space;

import org.bson.types.ObjectId;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator {

	@Override
	public String getUsername(Context ctx) {
		return ctx.session().get("email");
	}

	@Override
	public Result onUnauthorized(Context ctx) {
		return redirect(routes.Application.welcome());
	}

	public static boolean isCreatorOrOwnerOfRecord(ObjectId recordId) {
		return Record.isCreatorOrOwner(recordId, Context.current().request().username());
	}

	public static boolean isOwnerOfSpace(ObjectId spaceId) {
		return Space.isOwner(spaceId, Context.current().request().username());
	}

	public static boolean isOwnerOfCircle(ObjectId circleId) {
		return Circle.isOwner(circleId, Context.current().request().username());
	}

}
