package io.inji.verify.shared;

import io.inji.verify.dto.client.LdpVp;
import io.inji.verify.dto.client.VpFormatsSupported;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConstantsTest {

    @Test
    @DisplayName("Verify numeric constants")
    void testNumericConstants() {
        assertEquals(300, Constants.DEFAULT_EXPIRY);
    }

    @Test
    @DisplayName("Verify URI constants")
    void testUriConstants() {
        assertEquals("/v2/vp-submission/direct-post", Constants.VP_RESPONSE_SUBMISSION_URI);
        assertEquals("/vp-definition/", Constants.VP_DEFINITION_URI);
        assertEquals("/v2/vp-request", Constants.VP_REQUEST_URI);
    }

    @Test
    @DisplayName("Verify response type constant")
    void testResponseTypeConstants() {
        assertEquals("vp_token", Constants.RESPONSE_TYPE);
    }

    @Test
    @DisplayName("Verify ID prefix constants")
    void testIdPrefixConstants() {
        assertEquals("txn", Constants.TRANSACTION_ID_PREFIX);
        assertEquals("req", Constants.REQUEST_ID_PREFIX);
    }

    @Test
    @DisplayName("Verify signature constants")
    void testSignatureTypeConstants() {
        assertEquals("RsaSignature2018", Constants.RSA_SIGNATURE_2018);
        assertEquals("Ed25519Signature2018", Constants.ED25519_SIGNATURE_2018);
        assertEquals("Ed25519Signature2020", Constants.ED25519_SIGNATURE_2020);
    }

    @Test
    @DisplayName("Verify JSON key constants")
    void testJsonKeyConstants() {
        assertEquals("proof", Constants.KEY_PROOF);
        assertEquals("type", Constants.KEY_TYPE);
        assertEquals("jws", Constants.KEY_JWS);
        assertEquals("verificationMethod", Constants.KEY_VERIFICATION_METHOD);
        assertEquals("verifiableCredential", Constants.KEY_VERIFIABLE_CREDENTIAL);
        assertEquals("credential", Constants.KEY_CREDENTIAL);
    }

    @Test
    @DisplayName("Verify VP_FORMATS_SUPPORTED configuration")
    void testVpFormatsSupportedConstant() {
        assertNotNull(Constants.VP_FORMATS_SUPPORTED);
        assertInstanceOf(VpFormatsSupported.class, Constants.VP_FORMATS_SUPPORTED);

        LdpVp ldpVp = Constants.VP_FORMATS_SUPPORTED.getLdpVp();
        assertNotNull(ldpVp);

        List<String> proofTypes = ldpVp.getProofType();
        assertNotNull(proofTypes);
        assertEquals(3, proofTypes.size());

        List<String> expectedOrder = Arrays.asList(
                "Ed25519Signature2018",
                "Ed25519Signature2020",
                "RsaSignature2018"
        );

        assertEquals(expectedOrder, proofTypes);

        assertTrue(proofTypes.contains(Constants.ED25519_SIGNATURE_2018));
        assertTrue(proofTypes.contains(Constants.ED25519_SIGNATURE_2020));
        assertTrue(proofTypes.contains(Constants.RSA_SIGNATURE_2018));
    }

    @Test
    @DisplayName("Verify constants are not null or empty")
    void testConstantsNotNullOrEmpty() {
        assertNotNull(Constants.VP_RESPONSE_SUBMISSION_URI);
        assertFalse(Constants.VP_RESPONSE_SUBMISSION_URI.isEmpty());

        assertNotNull(Constants.VP_DEFINITION_URI);
        assertFalse(Constants.VP_DEFINITION_URI.isEmpty());

        assertNotNull(Constants.VP_REQUEST_URI);
        assertFalse(Constants.VP_REQUEST_URI.isEmpty());

        assertNotNull(Constants.RESPONSE_TYPE);
        assertFalse(Constants.RESPONSE_TYPE.isEmpty());

        assertNotNull(Constants.TRANSACTION_ID_PREFIX);
        assertNotNull(Constants.REQUEST_ID_PREFIX);

        assertNotNull(Constants.VP_FORMATS_SUPPORTED);
    }

    @Test
    @DisplayName("Verify URI formatting")
    void testUriFormatting() {
        assertTrue(Constants.VP_DEFINITION_URI.startsWith("/"));
        assertTrue(Constants.VP_REQUEST_URI.startsWith("/"));

        assertTrue(Constants.VP_DEFINITION_URI.endsWith("/"));
    }

    @Test
    @DisplayName("Cover private constructor for SonarQube")
    void coverPrivateConstructor() throws Exception {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        Executable exec = () -> constructor.newInstance();
        assertDoesNotThrow(exec);
    }

    @Test
    @DisplayName("Verify Status Purpose Constant")
    void testStatusPurposeConstant() {
        assertEquals("revocation", Constants.STATUS_PURPOSE_REVOKED);
    }
}
