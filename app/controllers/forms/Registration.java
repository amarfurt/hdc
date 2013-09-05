package controllers.forms;

import models.User;

public class Registration {
	
	public String email;
	public String firstName;
	public String lastName;
	public String password;
	
	public String validate() {
		try {
			if (User.find(email) != null) {
				return "A user with this email address already exists.";
			} else if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty()){
				return "Please fill out all fields.";
			} else {
				return null;
			}
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		} catch (IllegalAccessException e) {
			return e.getMessage();
		} catch (InstantiationException e) {
			return e.getMessage();
		}
	}

}
