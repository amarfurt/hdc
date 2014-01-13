package utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import models.Model;

public class ModelConversion {

	/**
	 * Builds a map from the fields of a model.
	 */
	public static <T extends Model> Map<String, Object> modelToMap(T modelObject) throws ConversionException {
		Map<String, Object> map = new HashMap<String, Object>();
		for (Field field : modelObject.getClass().getFields()) {
			try {
				map.put(field.getName(), field.get(modelObject));
			} catch (IllegalArgumentException e) {
				throw new ConversionException(e);
			} catch (IllegalAccessException e) {
				throw new ConversionException(e);
			}
		}
		return map;
	}

	/**
	 * Converts a map (returned by the database) to an instance of a model.
	 */
	public static <T extends Model> T mapToModel(Class<T> modelClass, Map modelData) throws ConversionException {
		T modelObject;
		try {
			modelObject = modelClass.newInstance();
		} catch (InstantiationException e) {
			throw new ConversionException(e);
		} catch (IllegalAccessException e) {
			throw new ConversionException(e);
		}
		for (Field field : modelClass.getFields()) {
			if (modelData.containsKey(field.getName())) {
				try {
					field.set(modelObject, modelData.get(field.getName()));
				} catch (IllegalArgumentException e) {
					throw new ConversionException(e);
				} catch (IllegalAccessException e) {
					throw new ConversionException(e);
				}
			}
		}
		return modelObject;
	}

	public static class ConversionException extends Exception {

		private static final long serialVersionUID = 1L;

		public ConversionException(Throwable cause) {
			super(cause);
		}

	}

}
