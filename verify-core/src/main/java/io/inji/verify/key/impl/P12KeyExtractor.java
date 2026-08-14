package io.inji.verify.key.impl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.security.Security;

import io.inji.verify.key.Extractor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Component
public class P12KeyExtractor implements Extractor {

    private final String p12FilePath;
    private final String keysStorePassword;
    private final ResourceLoader resourceLoader;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public P12KeyExtractor(@Value("${inji.keystore.file.path}") String p12FilePath,
                          @Value("${inji.keystore.file.pass}") String keysStorePassword,
                          ResourceLoader resourceLoader) {
        this.p12FilePath = p12FilePath;
        this.keysStorePassword = keysStorePassword;
        this.resourceLoader = resourceLoader;
    }

    public KeyPair extractKeyPair() {

        try {
            Resource resource = resourceLoader.getResource(p12FilePath);
            KeyStore p12Keystore = KeyStore.getInstance("PKCS12");
            try (InputStream inputStream = resource.getInputStream()) {
                p12Keystore.load(inputStream, keysStorePassword.toCharArray());
            }

            String targetAlias = null;
            Enumeration<String> aliases = p12Keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (p12Keystore.isKeyEntry(alias)) {
                    X509Certificate cert = (X509Certificate) p12Keystore.getCertificate(alias);
                    if (cert != null) {
                        String publicKeyAlgorithm = cert.getPublicKey().getAlgorithm();
                        if (publicKeyAlgorithm.equals("Ed25519") || publicKeyAlgorithm.equals("EdDSA")) {
                            targetAlias = alias;
                            break;
                        }
                    }
                }
            }

            if (targetAlias == null) {
                throw new Exception("No EdDSA key entry found in the P12 file.");
            }

            PrivateKey privateKey = (PrivateKey) p12Keystore.getKey(targetAlias, keysStorePassword.toCharArray());
            if (privateKey == null) {
                throw new Exception("Could not extract private key for alias: " + targetAlias);
            }

            X509Certificate certificate = (X509Certificate) p12Keystore.getCertificate(targetAlias);
            if (certificate == null) {
                throw new Exception("Could not extract certificate for alias: " + targetAlias);
            }
            PublicKey publicKey = certificate.getPublicKey();

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
