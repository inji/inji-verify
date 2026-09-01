package io.inji.verify.services.impl;

import io.inji.verify.exception.DidGenerationException;
import io.inji.verify.key.Extractor;
import io.inji.verify.key.impl.P12KeyExtractor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.NamedParameterSpec;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DID document *content* correctness (multibase encoding, verification method shape, etc.) is
 * already covered by DIDDocumentUtilTest; this class only checks that DidServiceImpl wires
 * Extractor + configured URIs into DIDDocumentUtil correctly, and that any failure surfaces as a
 * single DidGenerationException.
 */
class DidServiceImplTest {

    @Mock
    private Extractor extractor;

    private DidServiceImpl didService;

    private static KeyPair testKeyPair;
    private static final String VERIFY_DID_URI = "did:example:test-issuer";
    private static final String VERIFY_PUBLIC_KEY_URI = "did:example:test-issuer#key-0";

    @BeforeAll
    static void setup() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EdDSA", "BC");
        kpg.initialize(new NamedParameterSpec("Ed25519"));
        testKeyPair = kpg.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        didService = new DidServiceImpl(extractor);
        ReflectionTestUtils.setField(didService, "verifyDidURI", VERIFY_DID_URI);
        ReflectionTestUtils.setField(didService, "verifyPublicKeyURI", VERIFY_PUBLIC_KEY_URI);
    }

    @Test
    @DisplayName("Should build a DID document from the extracted key pair and configured URIs")
    void generateDidDocument_Success() {
        when(extractor.extractKeyPair()).thenReturn(testKeyPair);

        Map<String, Object> didDocument = didService.generateDidDocument();

        assertNotNull(didDocument);
        assertEquals(VERIFY_DID_URI, didDocument.get("id"));
        List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) didDocument.get("verificationMethod");
        assertEquals(VERIFY_PUBLIC_KEY_URI, verificationMethods.get(0).get("id"));
    }

    @Test
    @DisplayName("Should wrap key-extraction failures as DidGenerationException")
    void generateDidDocument_ExtractionFails_ThrowsDidGenerationException() {
        when(extractor.extractKeyPair()).thenThrow(new RuntimeException("keystore unreadable"));

        assertThrows(DidGenerationException.class, () -> didService.generateDidDocument());
    }

    @Test
    @DisplayName("Should wrap document-generation failures (e.g. malformed key) as DidGenerationException")
    void generateDidDocument_DocumentGenerationFails_ThrowsDidGenerationException() {
        // DIDDocumentUtil dereferences the public key internally; a null one fails there rather
        // than at extraction, exercising the same catch-and-wrap path in DidServiceImpl.
        when(extractor.extractKeyPair()).thenReturn(new KeyPair(null, testKeyPair.getPrivate()));

        assertThrows(DidGenerationException.class, () -> didService.generateDidDocument());
    }

    /**
     * End-to-end smoke test against the actual shipped keystore resource (real ResourceLoader,
     * real file bytes, real P12KeyExtractor) rather than a mocked Extractor. This is the only
     * test in the suite that touches sample-keystore/test.p12 directly, so it exists to catch
     * exactly the kind of problem a bad/regenerated keystore file would introduce (wrong alias
     * handling, wrong key algorithm, corrupt PKCS12 structure, wrong default password) before
     * it's discovered by actually running the app.
     */
    @Test
    @DisplayName("Should generate a DID document from the real shipped sample-keystore/test.p12")
    void generateDidDocument_RealSampleKeystore_Success() {
        P12KeyExtractor realExtractor = new P12KeyExtractor(
                "classpath:sample-keystore/test.p12", "mosip", new DefaultResourceLoader());
        DidServiceImpl realDidService = new DidServiceImpl(realExtractor);
        ReflectionTestUtils.setField(realDidService, "verifyDidURI", VERIFY_DID_URI);
        ReflectionTestUtils.setField(realDidService, "verifyPublicKeyURI", VERIFY_PUBLIC_KEY_URI);

        Map<String, Object> didDocument = realDidService.generateDidDocument();

        assertNotNull(didDocument);
        assertEquals(VERIFY_DID_URI, didDocument.get("id"));
        List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) didDocument.get("verificationMethod");
        assertEquals(1, verificationMethods.size());
        assertEquals(VERIFY_PUBLIC_KEY_URI, verificationMethods.get(0).get("id"));
        assertNotNull(verificationMethods.get(0).get("publicKeyMultibase"));
    }
}
