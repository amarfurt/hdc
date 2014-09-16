package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token for visualizations to access a user's records.
 */
public class AuthToken {

	public ObjectId spaceId;
	public ObjectId userId;

	public AuthToken(ObjectId spaceId, ObjectId userId) {
		this.spaceId = spaceId;
		this.userId = userId;
	}

	public String encrypt() {
		Map<String, String> map = new ChainedMap<String, String>().put("spaceId", spaceId.toString()).put("userId", userId.toString())
				.get();
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static AuthToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId spaceId = new ObjectId(json.get("spaceId").asText());
			ObjectId userId = new ObjectId(json.get("userId").asText());
			return new AuthToken(spaceId, userId);
		} catch (Exception e) {
			return null;
		}
	}
}
