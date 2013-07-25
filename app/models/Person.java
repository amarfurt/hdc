package models;

import java.net.UnknownHostException;

public class Person extends User {

	public String birthday;

	public static Person find(String email) throws UnknownHostException, IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		// TODO how to do model conversion to person?
		// Maybe save type (e.g. "models.Person") into database?
		return (Person) User.find(email);
	}

}
