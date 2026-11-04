package io.inji.verify.utils;

import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.utils.Util;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdocCredentialVerificationTest {

    /**
     * These tests expect vc-verifier to report an authentic expired or not-yet-valid mdoc as
     * ERR_VC_EXPIRED after checking its signature and digests. No published vcverifier-jar has that
     * fix yet; remove the annotation when vcverifier-jar.version is bumped to a release that does.
     */
    private static final String AWAITS_LIBRARY_VALIDITY_FIX =
            "Needs the vc-verifier mdoc validity fix (validFrom check, ERR_VC_EXPIRED after signature checks)";

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
    @Disabled(AWAITS_LIBRARY_VALIDITY_FIX)
    void reportsExpiredButAuthenticMdocThroughExpiryCheck() throws Exception {
        Instant signed = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String credential = MdocTestCredentialFactory.create(
                signed, signed, signed.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(signed));

        VerificationResult result = verify(credential);

        assertTrue(Utils.populateSchemaAndSignature(result).isValid(), result.getVerificationMessage());
        assertFalse(Utils.populateExpiryCheck(result).isValid());
        assertEquals(VerificationStatus.EXPIRED, Util.INSTANCE.getVerificationStatus(result));
        assertEquals("ERR_VC_EXPIRED", result.getVerificationErrorCode());
    }

    @Test
    @Disabled(AWAITS_LIBRARY_VALIDITY_FIX)
    void keepsExpiredAndTamperedMdocInvalid() throws Exception {
        Instant signed = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String credential = MdocTestCredentialFactory.tamperFirstElement(MdocTestCredentialFactory.create(
                signed, signed, signed.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(signed)));

        VerificationResult result = verify(credential);

        assertFalse(Utils.populateSchemaAndSignature(result).isValid());
        assertEquals(VerificationStatus.INVALID, Util.INSTANCE.getVerificationStatus(result));
        assertEquals("ERR_SIGNATURE_VERIFICATION_FAILED", result.getVerificationErrorCode());
    }

    @Test
    @Disabled(AWAITS_LIBRARY_VALIDITY_FIX)
    void keepsExpiredMdocWithInconsistentValidityWindowInvalid() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant signed = now.minus(3, ChronoUnit.DAYS);
        Instant validFrom = now.minus(1, ChronoUnit.DAYS);
        Instant validUntil = now.minus(2, ChronoUnit.DAYS);
        String credential = MdocTestCredentialFactory.create(
                signed, validFrom, validUntil, printedExpiryFiveYearsFrom(now));

        VerificationResult result = verify(credential);

        assertFalse(Utils.populateSchemaAndSignature(result).isValid());
        assertEquals("ERR_INVALID_DATE_MSO", result.getVerificationErrorCode());
    }

    @Test
    @Disabled(AWAITS_LIBRARY_VALIDITY_FIX)
    void reportsMdocBeforeItsValidFromDateThroughExpiryCheck() throws Exception {
        Instant signed = Instant.now().minus(10, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
        Instant validFrom = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        assertTrue(validFrom.isAfter(Instant.now()));
        String credential = MdocTestCredentialFactory.create(
                signed, validFrom, validFrom.plus(1, ChronoUnit.DAYS), printedExpiryFiveYearsFrom(validFrom));

        VerificationResult result = verify(credential);


        assertTrue(Utils.populateSchemaAndSignature(result).isValid(), result.getVerificationMessage());
        assertFalse(Utils.populateExpiryCheck(result).isValid());
        assertEquals(VerificationStatus.EXPIRED, Util.INSTANCE.getVerificationStatus(result));
        assertEquals("ERR_VC_EXPIRED", result.getVerificationErrorCode());
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

        // AC-6: expiry follows the MSO window (still valid), not the later printed date,
        // and the printed date is returned as a data element.
        assertTrue(result.getVerificationStatus(), result.getVerificationMessage());
        assertEquals(VerificationStatus.SUCCESS, Util.INSTANCE.getVerificationStatus(result));
        assertTrue(Utils.populateExpiryCheck(result).isValid());
        Map<?, ?> claims = (Map<?, ?>) Utils.extractMdocClaims(credential, List.of()).get("org.iso.18013.5.1");
        assertEquals(printedExpiry.toString(), claims.get("expiry_date"));
    }

    private VerificationResult verify(String credential) {
        return credentialsVerifier.verify(credential, CredentialFormat.MSO_MDOC, false);
    }

    private static String printedExpiryFiveYearsFrom(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).plusYears(5).toInstant().toString();
    }
}
