package controllers.database;

import java.net.UnknownHostException;

import play.Play;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Connection {

	private MongoClient mongoClient;

	public Connection() throws UnknownHostException {
		String host = Play.application().configuration().getString("mongo.host");
		int port = Play.application().configuration().getInt("mongo.port");
		mongoClient = new MongoClient(host, port);
	}

	/**
	 * Connects to the main database 'healthbank'.
	 */
	public DB connect() {
		String database = Play.application().configuration().getString("mongo.database");
		return mongoClient.getDB(database);
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		mongoClient.close();
	}

}
