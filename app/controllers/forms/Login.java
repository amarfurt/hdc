package controllers.forms;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import models.User;

public class Login {

	public String email;
	public String password;

	public String validate() {
		try {
			if (!User.authenticationValid(email, password)) {
				return "Invalid user or password.";
			} else {
				return null;
			}
		// multi-catch doesn't seem to work...
		} catch (IllegalArgumentException e) {
			return "Server error: " + e.getMessage();
		} catch (IllegalAccessException e) {
			return "Server error: " + e.getMessage();
		} catch (InstantiationException e) {
			return "Server error: " + e.getMessage();
		} catch (NoSuchAlgorithmException e) {
			return "Server error: " + e.getMessage();
		} catch (InvalidKeySpecException e) {
			return "Server error: " + e.getMessage();
		}
	}
}