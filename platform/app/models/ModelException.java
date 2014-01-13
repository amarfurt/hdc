package models;

public class ModelException extends Exception {

	private static final long serialVersionUID = 1L;

	public ModelException(String message) {
		super(message);
	}

	public ModelException(Throwable cause) {
		super(cause);
	}

	public static void throwIfPresent(String potentialError) throws ModelException {
		if (potentialError != null) {
			throw new ModelException(potentialError);
		}
	}

}
