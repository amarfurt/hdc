package models;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Set;

import org.bson.types.ObjectId;

import utils.PasswordHash;
import utils.collections.ChainedMap;
import utils.search.Search;
import utils.search.Search.Type;
import utils.search.SearchException;

public class User extends Model implements Comparable<User> {

	private static final String collection = "users";

	public String email; // must be unique
	public String name;
	public String password;
	public Map<String, Set<ObjectId>> visible; // map from users (DBObject requires string) to their shared records
	public Set<ObjectId> apps; // installed apps
	public Map<String, Map<String, String>> tokens; // map from apps to app details
	public Set<ObjectId> visualizations; // installed visualizations
	public Map<String, Set<ObjectId>> messages; // keys (folders) are: inbox, archive, trash
	public String login; // timestamp of last login
	public Set<ObjectId> news; // visible news items
	public Set<ObjectId> pushed; // records pushed by apps (since last login)
	public Set<ObjectId> shared; // records shared by users (since last login)
	
	public String resettoken; // token to reset password
	public long resettokenTs; // timestamp of password reset token

	@Override
	public int compareTo(User other) {
		if (this.name != null && other.name != null) {
			return this.name.compareTo(other.name);
		} else {
			return super.compareTo(other);
		}
	}

	public static boolean exists(Map<String, ? extends Object> properties) throws ModelException {
		return Model.exists(collection, properties);
	}

	public static User get(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.get(User.class, collection, properties, fields);
	}

	public static Set<User> getAll(Map<String, ? extends Object> properties, Set<String> fields) throws ModelException {
		return Model.getAll(User.class, collection, properties, fields);
	}

	public static void set(ObjectId userId, String field, Object value) throws ModelException {
		Model.set(collection, userId, field, value);
	}

	public static void add(User user) throws ModelException {
		Model.insert(collection, user);

		// add to search index (email is document's content, so that it is searchable as well)
		try {
			Search.add(Type.USER, user._id, user.name, user.email);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId userId) throws ModelException {
		// remove from search index
		Search.delete(Type.USER, userId);

		// TODO remove all the user's messages, records, spaces, circles, apps (if published, ask whether to leave it in
		// the marketplace), ...
		Model.delete(collection, new ChainedMap<String, ObjectId>().put("_id", userId).get());
	}

	/**
	 * Authenticate login data.
	 */
	public static boolean authenticationValid(String givenPassword, String savedPassword) throws ModelException {
		try {
			return PasswordHash.validatePassword(givenPassword, savedPassword);
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);
		} catch (InvalidKeySpecException e) {
			throw new ModelException(e);
		}
	}

	public static String encrypt(String password) throws ModelException {
		try {
			return PasswordHash.createHash(password);
		} catch (NoSuchAlgorithmException e) {
			throw new ModelException(e);
		} catch (InvalidKeySpecException e) {
			throw new ModelException(e);
		}
	}

}
