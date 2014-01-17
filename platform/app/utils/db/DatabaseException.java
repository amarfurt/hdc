package utils.db;

public class DatabaseException extends Exception {

	private static final long serialVersionUID = 1L;

	public DatabaseException(String message) {
		super(message);
	}

	public DatabaseException(Throwable cause) {
		super(cause);
	}

	public static void throwIfPresent(String potentialError) throws DatabaseException {
		if (potentialError != null) {
			throw new DatabaseException(potentialError);
		}
	}

}
