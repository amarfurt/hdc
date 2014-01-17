package utils.json;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonExtraction {

	public static Set<String> extractSet(Iterable<JsonNode> json) {
		Set<String> set = new HashSet<String>();
		for (JsonNode jsonNode : json) {
			set.add(jsonNode.asText());
		}
		return set;
	}

	public static Map<String, Object> extractMap(JsonNode json) {
		Map<String, Object> map = new HashMap<String, Object>();
		Iterator<Entry<String, JsonNode>> iterator = json.fields();
		while (iterator.hasNext()) {
			Entry<String, JsonNode> cur = iterator.next();
			if (cur.getValue().isObject()) {
				map.put(cur.getKey(), extractMap(cur.getValue()));
			} else if (cur.getValue().isArray()) {
				map.put(cur.getKey(), extractSet(cur.getValue()));
			} else {
				map.put(cur.getKey(), cur.getValue().asText());
			}
		}
		return map;
	}

}
