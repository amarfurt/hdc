package utils.collections;

import java.util.HashSet;
import java.util.Set;

public class CollectionConversion {

	public static <T> Set<T> toSet(Iterable<T> iterable) {
		Set<T> set = new HashSet<T>();
		for (T element : iterable) {
			set.add(element);
		}
		return set;
	}

}
