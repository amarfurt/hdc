package controllers.apps;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import models.App;
import models.LargeRecord;
import models.ModelException;
import models.Record;
import models.User;

import org.bson.types.ObjectId;

import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import utils.DateTimeUtils;
import utils.auth.AppToken;
import utils.collections.ChainedMap;
import utils.collections.ChainedSet;
import utils.db.FileStorage;
import utils.db.FileStorage.FileData;
import utils.json.JsonValidation;
import utils.json.JsonValidation.JsonValidationException;

import com.fasterxml.jackson.databind.JsonNode;

//Not secured, accessible from app server
public class GenomeDataConverter extends Controller {

	private static final String dateTag = "generated by 23andMe at:";
	private static final String buildTag = "reference human assembly build";
	private static final String buildUrlTag = "# http://www.ncbi.nlm.nih.gov";

	public static Result checkPreflight() {
		// allow cross-origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);
		response().setHeader("Access-Control-Allow-Methods", "POST");
		response().setHeader("Access-Control-Allow-Headers", "Content-Type");
		return ok();
	}

	/**
	 * Gets all the files of the user.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result getFiles() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}

		// perform checks whether the auth token is valid and issued by the 23andMe Converter app
		String errorMessage = checkAuthToken(appToken);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}

		// get the names of all files from the current user
		Map<String, Object> properties = new ChainedMap<String, Object>().put("owner", appToken.userId).put("data.type", "file").get();
		Set<String> fields = new ChainedSet<String>().add("name").get();
		List<Record> records;
		try {
			records = new ArrayList<Record>(Record.getAll(properties, fields));
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		Collections.sort(records);
		return ok(Json.toJson(records));
	}

	/**
	 * Check authenticity of request, i.e. whether it is performed by the 23andMe Converter app.
	 * @return An error message if a validity check failed, null otherwise.
	 */
	private static String checkAuthToken(AppToken appToken) {
		// check whether the app is the 23andMe Converter app
		Map<String, Object> appProperties = new ChainedMap<String, Object>().put("_id", appToken.appId)
				.put("filename", "23andme-converter").get();
		try {
			if (!App.exists(appProperties)) {
				return "Invalid authToken.";
			}
		} catch (ModelException e) {
			return e.getMessage();
		}

		// check whether there exists a user with the app installed
		Map<String, ObjectId> userProperties = new ChainedMap<String, ObjectId>().put("_id", appToken.userId).put("apps", appToken.appId)
				.get();
		try {
			if (!User.exists(userProperties)) {
				return "Invalid authToken.";
			}
		} catch (ModelException e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Convert a 23andMe file to the HDC format.
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public static Result convert() {
		// allow cross origin request from app server
		String appServer = Play.application().configuration().getString("apps.server");
		response().setHeader("Access-Control-Allow-Origin", "https://" + appServer);

		// check whether the request is complete
		JsonNode json = request().body().asJson();
		try {
			JsonValidation.validate(json, "authToken", "id", "name", "description");
		} catch (JsonValidationException e) {
			return badRequest(e.getMessage());
		}

		// decrypt authToken
		AppToken appToken = AppToken.decrypt(json.get("authToken").asText());
		if (appToken == null) {
			return badRequest("Invalid authToken.");
		}

		// perform checks whether the auth token is valid and issued by the 23andMe Converter app
		String errorMessage = checkAuthToken(appToken);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}

		// parse the file
		FileData fileData = FileStorage.retrieve(new ObjectId(json.get("id").asText()));
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		errorMessage = parseInput(fileData.inputStream, map);
		if (errorMessage != null) {
			return badRequest(errorMessage);
		}

		// create record
		Record record = new Record();
		record._id = new ObjectId();
		record.app = appToken.appId;
		record.created = DateTimeUtils.now();
		record.creator = appToken.userId;
		record.owner = appToken.userId;
		record.name = json.get("name").asText();
		record.description = json.get("description").asText();
		record.data = null;
		try {
			LargeRecord.add(record, map);
		} catch (ModelException e) {
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Parse the input stream and extract meta data and content.
	 */
	private static String parseInput(InputStream inputStream, TreeMap<String, Object> map) {
		try (Scanner scanner = new Scanner(inputStream)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("#")) {
					// this line is a comment; extract important information
					if (line.contains(dateTag)) {
						String date = line.split(dateTag, 2)[1].trim();
						map.put("date", date);
					} else if (line.contains(buildTag)) {
						int index = line.lastIndexOf(buildTag) + buildTag.length() + 1;
						String build = "";
						char character = line.charAt(index);
						while (Character.isDigit(character)) {
							build += character;
							character = line.charAt(++index);
						}
						map.put("build", build);
					} else if (line.startsWith(buildUrlTag)) {
						String buildUrl = line.substring(2).trim();
						map.put("buildUrl", buildUrl);
					}
				} else {
					// 23 and me files are tab-separated with exactly 4 columns
					String[] split = line.split("\t");
					if (split.length != 4) {
						// skip this line
						continue;
					} else {
						// the columns are (in order): rsid, chromosome, position, genotype
						String rsid = split[0]; // rs id or internal id (prefixed with 'i')
						String chromosome = split[1]; // can be 1-22, X, Y, MT
						int position = Integer.parseInt(split[2]); // position on the human reference genome
						String genotype = split[3]; // a pair of letters from A, C, G, T
						map.put(rsid, new Object[] { chromosome, position, genotype });
					}
				}
			}
		}

		// check whether data was found
		if (map.isEmpty()) {
			return "No data found. This is probably not a standard 23andMe file.";
		}
		return null;
	}

}
