package io.inji.verify.dto.client;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VpFormatsTest {

    @Test
    public void testVpFormats_WithLdpVp() {
        List<String> proofTypes = Arrays.asList("LdProof", "JsonWebSignature2020");
        LdpVp ldpVp = new LdpVp(proofTypes);

        VpFormats vpFormats = new VpFormats(ldpVp,null);

        assertNotNull(vpFormats.getLdpVp(), "The LdpVp object should not be null.");
        assertEquals(ldpVp, vpFormats.getLdpVp(), "The LdpVp object should match the one set in the constructor.");

        List<String> retrievedProofTypes = vpFormats.getLdpVp().getProofType();
        assertNotNull(retrievedProofTypes, "The proofType list should not be null.");
        assertEquals(2, retrievedProofTypes.size(), "The proofType list should contain 2 elements.");
        assertTrue(retrievedProofTypes.contains("LdProof"), "The proofType list should contain 'LdProof'.");
        assertTrue(retrievedProofTypes.contains("JsonWebSignature2020"), "The proofType list should contain 'JsonWebSignature2020'.");
    }

    @Test
    public void testVpFormats_WithNullLdpVp() {
        VpFormats vpFormats = new VpFormats(null,null);

        assertNull(vpFormats.getLdpVp(), "The LdpVp object should be null when initialized with null.");
    }

    @Test
    public void testVpFormats_WithLdpVp_EmptyProofTypeList() {
        List<String> proofTypes = Arrays.asList();
        LdpVp ldpVp = new LdpVp(proofTypes);

        VpFormats vpFormats = new VpFormats(ldpVp,null);

        assertNotNull(vpFormats.getLdpVp(), "The LdpVp object should not be null.");
        assertEquals(ldpVp, vpFormats.getLdpVp(), "The LdpVp object should match the one set in the constructor.");

        List<String> retrievedProofTypes = vpFormats.getLdpVp().getProofType();
        assertNotNull(retrievedProofTypes, "The proofType list should not be null.");
        assertTrue(retrievedProofTypes.isEmpty(), "The proofType list should be empty.");
    }
    @Test
     void testVpFormats_WithSdJwt() {
        List<String> algs = Arrays.asList("EdDSA");
        List<String> proofTypes = Arrays.asList("JwtProof2020");

        SdJwt sdJwt = new SdJwt(algs, proofTypes);

        VpFormats vpFormats = new VpFormats(null, sdJwt);

        assertNotNull(vpFormats.getSdJwt(), "The SdJwt object should not be null.");
        assertEquals(sdJwt, vpFormats.getSdJwt(), "The SdJwt object should match the one set in the constructor.");
    }
}