package controllers.forms;

import controllers.Spaces;

public class SpaceForm {

	public String name;
	public String visualization;
	public String spaceId; // id of new space in case of success

	public String validate() {
		String result = Spaces.validateSpace(name, visualization);
		if (result.startsWith("ObjectId:")) {
			spaceId = result.substring(9);
			return null;
		} else {
			return result;
		}
	}

}
