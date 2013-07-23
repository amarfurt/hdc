package models;

public class Person extends User {

	public String birthday;

	public Person() {
		// empty constructor
	}
	
	public Person(String email, String name, String password, String birthday) {
		super(email, name, password);
		this.birthday = birthday;
	}
	
	public static Person find(String id) {
		return new Person();
	}

}
