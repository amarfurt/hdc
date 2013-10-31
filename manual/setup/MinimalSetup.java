package setup;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;
import models.User;
import models.Visualization;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;

import utils.Connection;

import com.mongodb.BasicDBList;

/**
 * Minimal setup that is necessary to start a fresh Healthbank platform.
 */
public class MinimalSetup {

	public static void main(String[] args) throws Exception {
		System.out.println("Starting to create minimal setup for Healthbank platform.");
		start(fakeApplication(fakeGlobal()));
		Connection.connect();

		// create developer id
		ObjectId developerId = new ObjectId();

		// create default 'Record List' visualization
		System.out.print("Creating default visualization: \"Record List\"...");
		Visualization recordList = new Visualization();
		recordList.creator = developerId;
		recordList.name = "Record List";
		recordList.description = "Default record list implementation. "
				+ "Lists your records and lets you choose for a particular record: "
				+ "(1) In which spaces it is shown, and (2) which circles it is shared with.";
		recordList.url = controllers.visualizations.routes.RecordList.load().url();
		recordList.tags = new BasicDBList();
		recordList.tags.add("record");
		recordList.tags.add("list");
		Visualization.add(recordList);
		System.out.println("done.");

		// create developer account
		// developer account currently has record list visualization installed
		// TODO developer account is different from user account (cannot have spaces, records and circles)
		System.out.print("Creating Healthbank developer account...");
		User developer = new User();
		developer._id = developerId;
		developer.email = "developers@healthbank.ch";
		developer.name = "Healthbank Developers";
		developer.password = RandomStringUtils.randomAlphanumeric(20);
		User.add(developer);
		System.out.println("done.");

		Connection.close();
		System.out.println("Minimal setup complete.");
	}

}
