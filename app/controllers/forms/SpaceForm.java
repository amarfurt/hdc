package controllers.forms;

import controllers.Spaces;

public class SpaceForm {

	public String name;
	public String visualization;

	public String validate() {
		return Spaces.validateSpace(name, visualization);
	}

}
