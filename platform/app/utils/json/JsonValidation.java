package utils.json;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonValidation {

	public static void validate(JsonNode json, String... requiredFields) throws JsonValidationException {
		if (json == null) {
			throw new JsonValidationException("No json found.");
		} else {
			for (String requiredField : requiredFields) {
				if (!json.has(requiredField)) {
					throw new JsonValidationException("Request parameter '" + requiredField + "' not found.");
				}
			}
		}
	}

	public static class JsonValidationException extends Exception {

		private static final long serialVersionUID = 1L;

		public JsonValidationException(String message) {
			super(message);
		}
	}

}
