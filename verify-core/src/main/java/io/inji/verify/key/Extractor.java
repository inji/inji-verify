package io.inji.verify.key;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public interface Extractor {
    KeyPair extractKeyPair();

    /**
     * The signing key's X.509 certificate chain (leaf-first), as stored alongside the key
     * entry in the keystore. Used to populate the {@code x5c} JWT header for requests whose
     * {@code client_id} uses the {@code x509_san_dns} scheme.
     */
    X509Certificate[] extractCertificateChain();
}
