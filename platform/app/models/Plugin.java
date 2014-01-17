package models;

import org.bson.types.ObjectId;

public abstract class Plugin extends Model {

	public ObjectId creator;
	public String name;
	public String description;
	public boolean spotlighted;

}
