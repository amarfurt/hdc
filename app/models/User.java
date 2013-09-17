package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utils.ModelConversion;
import utils.PasswordHash;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

import controllers.database.Connection;

public class User implements Comparable<User> {

	private static final String collection = "users";

	public String email; // serves as id
	public String name;
	public String password;
	public BasicDBList tags;

	@Override
	public int compareTo(User o) {
		return this.name.compareTo(o.name);
	}

	public static String getCollection() {
		return collection;
	}

	public static String getName(String email) {
		DBObject query = new BasicDBObject("email", email);
		DBObject projection = new BasicDBObject("name", 1);
		return (String) Connection.getCollection(collection).findOne(query, projection).get("name");
	}

	public static User find(String email) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("email", email);
		DBObject result = Connection.getCollection(collection).findOne(query);
		if (result != null) {
			return ModelConversion.mapToModel(User.class, result.toMap());
		} else {
			return null;
		}
	}

	public static List<User> findAllExcept(String... users) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<User> userList = new ArrayList<User>();
		DBObject query = new BasicDBObject("email", new BasicDBObject("$nin", users));
		DBCursor result = Connection.getCollection(collection).find(query);
		while (result.hasNext()) {
			userList.add(ModelConversion.mapToModel(User.class, result.next().toMap()));
		}
		Collections.sort(userList);
		return userList;
	}

	public static User authenticate(String email, String password) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		User user = find(email);
		if (user != null && PasswordHash.validatePassword(password, user.password)) {
			return user;
		} else {
			return null;
		}
	}

	public static String add(User newUser) throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		if (userWithSameEmailExists(newUser.email)) {
			return "A user with this email address already exists.";
		}
		newUser.password = PasswordHash.createHash(newUser.password);
		newUser.tags = new BasicDBList();
		newUser.tags.add(newUser.email.toLowerCase());
		for (String namePart : newUser.name.toLowerCase().split(" ")) {
			newUser.tags.add(namePart);
		}
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(User.class, newUser));
		WriteResult result = Connection.getCollection(collection).insert(insert);
		return result.getLastError().getErrorMessage();
	}

	public static String remove(String email) {
		if (!userWithSameEmailExists(email)) {
			return "No user with this email address exists.";
		}
		// TODO remove all the user's messages, records, spaces, circles, apps (if published, ask whether to leave it in
		// the marketplace), ...
		DBObject query = new BasicDBObject("email", email);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

	private static boolean userWithSameEmailExists(String email) {
		DBObject query = new BasicDBObject("email", email);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	public static boolean isPerson(String email) {
		// TODO security check before casting to person?
		// requirement for record owners?
		return true;
	}

}
