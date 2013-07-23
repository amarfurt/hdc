package models;

public class User {

	public String email;
	public String name;
	public String password;

	public User() {
		// empty constructor
	}

	public User(String email, String name, String password) {
		this.email = email;
		this.name = name;
		this.password = password;
	}
	
	public static User find(String id) {
		User user = new User();
		user.email = "andreas.marfurt@healthbank.ch";
		user.name = "Andreas Marfurt";
		user.password = "secreta";
		return user;
	}
	
	public static User authenticate(String email, String password) {
        User user = find(email);
        if (user.password.equals(password)) {
        	return user;
        } else {
        	return null;
        }
    }

}
