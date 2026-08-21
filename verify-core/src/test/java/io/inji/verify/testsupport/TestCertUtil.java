package io.inji.verify.testsupport;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Shared test helper for minting throwaway self-signed X.509 certificates, used by tests
 * that exercise keystore/x5c/x509_san_dns code paths without needing a real PKCS12 fixture on
 * disk.
 */
public final class TestCertUtil {

    private TestCertUtil() {
    }

    public static X509Certificate generateSelfSignedCert(KeyPair keyPair) throws Exception {
        return generateSelfSignedCert(keyPair, null);
    }

    /**
     * @param sanDnsName if non-null, adds a Subject Alternative Name dNSName extension —
     *                   needed to exercise the x509_san_dns client_id scheme, which requires the
     *                   claimed DNS name to be present in the signing cert's SAN.
     */
    public static X509Certificate generateSelfSignedCert(KeyPair keyPair, String sanDnsName) throws Exception {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 1000L * 60);
        Date notAfter = new Date(now + 1000L * 60 * 60);
        return generateSelfSignedCert(keyPair, sanDnsName, notBefore, notAfter);
    }

    /**
     * @param notBefore / notAfter validity window — pass a window entirely in the past or future
     *                   to mint an expired / not-yet-valid cert for exercising expiry validation.
     */
    public static X509Certificate generateSelfSignedCert(
            KeyPair keyPair, String sanDnsName, Date notBefore, Date notAfter) throws Exception {

        X500Name issuer = new X500Name("CN=Test");
        X500Name subject = new X500Name("CN=Test");

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        if (sanDnsName != null) {
            GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, sanDnsName));
            certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
        }

        ContentSigner signer = new JcaContentSignerBuilder("Ed25519").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }
}
