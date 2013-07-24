package models;

import java.net.UnknownHostException;

import utils.ModelConversion;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Access;

public class User {

	public String email;
	public String name;
	public String password;

	public User() {
		// empty constructor
	}

	public User(String email, String name, String password) {
		this.email = email;
		this.name = name;
		this.password = password;
	}

	public static User find(String email) throws UnknownHostException, IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		User user = null;
		DBObject cur = null;
		DBCursor cursor = Access.getUsers();
		while (cursor.hasNext()) {
			cur = cursor.next();
			if (cur.get("email").equals(email))
				break;
		}
		if (cur != null) {
			// User found, create object
			user = ModelConversion.mapToModel(User.class, cur.toMap());
		}
		return user;
	}

	public static User authenticate(String email, String password) throws UnknownHostException, IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		User user = find(email);
		if (user.password.equals(password)) {
			return user;
		} else {
			return null;
		}
	}

}
