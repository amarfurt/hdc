package authentication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Test;

import utils.PasswordHash;

public class PasswordHashTest {

	@Test
	public void sameHash() throws NoSuchAlgorithmException, InvalidKeySpecException {
		for (int i = 0; i < 100; i++) {
			String password = "" + i;
			String hash = PasswordHash.createHash(password);
			String secondHash = PasswordHash.createHash(password);
			assertFalse(hash.equals(secondHash));
		}
	}

	@Test
	public void wrongPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
		for (int i = 0; i < 100; i++) {
			String password = "" + i;
			String hash = PasswordHash.createHash(password);
			String wrongPassword = "" + (i + 1);
			assertFalse(PasswordHash.validatePassword(wrongPassword, hash));
		}
	}

	@Test
	public void correctPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
		for (int i = 0; i < 100; i++) {
			String password = "" + i;
			String hash = PasswordHash.createHash(password);
			assertTrue(PasswordHash.validatePassword(password, hash));
		}
	}

}
