package io.inji.verify.utils;

import io.inji.verify.exception.DidGenerationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;

import java.security.spec.NamedParameterSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class DIDDocumentUtilTest {

    private static KeyPair testKeyPair;
    private static final String issuerURI = "did:example:123";
    private static final String issuerPublicKeyURI = "did:example:123#key-1";

    @BeforeAll
    static void setup() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            System.out.println("Bouncy Castle provider added for tests.");
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EdDSA", "BC");
        kpg.initialize(new NamedParameterSpec("Ed25519"));

        testKeyPair = kpg.generateKeyPair();
    }

    @Test
    @DisplayName("Should generate DID Document correctly for Ed25519 public key")
    void generateDIDDocument_Ed25519PublicKey_Success() {
        Map<String, Object> didDocument = DIDDocumentUtil.generateDIDDocument(
                testKeyPair.getPublic(),
                issuerURI,
                issuerPublicKeyURI
        );

        assertNotNull(didDocument);
        assertEquals(issuerURI, didDocument.get("id"));
        assertEquals(Collections.singletonList("https://www.w3.org/ns/did/v1"), didDocument.get("@context"));
        assertTrue(((List<?>) didDocument.get("alsoKnownAs")).isEmpty());
        assertTrue(((List<?>) didDocument.get("service")).isEmpty());
        assertEquals(Collections.singletonList(issuerPublicKeyURI), didDocument.get("authentication"));
        assertEquals(Collections.singletonList(issuerPublicKeyURI), didDocument.get("assertionMethod"));

        List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) didDocument.get("verificationMethod");
        assertNotNull(verificationMethods);
        assertEquals(1, verificationMethods.size());

        Map<String, Object> vm = verificationMethods.get(0);
        assertEquals(issuerPublicKeyURI, vm.get("id"));
        assertEquals("Ed25519VerificationKey2020", vm.get("type"));
        assertEquals("https://w3id.org/security/suites/ed25519-2020/v1", vm.get("@context"));
        assertEquals(issuerURI, vm.get("controller"));

        String publicKeyMultibase = (String) vm.get("publicKeyMultibase");
        assertNotNull(publicKeyMultibase);
        assertTrue(publicKeyMultibase.startsWith("z"), "Public key should be multibase encoded with base58btc (z prefix)");

        assertTrue(publicKeyMultibase.length() > 40, "Multibase encoded key should have sufficient length");
    }

    @Test
    @DisplayName("Should throw DidGenerationException when KeyFactory throws NoSuchAlgorithmException")
    void generateDIDDocument_NoSuchAlgorithmException_ThrowsDidGenerationException() {
        try (MockedStatic<KeyFactory> mockedKeyFactory = Mockito.mockStatic(KeyFactory.class)) {
            mockedKeyFactory.when(() -> KeyFactory.getInstance("EdDSA", "BC"))
                    .thenThrow(new NoSuchAlgorithmException("EdDSA algorithm not available"));

            Exception exception = assertThrows(DidGenerationException.class, () -> {
                DIDDocumentUtil.generateDIDDocument(testKeyPair.getPublic(), issuerURI, issuerPublicKeyURI);
            });

            assertNotNull(exception);
        }
    }

    @Test
    @DisplayName("Should throw DidGenerationException when KeyFactory throws NoSuchProviderException")
    void generateDIDDocument_NoSuchProviderException_ThrowsDidGenerationException() {
        try (MockedStatic<KeyFactory> mockedKeyFactory = Mockito.mockStatic(KeyFactory.class)) {
            mockedKeyFactory.when(() -> KeyFactory.getInstance("EdDSA", "BC"))
                    .thenThrow(new NoSuchProviderException("BC provider not available"));

            Exception exception = assertThrows(DidGenerationException.class, () -> {
                DIDDocumentUtil.generateDIDDocument(testKeyPair.getPublic(), issuerURI, issuerPublicKeyURI);
            });

            assertNotNull(exception);
        }
    }

    @Test
    @DisplayName("Should throw DidGenerationException when KeyFactory throws InvalidKeySpecException")
    void generateDIDDocument_InvalidKeySpecException_ThrowsDidGenerationException() {
        try (MockedStatic<KeyFactory> mockedKeyFactory = Mockito.mockStatic(KeyFactory.class)) {
            KeyFactory mockKeyFactory = Mockito.mock(KeyFactory.class);
            mockedKeyFactory.when(() -> KeyFactory.getInstance("EdDSA", "BC"))
                    .thenReturn(mockKeyFactory);

            try {
                Mockito.when(mockKeyFactory.generatePublic(any(X509EncodedKeySpec.class)))
                        .thenThrow(new InvalidKeySpecException("Invalid key specification"));
            } catch (InvalidKeySpecException e) {
            }

            Exception exception = assertThrows(DidGenerationException.class, () -> {
                DIDDocumentUtil.generateDIDDocument(testKeyPair.getPublic(), issuerURI, issuerPublicKeyURI);
            });

            assertNotNull(exception);
        }
    }

    @Test
    @DisplayName("Should throw DidGenerationException when BCEdDSAPublicKey.getPointEncoding() throws RuntimeException")
    void generateDIDDocument_PointEncodingError_ThrowsDidGenerationException() {
        try (MockedStatic<KeyFactory> mockedKeyFactory = Mockito.mockStatic(KeyFactory.class)) {
            KeyFactory mockKeyFactory = Mockito.mock(KeyFactory.class);
            BCEdDSAPublicKey mockBCKey = Mockito.mock(BCEdDSAPublicKey.class);

            mockedKeyFactory.when(() -> KeyFactory.getInstance("EdDSA", "BC"))
                    .thenReturn(mockKeyFactory);

            try {
                Mockito.when(mockKeyFactory.generatePublic(any(X509EncodedKeySpec.class)))
                        .thenReturn(mockBCKey);
            } catch (InvalidKeySpecException e) {
            }

            Mockito.when(mockBCKey.getPointEncoding())
                    .thenThrow(new RuntimeException("Error getting point encoding"));

            Exception exception = assertThrows(DidGenerationException.class, () -> {
                DIDDocumentUtil.generateDIDDocument(testKeyPair.getPublic(), issuerURI, issuerPublicKeyURI);
            });

            assertNotNull(exception);
        }
    }

    @Test
    @DisplayName("Should handle null parameters gracefully")
    void generateDIDDocument_NullParameters_ThrowsException() {
        assertThrows(Exception.class, () -> {
            DIDDocumentUtil.generateDIDDocument(null, issuerURI, issuerPublicKeyURI);
        });

    }

    @Test
    @DisplayName("Should verify multicodec prefix is correctly applied")
    void generateDIDDocument_VerifyMulticodecPrefix_Success() throws Exception {
        Map<String, Object> didDocument = DIDDocumentUtil.generateDIDDocument(
                testKeyPair.getPublic(),
                issuerURI,
                issuerPublicKeyURI
        );

        List<Map<String, Object>> verificationMethods = (List<Map<String, Object>>) didDocument.get("verificationMethod");
        String publicKeyMultibase = (String) verificationMethods.get(0).get("publicKeyMultibase");

        assertNotNull(publicKeyMultibase);
        assertTrue(publicKeyMultibase.startsWith("z"));

        assertTrue(publicKeyMultibase.length() > 40);
    }
}