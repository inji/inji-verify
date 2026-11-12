package io.inji.testrig.apirig.injiverify.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

public final class SdJwtVpUtil {

    private SdJwtVpUtil() {
    }

    public static String build(
            String mode,
            String vct,
            String nonce,
            String audience) {

        try {
            ECKey issuer =
                    new ECKeyGenerator(Curve.P_256)
                            .generate();

            ECKey holder =
                    new ECKeyGenerator(Curve.P_256)
                            .generate();

            String[][] claims;

            if ("eu.europa.ec.eudi.msisdn.1".equals(vct)) {
                claims = new String[][] {
                        { "registered_family_name", "Musterman" },
                        { "phone_number", "4915111234567" },
                        { "verification_method_information", "NumberVerify" }
                };
            } else {
                claims = new String[][] {
                        { "given_name", "TEST_GIVEN" },
                        { "family_name", "TEST_FAMILY" },
                        { "birthdate", "1992-04-15" },
                        { "email", "test@mosip.net" }
                };
            }

            List<String> disclosures =
                    new ArrayList<>();

            JSONArray sd =
                    new JSONArray();

            for (String[] claim : claims) {

                String disclosure =
                        b64(
                                new JSONArray()
                                        .put(randomSalt())
                                        .put(claim[0])
                                        .put(claim[1])
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8));

                disclosures.add(disclosure);

                sd.put(
                        b64(
                                sha256(
                                        disclosure.getBytes(
                                                StandardCharsets.US_ASCII))));
            }

            long now =
                    System.currentTimeMillis() / 1000;

            JSONObject payload =
                    new JSONObject();

            payload.put(
                    "iss",
                    "https://issuer.test.example.com");

            payload.put("iat", now);

            payload.put(
                    "exp",
                    mode.equals("EXPIRED")
                            ? now - 86400
                            : now + 365L * 86400);

            payload.put("vct", vct);
            payload.put("_sd_alg", "sha-256");
            payload.put("_sd", sd);

            if (!mode.equals("MISSING_CNF")) {
                payload.put(
                        "cnf",
                        new JSONObject()
                                .put(
                                        "jwk",
                                        new JSONObject(
                                                holder.toPublicJWK()
                                                        .toJSONString())));
            }

            JWSHeader header =
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .type(new JOSEObjectType("dc+sd-jwt"))
                            .x509CertChain(
                                    List.of(
                                            com.nimbusds.jose.util.Base64
                                                    .encode(
                                                            selfSignedCert(issuer)
                                                                    .getEncoded())))
                            .build();

            JWSObject jws =
                    new JWSObject(
                            header,
                            new Payload(payload.toString()));

            jws.sign(new ECDSASigner(issuer));

            String issuerJwt =
                    jws.serialize();

            if (mode.equals("INVALID_SIGNATURE")) {
                issuerJwt =
                        issuerJwt.substring(
                                0,
                                issuerJwt.length() - 1)
                                + (issuerJwt.endsWith("A")
                                        ? "B"
                                        : "A");
            }

            StringBuilder sdJwt =
                    new StringBuilder(issuerJwt)
                            .append('~');

            for (String disclosure : disclosures) {
                sdJwt.append(disclosure)
                        .append('~');
            }

            if (mode.equals("MISSING_KB")) {
                return sdJwt.toString();
            }

            JSONObject kbPayload =
                    new JSONObject();

            kbPayload.put("iat", now);

            if (!mode.equals("MISSING_AUD")) {
                kbPayload.put(
                        "aud",
                        mode.equals("WRONG_DOMAIN")
                                ? "https://evil.example.com/"
                                : audience);
            }

            if (!mode.equals("MISSING_NONCE")) {
                kbPayload.put(
                        "nonce",
                        mode.equals("WRONG_NONCE")
                                ? "wrongNonce123"
                                : nonce);
            }

            kbPayload.put(
                    "sd_hash",
                    b64(
                            sha256(
                                    sdJwt.toString()
                                            .getBytes(
                                                    StandardCharsets.US_ASCII))));

            JWSObject kb =
                    new JWSObject(
                            new JWSHeader.Builder(
                                    JWSAlgorithm.ES256)
                                    .type(new JOSEObjectType("kb+jwt"))
                                    .build(),
                            new Payload(kbPayload.toString()));

            kb.sign(new ECDSASigner(holder));

            return sdJwt
                    .append(kb.serialize())
                    .toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to build SD-JWT VP",
                    e);
        }
    }

    private static X509Certificate selfSignedCert(
            ECKey key)
            throws Exception {

        X500Name name =
                new X500Name(
                        "CN=vc-verifier-test-issuer,O=MOSIP-TEST,C=IN");

        Date from =
                new Date(
                        System.currentTimeMillis()
                                - 86400000L);

        Date to =
                new Date(
                        System.currentTimeMillis()
                                + 365L * 86400000L);

        java.security.KeyPair keyPair =
                key.toKeyPair();

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        name,
                        BigInteger.valueOf(
                                System.currentTimeMillis()),
                        from,
                        to,
                        name,
                        keyPair.getPublic());

        ContentSigner signer =
                new JcaContentSignerBuilder(
                        "SHA256withECDSA")
                        .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
                .getCertificate(
                        builder.build(signer));
    }

    private static String randomSalt() {

        byte[] salt =
                new byte[16];

        new java.security.SecureRandom()
                .nextBytes(salt);

        return b64(salt);
    }

    private static byte[] sha256(byte[] input)
            throws Exception {

        return MessageDigest
                .getInstance("SHA-256")
                .digest(input);
    }

    private static String b64(byte[] input) {

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input);
    }
}