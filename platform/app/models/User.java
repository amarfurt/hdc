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
	public Map<String, Set<ObjectId>> visible; // map from users (DBObjet requires string) to their shared records
	public Set<ObjectId> apps; // installed apps
	public Set<ObjectId> visualizations; // installed visualizations

	@Override
	public int compareTo(User o) {
		return this.name.compareTo(o.name);
	}

	public static boolean exists(Map<String, ? extends Object> properties) {
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
			Search.addPublic(Type.USER, user._id, user.name, user.email);
		} catch (SearchException e) {
			throw new ModelException(e);
		}
	}

	public static void delete(ObjectId userId) throws ModelException {
		// remove from search index
		Search.deletePublic(Type.USER, userId);

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
