package controllers.forms;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import models.User;
import utils.ModelConversion.ConversionException;

public class Login {

	public String email;
	public String password;

	public String validate() {
		try {
			if (email.isEmpty() || password.isEmpty()) {
				return "Please provide an email address and a password.";
			} else if (!User.authenticationValid(email, password)) {
				return "Invalid user or password.";
			} else {
				return null;
			}
		} catch (ConversionException e) {
			return "Server error: " + e.getMessage();
		} catch (NoSuchAlgorithmException e) {
			return "Server error: " + e.getMessage();
		} catch (InvalidKeySpecException e) {
			return "Server error: " + e.getMessage();
		}
	}
}