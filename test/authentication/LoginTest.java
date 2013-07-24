package authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.session;
import static play.test.Helpers.start;
import static play.test.Helpers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.LoadData;

import com.google.common.collect.ImmutableMap;

import controllers.database.Connection;

public class LoginTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Connection.connectTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		Connection.close();
	}

	@Test
	public void authenticateSuccess() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(),
				fakeRequest().withFormUrlEncodedBody(ImmutableMap.of("email", "test1@example.com", "password", "secret")));
		assertEquals(303, status(result));
		assertEquals("test1@example.com", session(result).get("email"));
	}

	@Test
	public void authenticateFailure() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(),
				fakeRequest().withFormUrlEncodedBody(ImmutableMap.of("email", "test1@example.com", "password", "badpassword")));
		assertEquals(400, status(result));
		assertNull(session(result).get("email"));
	}

	@Test
	public void authenticated() {
		Result result = callAction(controllers.routes.ref.Application.index(), fakeRequest().withSession("email", "test1@example.com"));
		assertEquals(200, status(result));
	}

	@Test
	public void notAuthenticated() {
		Result result = callAction(controllers.routes.ref.Application.index(), fakeRequest());
		assertEquals(303, status(result));
		assertEquals("/welcome", header("Location", result));
	}

}
