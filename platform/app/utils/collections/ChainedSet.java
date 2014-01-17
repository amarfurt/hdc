package utils.collections;

import java.util.HashSet;
import java.util.Set;

public class ChainedSet<T> {

	private Set<T> set;

	public ChainedSet() {
		set = new HashSet<T>();
	}

	public ChainedSet<T> add(T element) {
		set.add(element);
		return this;
	}

	public Set<T> get() {
		return set;
	}

}
