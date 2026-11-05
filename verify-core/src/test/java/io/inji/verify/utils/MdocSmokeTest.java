package io.inji.verify.utils;

import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One smoke test that the fixed valid mDL sample verifies with vc-verifier. Signature, digest,
 * validity and algorithm behaviour is tested in vc-verifier itself.
 */
class MdocSmokeTest {

    @Test
    void fixedValidMdlSampleVerifies() throws Exception {
        VerificationResult result = new CredentialsVerifier()
                .verify(UtilsTest.mdlSample("mdl-valid.txt"), CredentialFormat.MSO_MDOC, false);

        // The sample's validUntil is 2031-11-05T05:05:21Z; regenerate the set before then
        // (src/test/resources/mdl-samples/README.md).
        assertTrue(result.getVerificationStatus(), result.getVerificationMessage());
        assertTrue(Utils.populateSchemaAndSignature(result).isValid());
        assertTrue(Utils.populateExpiryCheck(result).isValid());
    }
}
