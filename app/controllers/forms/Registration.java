package controllers.forms;

import models.User;

public class Registration {

	public String email;
	public String firstName;
	public String lastName;
	public String password;

	public String validate() {
		if (User.userExists(email)) {
			return "A user with this email address already exists.";
		} else if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty()) {
			return "Please fill in all fields.";
		} else {
			return null;
		}
	}

}
