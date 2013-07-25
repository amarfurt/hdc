package controllers;

import models.Circle;

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

	public static boolean isOwnerOf(ObjectId circleId) {
		return Circle.isOwner(circleId, Context.current().request().username());
	}

}
