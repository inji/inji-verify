package io.inji.verify.models;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class VPSubmissionTest {

	@Test
	public void testConstructorAndGetters() {
		String requestId = "request123";
		String vpToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, null, null, false);
		assertEquals(requestId, vpSubmission.getRequestId());
		assertEquals(vpToken, vpSubmission.getVpToken());
		assertNull(vpSubmission.getPresentationSubmission());
		assertNull(vpSubmission.getError());
		assertNull(vpSubmission.getErrorDescription());
	}

	@Test
	public void testEmptyConstructor() {
		VPSubmission vpSubmission = new VPSubmission();
		assertNull(vpSubmission.getRequestId());
		assertNull(vpSubmission.getVpToken());
		assertNull(vpSubmission.getPresentationSubmission());
	}

	@Test
	public void testConstructorWithResponseCode() {
		String requestId = "request123";
		String vpToken = "token123";
		String responseCode = "response-code-123";
		Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));
		Boolean responseCodeUsed = false;

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, responseCode, expiryAt,
				responseCodeUsed);

		assertEquals(requestId, vpSubmission.getRequestId());
		assertEquals(vpToken, vpSubmission.getVpToken());
		assertEquals(responseCode, vpSubmission.getResponseCode());
		assertEquals(expiryAt, vpSubmission.getResponseCodeExpiryAt());
		assertEquals(false, vpSubmission.isResponseCodeUsed());
	}

	@Test
	public void testConstructorWithNullResponseCode() {
		String requestId = "request123";
		String vpToken = "token123";

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, null, null, false);

		assertNull(vpSubmission.getResponseCode());
		assertNull(vpSubmission.getResponseCodeExpiryAt());
		assertEquals(false, vpSubmission.isResponseCodeUsed());
	}

	@Test
	public void testResponseCodeUsed_CanBeSetToTrue() {
		String requestId = "request123";
		String vpToken = "token123";
		String responseCode = "code123";
		Timestamp expiryAt = Timestamp.from(java.time.Instant.now().plus(5, ChronoUnit.MINUTES));

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, responseCode, expiryAt,
				true);

		assertEquals(true, vpSubmission.isResponseCodeUsed());
	}

	@Test
	public void testResponseCodeExpiryAt_InFuture() {
		String requestId = "request123";
		String vpToken = "token123";
		String responseCode = "code123";
		Timestamp futureExpiry = Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES));

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, responseCode, futureExpiry,
				false);

		assertEquals(futureExpiry, vpSubmission.getResponseCodeExpiryAt());
		assertTrue(vpSubmission.getResponseCodeExpiryAt().toInstant().isAfter(Instant.now()),
				"Response code expiry should be in the future");
	}

	@Test
	public void testConstructorWithError() {
		String error = "access_denied";
		String errorDescription = "User denied access";

		VPSubmission vpSubmission = new VPSubmission(null, null, null, error, errorDescription, null, null, false);

		assertEquals(error, vpSubmission.getError());
		assertEquals(errorDescription, vpSubmission.getErrorDescription());
		assertNull(vpSubmission.getResponseCode());
		assertNull(vpSubmission.getResponseCodeExpiryAt());
	}

	@Test
	public void testConstructor_AllowsBothResponseCodeAndExpiryAtNull() {
		// When both are null, it satisfies the constraint
		String requestId = "request123";
		String vpToken = "token123";

		VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, null, null, null, null, // responseCode is null
				null, // responseCodeExpiryAt is null - OK
				false);

		assertNull(vpSubmission.getResponseCode());
		assertNull(vpSubmission.getResponseCodeExpiryAt());
		assertEquals(false, vpSubmission.isResponseCodeUsed());
	}
}