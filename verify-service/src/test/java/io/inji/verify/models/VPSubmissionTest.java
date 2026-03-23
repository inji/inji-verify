package io.inji.verify.models;

import io.inji.verify.dto.submission.PresentationSubmissionDto;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VPSubmissionTest {

    @Test
    public void testConstructorAndGetters() {
        String requestId = "request123";
        String vpToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());

        VPSubmission vpSubmission = new VPSubmission(requestId, vpToken,
                presentationSubmission, null, null, null, null, false);
        assertEquals(requestId, vpSubmission.getRequestId());
        assertEquals(vpToken, vpSubmission.getVpToken());
        assertEquals(presentationSubmission, vpSubmission.getPresentationSubmission());
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
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());
        String responseCode = "response-code-123";
        Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));
        Boolean responseCodeUsed = false;

        VPSubmission vpSubmission = new VPSubmission(
                requestId,
                vpToken,
                presentationSubmission,
                null,
                null,
                responseCode,
                expiryAt,
                responseCodeUsed
        );

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
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());

        VPSubmission vpSubmission = new VPSubmission(
                requestId,
                vpToken,
                presentationSubmission,
                null,
                null,
                null,
                null,
                false
        );

        assertNull(vpSubmission.getResponseCode());
        assertNull(vpSubmission.getResponseCodeExpiryAt());
        assertEquals(false, vpSubmission.isResponseCodeUsed());
    }

    @Test
    public void testResponseCodeUsed_CanBeSetToTrue() {
        String requestId = "request123";
        String vpToken = "token123";
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());
        String responseCode = "code123";
        Timestamp expiryAt = Timestamp.from(java.time.Instant.now().plus(5, ChronoUnit.MINUTES));

        VPSubmission vpSubmission = new VPSubmission(
                requestId,
                vpToken,
                presentationSubmission,
                null,
                null,
                responseCode,
                expiryAt,
                true
        );

        assertEquals(true, vpSubmission.isResponseCodeUsed());
    }

    @Test
    public void testResponseCodeExpiryAt_InFuture() {
        String requestId = "request123";
        String vpToken = "token123";
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());
        String responseCode = "code123";
        Timestamp futureExpiry = Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES)
        );

        VPSubmission vpSubmission = new VPSubmission(
                requestId,
                vpToken,
                presentationSubmission,
                null,
                null,
                responseCode,
                futureExpiry,
                false
        );

        assertEquals(futureExpiry, vpSubmission.getResponseCodeExpiryAt());
        assertTrue(vpSubmission.getResponseCodeExpiryAt().toInstant().isAfter(Instant.now()),
                "Response code expiry should be in the future");
    }

    @Test
    public void testConstructorWithError() {
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());
        String error = "access_denied";
        String errorDescription = "User denied access";

        VPSubmission vpSubmission = new VPSubmission(
                null,
                null,
                presentationSubmission,
                error,
                errorDescription,
                null,
                null,
                false
        );

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
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());

        VPSubmission vpSubmission = new VPSubmission(
                requestId,
                vpToken,
                presentationSubmission,
                null,
                null,
                null,  // responseCode is null
                null,  // responseCodeExpiryAt is null - OK
                false
        );

        assertNull(vpSubmission.getResponseCode());
        assertNull(vpSubmission.getResponseCodeExpiryAt());
        assertEquals(false, vpSubmission.isResponseCodeUsed());
    }
}