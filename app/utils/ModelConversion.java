package utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import models.Model;

public class ModelConversion {

	public static <T extends Model> Map<String, Object> modelToMap(T modelObject) throws IllegalArgumentException,
			IllegalAccessException {
		Map<String, Object> map = new HashMap<String, Object>();
		for (Field field : modelObject.getClass().getFields()) {
			map.put(field.getName(), field.get(modelObject));
		}
		return map;
	}

	public static <T extends Model> T mapToModel(Class<T> modelClass, Map modelData) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		T modelObject = modelClass.newInstance();
		for (Field field : modelClass.getFields()) {
			field.set(modelObject, modelData.get(field.getName()));
		}
		return modelObject;
	}

}
