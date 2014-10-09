package utils.auth;

import java.util.Map;

import org.bson.types.ObjectId;

import play.libs.Crypto;
import play.libs.Json;
import utils.collections.ChainedMap;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Authorization token for apps to push records to a user's repository.
 */
public class AppToken {

	public ObjectId appId;
	public ObjectId userId;

	public AppToken(ObjectId appId, ObjectId userId) {
		this.appId = appId;
		this.userId = userId;
	}

	public String encrypt() {
		Map<String, String> map = new ChainedMap<String, String>().put("appId", appId.toString()).put("userId", userId.toString()).get();
		String json = Json.stringify(Json.toJson(map));
		return Crypto.encryptAES(json);
	}

	/**
	 * The secret passed here can be an arbitrary string, so check all possible exceptions.
	 */
	public static AppToken decrypt(String unsafeSecret) {
		try {
			// decryptAES can throw DecoderException, but there is no way to catch it; catch all exceptions for now...
			String plaintext = Crypto.decryptAES(unsafeSecret);
			JsonNode json = Json.parse(plaintext);
			ObjectId appId = new ObjectId(json.get("appId").asText());
			ObjectId userId = new ObjectId(json.get("userId").asText());
			return new AppToken(appId, userId);
		} catch (Exception e) {
			return null;
		}
	}
}
