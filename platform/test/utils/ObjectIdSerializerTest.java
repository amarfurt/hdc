package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import utils.json.CustomObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;

public class ObjectIdSerializerTest {

	@Before
	public void setUp() {
		// Set custom object mapper for Json
		Json.setObjectMapper(new CustomObjectMapper());
	}

	@Test
	public void serializeObjectId() {
		ObjectId id = new ObjectId();
		// model series of operations when getting a request from JS
		JsonNode json = Json.parse(Json.stringify(Json.toJson(id)));
		assertEquals(id.toString(), json.get("$oid").asText());
	}

	@Test
	public void serializeModel() {
		// create model
		TestModel model = new TestModel();
		model.id = new ObjectId();
		model.name = "Test model";
		model.friends = new HashSet<ObjectId>();
		ObjectId[] friendIds = { new ObjectId(), new ObjectId(), new ObjectId() };
		for (ObjectId friendId : friendIds) {
			model.friends.add(friendId);
		}

		// serialize
		JsonNode json = Json.parse(Json.stringify(Json.toJson(model)));
		;
		assertEquals(model.id.toString(), json.get("id").get("$oid").asText());
		assertEquals(model.name, json.get("name").asText());
		assertEquals(model.friends.size(), json.get("friends").size());
		Iterator<JsonNode> iterator = json.get("friends").iterator();
		for (int i = 0; i < model.friends.size(); i++) {
			assertTrue(model.friends.contains(new ObjectId(iterator.next().get("$oid").asText())));
		}
	}

	public static class TestModel {
		public ObjectId id;
		public String name;
		public Set<ObjectId> friends;
	}

}
