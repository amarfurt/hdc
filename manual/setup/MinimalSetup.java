package setup;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.ModelException;
import models.User;
import models.Visualization;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;

import utils.db.Database;
import utils.search.Search;

/**
 * Minimal setup that is necessary to start a fresh Health Data Cooperative platform.
 */
public class MinimalSetup {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting to create minimal setup for Health Data Cooperative platform.");

		// connecting
		System.out.print("Connecting to MongoDB...");
		start(fakeApplication(fakeGlobal()));
		Database.connect();
		System.out.println("done.");
		System.out.print("Connecting to ElasticSearch...");
		Search.connect();
		System.out.println("done.");

		// initializing
		System.out.print("Setting up MongoDB...");
		Database.initialize();
		System.out.println("done.");
		System.out.print("Setting up ElasticSearch...");
		Search.initialize();
		System.out.println("done.");

		// create developer id (used as a creator of the default visualization)
		ObjectId developerId = new ObjectId();

		// create default visualization
		System.out.print("Creating default visualization: \"" + Visualization.getDefaultVisualization()
				+ "\" in MongoDB...");
		Visualization recordList = new Visualization();
		recordList.creator = developerId;
		recordList.name = Visualization.getDefaultVisualization();
		recordList.description = "Default record list implementation. "
				+ "Lists your records and lets you choose for a particular record: "
				+ "(1) In which spaces it is shown, and (2) which circles it is shared with.";
		recordList.url = controllers.visualizations.routes.RecordList.load().url();
		try {
			Visualization.add(recordList);
		} catch (ModelException e) {
			System.out.println("error.\n" + e.getMessage() + "\nAborting...");
			System.exit(1);
		}
		System.out.println("done.");

		// create developer account
		// developer account currently has record list visualization installed
		// TODO developer account is different from user account (cannot have spaces, records and circles)
		System.out.print("Creating Health Data Cooperative developer account...");
		User developer = new User();
		developer._id = developerId;
		developer.email = "developers@hdc.ch";
		developer.name = "Health Data Cooperative Developers";
		developer.password = RandomStringUtils.randomAlphanumeric(20);
		try {
			User.add(developer);
		} catch (ModelException e) {
			System.out.println("error.\n" + e.getMessage() + "\nAborting...");
			System.exit(1);
		}
		System.out.println("done.");

		System.out.println("Shutting down...");
		Database.close();
		Search.close();
		System.out.println("Minimal setup complete.");
	}

}
