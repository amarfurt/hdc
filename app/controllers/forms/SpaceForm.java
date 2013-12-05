package controllers.forms;

import models.Space;
import controllers.Application;

public class SpaceForm {

	public String name;
	public String visualization; // cannot pass ObjectId from HTML

	public String validate() {
		if (name.isEmpty()) {
			return "Please provide a name for your new space.";
		} else if (Space.exists(Application.getCurrentUserId(), name)) {
			return "A space with this name already exists.";
		} else if (visualization.isEmpty()) {
			return "Please select a visualization or install one in case you have none.";
		}
		return null;
	}

}
