package models;

import java.net.UnknownHostException;

import utils.ModelConversion;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class User {

	public String email; // serves as id
	public String name;
	public String password;

	public static User find(String email) throws UnknownHostException, IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject cur = null;
		DBCursor cursor = Connection.getCursor("users");
		while (cursor.hasNext()) {
			cur = cursor.next();
			if (cur.get("email").equals(email)) {
				return ModelConversion.mapToModel(User.class, cur.toMap());
			}
		}
		return null;
	}

	public static User authenticate(String email, String password) throws UnknownHostException, IllegalArgumentException,
			IllegalAccessException, InstantiationException {
		User user = find(email);
		if (user != null && user.password.equals(password)) {
			return user;
		} else {
			return null;
		}
	}

}
