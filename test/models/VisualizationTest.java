package models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.start;

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

public class VisualizationTest {

	private static final String collection = "visualizations";

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
	public void add() throws IllegalArgumentException, IllegalAccessException {
		DBCollection visualizations = TestConnection.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.tags = new BasicDBList();
		assertNull(Visualization.add(visualization));
		assertEquals(1, visualizations.count());
		assertEquals("Test visualization", visualizations.findOne().get("name"));
		assertNotNull(visualization._id);
	}

	@Test
	public void addWithExistingName() throws IllegalArgumentException, IllegalAccessException {
		DBCollection visualizations = TestConnection.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.tags = new BasicDBList();
		assertNull(Visualization.add(visualization));
		assertEquals(1, visualizations.count());
		Visualization anotherVisualization = new Visualization();
		anotherVisualization.name = visualization.name;
		anotherVisualization.description = "Test description 2";
		anotherVisualization.tags = new BasicDBList();
		assertEquals("A visualization with this name already exists.", Visualization.add(anotherVisualization));
		assertEquals(1, visualizations.count());
	}

	@Test
	public void delete() throws IllegalArgumentException, IllegalAccessException {
		DBCollection visualizations = TestConnection.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.tags = new BasicDBList();
		DBObject dbObject = new BasicDBObject(ModelConversion.modelToMap(visualization));
		visualizations.insert(dbObject);
		assertEquals(1, visualizations.count());
		assertNull(Visualization.delete((ObjectId) dbObject.get("_id")));
		assertEquals(0, visualizations.count());
	}
	
	@Test
	public void deleteFailure() throws IllegalArgumentException, IllegalAccessException {
		DBCollection visualizations = TestConnection.getCollection(collection);
		assertEquals(0, visualizations.count());
		Visualization visualization = new Visualization();
		visualization.name = "Test visualization";
		visualization.description = "Test description";
		visualization.tags = new BasicDBList();
		DBObject dbObject = new BasicDBObject(ModelConversion.modelToMap(visualization));
		visualizations.insert(dbObject);
		assertEquals(1, visualizations.count());
		assertNull(Visualization.delete((ObjectId) dbObject.get("_id")));
		assertEquals(0, visualizations.count());
		assertEquals("No visualizations with this id exists.", Visualization.delete((ObjectId) dbObject.get("_id")));
		assertEquals(0, visualizations.count());
	}
}
