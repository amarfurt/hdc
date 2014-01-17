package utils;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import utils.json.CustomObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBList;

public class ObjectIdSerializerTest {

	@Before
	public void setUp() {
		// Set custom object mapper for Json
		Json.setObjectMapper(new CustomObjectMapper());
	}

	@Test
	public void serializeObjectId() {
		ObjectId id = new ObjectId();
		JsonNode json = Json.toJson(id);
		assertEquals(id.toString(), json.asText());
	}

	@Test
	public void serializeModel() {
		// create model
		TestModel model = new TestModel();
		model.id = new ObjectId();
		model.name = "Test model";
		model.friends = new BasicDBList();
		ObjectId[] friendIds = { new ObjectId(), new ObjectId(), new ObjectId() };
		for (ObjectId friendId : friendIds) {
			model.friends.add(friendId);
		}

		// serialize
		JsonNode json = Json.toJson(model);
		assertEquals(model.id.toString(), json.get("id").asText());
		assertEquals(model.name, json.get("name").asText());
		assertEquals(model.friends.size(), json.get("friends").size());
		Iterator<JsonNode> iterator = json.get("friends").iterator();
		for (int i = 0; i < model.friends.size(); i++) {
			assertEquals(model.friends.get(i).toString(), iterator.next().asText());
		}
	}

	public static class TestModel {
		public ObjectId id;
		public String name;
		public BasicDBList friends;
	}

}
