package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import models.Model;

import org.bson.types.ObjectId;

public class ListOperations {

	public static <T extends Model> List<T> removeFromList(List<T> list, ObjectId... ids) {
		return removeFromList(list, new HashSet<ObjectId>(Arrays.asList(ids)));
	}

	public static <T extends Model> List<T> removeFromList(List<T> list, Set<ObjectId> ids) {
		List<T> newList = new ArrayList<T>();
		newList.addAll(list);
		Iterator<T> iterator = newList.iterator();
		while (iterator.hasNext()) {
			if (ids.contains(iterator.next()._id)) {
				iterator.remove();
			}
		}
		return newList;
	}

}
