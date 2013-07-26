package models;

import java.net.UnknownHostException;

import utils.ModelConversion;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import controllers.database.Connection;

public class User {
	
	private static final String collection = "users";

	public String email; // serves as id
	public String name;
	public String password;

	public static User find(String email) throws UnknownHostException, IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("email", email);
		DBObject result = Connection.getCollection(collection).findOne(query);
		if (result != null) {
			return ModelConversion.mapToModel(User.class, result.toMap());
		} else {
			return null;
		}
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
