package controllers;

import java.net.UnknownHostException;

import models.User;

public class Login {

	public String email;
	public String password;

	public String validate() {
		try {
			if (User.authenticate(email, password) == null) {
				return "Invalid user or password";
			}
		// multi-catch doesn't seem to work...
		} catch (UnknownHostException e) {
			return "Server error: " + e.getMessage();
		} catch (IllegalArgumentException e) {
			return "Server error: " + e.getMessage();
		} catch (IllegalAccessException e) {
			return "Server error: " + e.getMessage();
		} catch (InstantiationException e) {
			return "Server error: " + e.getMessage();
		}
		return null;
	}
}
