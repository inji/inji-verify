package io.inji.verify.key.impl;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
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

    @Override
    public KeyPair extractKeyPair() {
        try {
            KeyStore p12Keystore = loadKeystore();
            String targetAlias = findEdDsaKeyAlias(p12Keystore);

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

    @Override
    public X509Certificate[] extractCertificateChain() {
        try {
            KeyStore p12Keystore = loadKeystore();
            String targetAlias = findEdDsaKeyAlias(p12Keystore);

            Certificate[] chain = p12Keystore.getCertificateChain(targetAlias);
            if (chain == null || chain.length == 0) {
                throw new Exception("Could not extract certificate chain for alias: " + targetAlias);
            }

            X509Certificate[] x509Chain = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++) {
                x509Chain[i] = (X509Certificate) chain[i];
            }
            return x509Chain;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore loadKeystore() throws Exception {
        Resource resource = resourceLoader.getResource(p12FilePath);
        KeyStore p12Keystore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = resource.getInputStream()) {
            p12Keystore.load(inputStream, keysStorePassword.toCharArray());
        }
        return p12Keystore;
    }

    private String findEdDsaKeyAlias(KeyStore p12Keystore) throws Exception {
        Enumeration<String> aliases = p12Keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (p12Keystore.isKeyEntry(alias)) {
                X509Certificate cert = (X509Certificate) p12Keystore.getCertificate(alias);
                if (cert != null) {
                    String publicKeyAlgorithm = cert.getPublicKey().getAlgorithm();
                    if (publicKeyAlgorithm.equals("Ed25519") || publicKeyAlgorithm.equals("EdDSA")) {
                        return alias;
                    }
                }
            }
        }
        throw new Exception("No EdDSA key entry found in the P12 file.");
    }
}
