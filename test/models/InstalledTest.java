package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import utils.ModelConversion;
import utils.TestConnection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class InstalledTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		TestConnection.connectToTest();
		TestConnection.dropDatabase();
	}

	@After
	public void tearDown() {
		TestConnection.close();
	}

	@Test
	public void addUser() throws IllegalArgumentException, IllegalAccessException, NoSuchAlgorithmException,
			InvalidKeySpecException, InstantiationException {
		DBCollection users = TestConnection.getCollection("users");
		assertEquals(0, users.count());
		DBCollection installed = TestConnection.getCollection("installed");
		assertEquals(0, installed.count());
		User user = new User();
		user.email = "test1@example.com";
		user.name = "Test User";
		user.password = "secret";
		assertNull(User.add(user));
		assertEquals(1, users.count());
		assertEquals(1, installed.count());
		DBObject dbObject = installed.findOne();
		assertEquals(user._id, dbObject.get("_id"));
		assertEquals(0, ((BasicDBList) dbObject.get("apps")).size());
		assertEquals(0, ((BasicDBList) dbObject.get("visualizations")).size());
	}

	@Test
	public void installApp() throws IllegalArgumentException, IllegalAccessException {
		DBCollection installed = TestConnection.getCollection("installed");
		assertEquals(0, installed.count());
		Installed newInstalled = new Installed();
		newInstalled._id = new ObjectId();
		newInstalled.apps = new BasicDBList();
		newInstalled.visualizations = new BasicDBList();
		installed.insert(new BasicDBObject(ModelConversion.modelToMap(newInstalled)));
		assertEquals(1, installed.count());
		ObjectId appId = new ObjectId();
		assertNull(Installed.installApp(appId, newInstalled._id));
		assertEquals(1, installed.count());
		DBObject dbObject = installed.findOne();
		BasicDBList apps = (BasicDBList) dbObject.get("apps");
		assertEquals(1, apps.size());
		assertEquals(appId, apps.get(0));
	}

	@Test
	public void uninstallVisualization() throws IllegalArgumentException, IllegalAccessException {
		DBCollection installed = TestConnection.getCollection("installed");
		assertEquals(0, installed.count());
		Installed newInstalled = new Installed();
		newInstalled._id = new ObjectId();
		newInstalled.apps = new BasicDBList();
		newInstalled.visualizations = new BasicDBList();
		installed.insert(new BasicDBObject(ModelConversion.modelToMap(newInstalled)));
		assertEquals(1, installed.count());
		ObjectId visualizationId = new ObjectId();
		Installed.installVisualization(visualizationId, newInstalled._id);
		assertEquals(1, installed.count());
		DBObject before = installed.findOne();
		assertEquals(1, ((BasicDBList) before.get("visualizations")).size());
		assertNull(Installed.uninstallVisualization(visualizationId, newInstalled._id));
		assertEquals(1, installed.count());
		DBObject after = installed.findOne();
		assertEquals(0, ((BasicDBList) after.get("visualizations")).size());
	}

	@Test
	public void deleteVisualization() throws IllegalArgumentException, IllegalAccessException {
		DBCollection installed = TestConnection.getCollection("installed");
		assertEquals(0, installed.count());
		ObjectId installed1 = new ObjectId();
		ObjectId installed2 = new ObjectId();
		ObjectId installed3 = new ObjectId();
		ObjectId visualizationId = new ObjectId();
		ObjectId visualizationId2 = new ObjectId();
		Installed newInstalled = new Installed();
		newInstalled._id = installed1;
		newInstalled.apps = new BasicDBList();
		newInstalled.visualizations = new BasicDBList();
		newInstalled.visualizations.add(visualizationId);
		installed.insert(new BasicDBObject(ModelConversion.modelToMap(newInstalled)));
		newInstalled._id = installed2;
		newInstalled.visualizations.add(visualizationId2);
		installed.insert(new BasicDBObject(ModelConversion.modelToMap(newInstalled)));
		newInstalled._id = installed3;
		newInstalled.visualizations.clear();
		installed.insert(new BasicDBObject(ModelConversion.modelToMap(newInstalled)));
		assertEquals(3, installed.count());
		assertNull(Installed.deleteVisualization(visualizationId));
		DBObject dbObject1 = installed.findOne(new BasicDBObject("_id", installed1));
		assertEquals(0, ((BasicDBList) dbObject1.get("visualizations")).size());
		DBObject dbObject2 = installed.findOne(new BasicDBObject("_id", installed2));
		assertEquals(1, ((BasicDBList) dbObject2.get("visualizations")).size());
		assertEquals(visualizationId2, ((BasicDBList) dbObject2.get("visualizations")).get(0));
		DBObject dbObject3 = installed.findOne(new BasicDBObject("_id", installed3));
		assertEquals(0, ((BasicDBList) dbObject3.get("visualizations")).size());
	}

}
