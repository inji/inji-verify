package io.inji.verify.utils;

import org.junit.jupiter.api.Test;

import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @Test
    void testGenerateNonce_LengthAndFormat() {
        String nonce = SecurityUtils.generateNonce();

        assertEquals(22, nonce.length());

        // Base64URL without padding: [A-Za-z0-9\-_], no '=' padding
        assertTrue(Pattern.matches("^[A-Za-z0-9\\-_]+$", nonce));
        assertFalse(nonce.contains("="), "Nonce must not contain Base64 padding");

        Set<String> nonces = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            nonces.add(SecurityUtils.generateNonce());
        }
        assertTrue(nonces.size() > 1, "Expected different nonces across calls");
    }

    @Test
    void testReadX509PublicKey_Success() throws Exception {
        String pem = """
                -----BEGIN PUBLIC KEY-----
                MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv9P0hHwU4J3QJzGk3kuo
                5fTvb/hV1GvYaE2z9Y3gwx9VXQ4KZ0S6jKf4d6BtJK+eXmn9XyOiOjIizgFJfbl6
                yP4pYOyHwZnGctzQDZ7RZ+v4Ul6EhrVX1vlz0f04Jq+XrmP9bhRt5GqgLbMsmnGj
                y3+1jqczVpHjfoM0E/wSd8JxRa5waeQpgjbnA7Rbs6Itzrx6oGZ+8lK9j7c9y5Ya
                1AavDY6sgXDNtKMWuMNEdwrjlZlj5Sh7XIsbF3b5tTSfiQOPjOqIu+m+dU+05GHc
                sPV8YPOHExu5eXkpB5aJX4RyN0u7XgPq9iOx4G9ejC0t5FrcwD8lbV1aEAYEoOQq
                ZwIDAQAB
                -----END PUBLIC KEY-----
                """;

        RSAPublicKey publicKey = SecurityUtils.readX509PublicKey(pem);

        assertNotNull(publicKey);
        assertEquals("RSA", publicKey.getAlgorithm());
        assertTrue(publicKey.getModulus().bitLength() >= 2048);
    }

    @Test
    void testReadX509PublicKey_InvalidPEM_ThrowsException() {
        String invalidPem = "-----BEGIN PUBLIC KEY-----\nINVALIDKEYDATA\n-----END PUBLIC KEY-----";

        assertThrows(Exception.class, () -> SecurityUtils.readX509PublicKey(invalidPem));
    }
}