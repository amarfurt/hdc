package utils.collections;

import java.util.HashMap;
import java.util.Map;

public class ChainedMap<K, V> {

	private Map<K, V> map;

	public ChainedMap() {
		map = new HashMap<K, V>();
	}

	public ChainedMap<K, V> put(K key, V value) {
		map.put(key, value);
		return this;
	}

	public Map<K, V> get() {
		return map;
	}

}
