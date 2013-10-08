package models;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;

public abstract class Model {

	public ObjectId _id;
	public BasicDBList tags;

}
