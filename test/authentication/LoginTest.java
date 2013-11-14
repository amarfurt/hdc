package authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.fakeGlobal;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.session;
import static play.test.Helpers.start;
import static play.test.Helpers.status;
import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import utils.Connection;
import utils.LoadData;

import com.google.common.collect.ImmutableMap;

public class LoginTest {

	@Before
	public void setUp() {
		start(fakeApplication(fakeGlobal()));
		Connection.connectToTest();
		LoadData.load();
	}

	@After
	public void tearDown() {
		Connection.close();
	}

	@Test
	public void authenticateSuccess() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(), fakeRequest()
				.withFormUrlEncodedBody(ImmutableMap.of("email", "test1@example.com", "password", "secret")));
		assertEquals(303, status(result));
		assertNotNull(session(result).get("id"));
		assertEquals(User.getId("test1@example.com").toString(), session(result).get("id"));
	}

	@Test
	public void authenticateFailureEmail() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(), fakeRequest()
				.withFormUrlEncodedBody(ImmutableMap.of("email", "testA@example.com", "password", "secret")));
		assertEquals(400, status(result));
		assertNull(session(result).get("id"));
	}

	@Test
	public void authenticateFailurePassword() {
		Result result = callAction(controllers.routes.ref.Application.authenticate(), fakeRequest()
				.withFormUrlEncodedBody(ImmutableMap.of("email", "test1@example.com", "password", "badpassword")));
		assertEquals(400, status(result));
		assertNull(session(result).get("id"));
	}

	@Test
	public void authenticated() {
		Result result = callAction(controllers.routes.ref.Application.index(),
				fakeRequest().withSession("id", User.getId("test1@example.com").toString()));
		assertEquals(200, status(result));
	}

	@Test
	public void notAuthenticated() {
		Result result = callAction(controllers.routes.ref.Application.index(), fakeRequest());
		assertEquals(303, status(result));
		assertEquals("/welcome", header("Location", result));
	}

}
