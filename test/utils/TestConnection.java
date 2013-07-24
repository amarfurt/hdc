package utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class TestConnection {

	public static String CONFIG = "conf/test.conf";

	private MongoClient mongoClient;

	public TestConnection() throws IOException {
		Properties properties = new Properties();
		try (FileReader fr = new FileReader(CONFIG)) {
			properties.load(fr);
			String host = properties.getProperty("mongo.host");
			int port = Integer.parseInt(properties.getProperty("mongo.port"));
			mongoClient = new MongoClient(host, port);
		}
	}

	public DB connect() throws IOException {
		Properties properties = new Properties();
		try (FileReader fr = new FileReader(CONFIG)) {
			properties.load(fr);
			String database = properties.getProperty("mongo.database");
			return mongoClient.getDB(database);
		}
	}

	public void close() {
		mongoClient.close();
	}

}
