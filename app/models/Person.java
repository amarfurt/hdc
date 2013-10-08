package models;

import org.bson.types.ObjectId;

public class Person extends User {

	public String birthday;

	public static Person find(ObjectId person) throws IllegalArgumentException, IllegalAccessException,
			InstantiationException {
		// TODO how to do model conversion to person?
		// Maybe save type (e.g. "models.Person") into database?
		return (Person) User.find(person);
	}

}
