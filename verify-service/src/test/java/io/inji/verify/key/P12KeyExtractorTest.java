package io.inji.verify.key;

import io.inji.verify.key.impl.P12KeyExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x500.X500Name;

class P12KeyExtractorTest {

    ResourceLoader resourceLoader;
    P12KeyExtractor extractor;

    @BeforeEach
    void setup() {
        resourceLoader = mock(ResourceLoader.class);
        extractor = new P12KeyExtractor("classpath:dummy.p12", "password", resourceLoader);
    }

    private X509Certificate generateSelfSignedCert(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 1000L * 60);
        Date notAfter = new Date(now + 1000L * 60 * 60);

        X500Name issuer = new X500Name("CN=Test");
        X500Name subject = new X500Name("CN=Test");

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                java.math.BigInteger.valueOf(now),
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("Ed25519").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

    @Test
    void extractKeyPair_ShouldReturnKeyPair() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

        X509Certificate cert = generateSelfSignedCert(keyPair);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "password".toCharArray());
        ks.setKeyEntry("alias", keyPair.getPrivate(), "password".toCharArray(), new X509Certificate[]{cert});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, "password".toCharArray());
        byte[] keystoreBytes = baos.toByteArray();

        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(keystoreBytes));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);

        KeyPair result = extractor.extractKeyPair();

        assertNotNull(result);
        assertTrue(
                result.getPrivate().getAlgorithm().equals("Ed25519")
                || result.getPrivate().getAlgorithm().equals("EdDSA")
        );
        assertTrue(
                result.getPublic().getAlgorithm().equals("Ed25519")
                || result.getPublic().getAlgorithm().equals("EdDSA")
        );

    }

    @Test
    void extractKeyPair_ShouldThrow_WhenNoEdDSAKeyEntryFound() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "password".toCharArray());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, "password".toCharArray());
        byte[] keystoreBytes = baos.toByteArray();

        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(keystoreBytes));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> extractor.extractKeyPair());

        assertTrue(exception.getCause().getMessage().contains("No EdDSA key entry"));
    }

    @Test
    void extractKeyPair_ShouldThrow_WhenPrivateKeyMissing() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(keyPair);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "password".toCharArray());

        ks.setCertificateEntry("alias", cert);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, "password".toCharArray());

        Resource resource = mock(Resource.class);
        when(resource.getInputStream())
                .thenReturn(new ByteArrayInputStream(baos.toByteArray()));
        when(resourceLoader.getResource(anyString()))
                .thenReturn(resource);

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> extractor.extractKeyPair());

        assertTrue(exception.getCause().getMessage()
                .contains("No EdDSA key entry"));
    }

    @Test
    void extractKeyPair_ShouldWrapException_WhenInputStreamFails() throws Exception {
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenThrow(new RuntimeException("Stream failure"));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> extractor.extractKeyPair());

        assertEquals("Stream failure", exception.getCause().getMessage());
    }
}
