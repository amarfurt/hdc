package models;

import org.bson.types.ObjectId;

public abstract class Model {

	public ObjectId _id;

	@Override
	public boolean equals(Object other) {
		if (this.getClass().equals(other.getClass())) {
			Model otherModel = (Model) other;
			return _id.equals(otherModel._id);
		}
		return false;
	}

}
