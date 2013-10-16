package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;

import utils.Connection;
import utils.ModelConversion;
import utils.PasswordHash;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class User extends SearchableModel implements Comparable<User> {

	private static final String collection = "users";

	public String email; // must be unique
	public String name;
	public String password;

	@Override
	public int compareTo(User o) {
		return this.name.compareTo(o.name);
	}

	public static String getCollection() {
		return collection;
	}
	
	public static ObjectId getId(String email) {
		DBObject query = new BasicDBObject("email", email);
		DBObject projection = new BasicDBObject("_id", 1);
		return (ObjectId) Connection.getCollection(collection).findOne(query, projection).get("_id");
	}

	public static String getName(ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject projection = new BasicDBObject("name", 1);
		return (String) Connection.getCollection(collection).findOne(query, projection).get("name");
	}

	public static User find(ObjectId userId) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		DBObject query = new BasicDBObject("_id", userId);
		DBObject result = Connection.getCollection(collection).findOne(query);
		if (result != null) {
			return ModelConversion.mapToModel(User.class, result.toMap());
		} else {
			return null;
		}
	}

	public static List<User> findAll(int limit) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		List<User> userList = new ArrayList<User>();
		DBObject query = new BasicDBObject();
		DBCursor result = Connection.getCollection(collection).find(query).limit(limit);
		while (result.hasNext()) {
			userList.add(ModelConversion.mapToModel(User.class, result.next().toMap()));
		}
		Collections.sort(userList);
		return userList;
	}

	public static boolean authenticationValid(String email, String password) throws IllegalArgumentException,
			IllegalAccessException, InstantiationException, NoSuchAlgorithmException, InvalidKeySpecException {
		if (!userExists(email)) {
			return false;
		}
		String storedPassword = getPassword(email);
		return PasswordHash.validatePassword(password, storedPassword);
	}

	public static String add(User newUser) throws IllegalArgumentException, IllegalAccessException,
			NoSuchAlgorithmException, InvalidKeySpecException, InstantiationException {
		if (userExists(newUser.email)) {
			return "A user with this email address already exists.";
		}
		newUser.password = PasswordHash.createHash(newUser.password);
		newUser.tags = new BasicDBList();
		newUser.tags.add(newUser.email.toLowerCase());
		for (String namePart : newUser.name.toLowerCase().split(" ")) {
			newUser.tags.add(namePart);
		}
		DBObject insert = new BasicDBObject(ModelConversion.modelToMap(newUser));
		WriteResult result = Connection.getCollection(collection).insert(insert);
		newUser._id = (ObjectId) insert.get("_id");
		String errorMessage = result.getLastError().getErrorMessage();
		if (errorMessage != null) {
			return errorMessage;
		}
		
		// also set up installed entry
		return Installed.addUser(newUser._id);
	}

	public static String remove(ObjectId userId) {
		if (!userExists(userId)) {
			return "No user with this id exists.";
		}
		// TODO remove all the user's messages, records, spaces, circles, apps (if published, ask whether to leave it in
		// the marketplace), ...
		DBObject query = new BasicDBObject("_id", userId);
		WriteResult result = Connection.getCollection(collection).remove(query);
		return result.getLastError().getErrorMessage();
	}

	private static boolean userExists(ObjectId userId) {
		DBObject query = new BasicDBObject("_id", userId);
		return (Connection.getCollection(collection).findOne(query) != null);
	}
	
	public static boolean userExists(String email) {
		DBObject query = new BasicDBObject("email", email);
		return (Connection.getCollection(collection).findOne(query) != null);
	}

	private static String getPassword(String email) {
		DBObject query = new BasicDBObject("email", email);
		DBObject projection = new BasicDBObject("password", 1);
		return (String) Connection.getCollection(collection).findOne(query, projection).get("password");
	}

	public static boolean isPerson(ObjectId userId) {
		// TODO security check before casting to person?
		// requirement for record owners?
		return true;
	}

}
