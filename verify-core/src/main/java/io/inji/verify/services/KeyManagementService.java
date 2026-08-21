package io.inji.verify.services;

import java.security.cert.X509Certificate;

public interface KeyManagementService<T> {
    T getKeyPair();

    /**
     * Signing key's X.509 certificate chain (leaf-first), used to populate the {@code x5c}
     * JWT header. Default throws, since not every KeyManagementService backing (e.g. a future
     * non-certificate-based key source) is guaranteed to have one.
     */
    default X509Certificate[] getCertificateChain() {
        throw new UnsupportedOperationException("This KeyManagementService does not provide a certificate chain.");
    }
}
