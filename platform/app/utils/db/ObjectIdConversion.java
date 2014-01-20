package utils.db;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;

public class ObjectIdConversion {

	public static Set<ObjectId> toObjectIds(Set<String> strings) {
		Set<ObjectId> set = new HashSet<ObjectId>();
		for (String s : strings) {
			set.add(new ObjectId(s));
		}
		return set;
	}

	public static Set<ObjectId> castToObjectIds(Set<Object> objects) {
		Set<ObjectId> set = new HashSet<ObjectId>();
		for (Object o : objects) {
			set.add((ObjectId) o);
		}
		return set;
	}

	public static Set<String> toStrings(Set<ObjectId> objectIds) {
		Set<String> set = new HashSet<String>();
		for (ObjectId oid : objectIds) {
			set.add(oid.toString());
		}
		return set;
	}

}
