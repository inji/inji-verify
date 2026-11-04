package io.inji.verify.utils;

import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.utils.Util;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdocCredentialVerificationTest {

    private final CredentialsVerifier credentialsVerifier = new CredentialsVerifier();

    @Test
    void rejectsTamperedIssuerSignedElement() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String signedCredential = MdocTestCredentialFactory.create(
                now, now, now.plus(2, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(now));
        String tamperedCredential = MdocTestCredentialFactory.tamperFirstElement(signedCredential);

        VerificationResult result = verify(tamperedCredential);

        assertFalse(result.getVerificationStatus(), result.getVerificationMessage());
        assertTrue(result.getVerificationMessage().toLowerCase(Locale.ROOT).contains("digest"),
                result.getVerificationMessage());
    }

    @Test
    void rejectsExpiredMdoc() throws Exception {
        Instant signed = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = signed;
        Instant validUntil = signed.plus(1, ChronoUnit.DAYS);
        assertTrue(validUntil.isBefore(Instant.now()));
        String credential = MdocTestCredentialFactory.create(
                signed, validFrom, validUntil, printedExpiryFiveYearsFrom(signed));

        VerificationResult result = verify(credential);

        assertFalse(result.getVerificationStatus(), result.getVerificationMessage());
        assertTrue(result.getVerificationMessage().toLowerCase(Locale.ROOT).contains("validuntil"),
                result.getVerificationMessage());
    }

    @Test
    void reportsExpiredButAuthenticMdocThroughExpiryCheck() throws Exception {
        Instant signed = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String credential = MdocTestCredentialFactory.create(
                signed, signed, signed.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(signed));

        VerificationResult result = verify(credential);
        Utils.reportExpiredMdocAsExpiry(credential, CredentialFormat.MSO_MDOC, result);

        assertTrue(Utils.populateSchemaAndSignature(result).isValid(), result.getVerificationMessage());
        assertFalse(Utils.populateExpiryCheck(result).isValid());
        assertEquals(VerificationStatus.EXPIRED, Util.INSTANCE.getVerificationStatus(result));
    }

    @Test
    void keepsExpiredAndTamperedMdocInvalid() throws Exception {
        Instant signed = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String credential = MdocTestCredentialFactory.tamperFirstElement(MdocTestCredentialFactory.create(
                signed, signed, signed.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(signed)));

        VerificationResult result = verify(credential);
        Utils.reportExpiredMdocAsExpiry(credential, CredentialFormat.MSO_MDOC, result);

        assertFalse(Utils.populateSchemaAndSignature(result).isValid());
        assertEquals(VerificationStatus.INVALID, Util.INSTANCE.getVerificationStatus(result));
    }

    @Test
    void currentlyAcceptsMdocBeforeItsValidFromDate() throws Exception {
        Instant signed = Instant.now().minus(10, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        assertTrue(validFrom.isAfter(Instant.now()));
        String credential = MdocTestCredentialFactory.create(
                signed, validFrom, validFrom.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(validFrom));

        VerificationResult result = verify(credential);

        // Library checks validUntil but does not reject credentials before validFrom.
        assertTrue(result.getVerificationStatus(), result.getVerificationMessage());
    }

    @Test
    void acceptsPrintedExpiryLaterThanMsoValidity() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant validUntil = now.plus(2, ChronoUnit.DAYS);
        Instant printedExpiry = Instant.parse(printedExpiryFiveYearsFrom(now));
        assertTrue(printedExpiry.isAfter(validUntil));
        String credential = MdocTestCredentialFactory.create(
                now, now, validUntil, printedExpiry.toString());

        VerificationResult result = verify(credential);

        assertTrue(result.getVerificationStatus(), result.getVerificationMessage());
    }

    private VerificationResult verify(String credential) {
        return credentialsVerifier.verify(credential, CredentialFormat.MSO_MDOC, false);
    }

    private static String printedExpiryFiveYearsFrom(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).plusYears(5).toInstant().toString();
    }
}
