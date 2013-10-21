package controllers.visualizations;

import org.bson.types.ObjectId;

import play.cache.Cache;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.visualizations.runaggregator;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.Secured;

@Security.Authenticated(Secured.class)
public class RunAggregator extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	public static Result load() {
		// check whether the request is complete
		JsonNode json = request().body().asJson();
		String requestComplete = Visualizations.requestComplete(json);
		if (requestComplete != null) {
			return badRequest(requestComplete);
		}

		double distance = 0;
		double time = 0;
		for (JsonNode cur : json.get("records")) {
			if (cur.has("data")) {
				// assume the format "... {distance}[ ]km in {time}[ ]h ..."
				String data = cur.get("data").asText().toLowerCase();
				if (!data.matches(".+km in .+h.*")) {
					continue;
				}
				String curDistance = data.substring(0, data.lastIndexOf("km")).trim();
				curDistance = curDistance.substring(curDistance.lastIndexOf(" ") + 1);
				distance += Double.parseDouble(curDistance);

				String curTime = data.substring(0, data.lastIndexOf("h")).trim();
				curTime = curTime.substring(curTime.lastIndexOf(" ") + 1);
				time += Double.parseDouble(curTime);
			}
		}
		double speed = 0;
		if (time > 0) {
			speed = distance / time;
		}
		String distanceString = String.format("%.2f", distance);
		String timeString = String.format("%.2f", time);
		String speedString = String.format("%.2f", speed);
		Result response = ok(runaggregator.render(distanceString, timeString, speedString));

		// cache the response and return the url to retrieve it
		ObjectId requestId = new ObjectId();
		Cache.set(requestId.toString() + ":" + request().username(), response);
		return ok(routes.Visualizations.show(requestId.toString()).url());
	}

}
