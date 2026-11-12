package io.inji.testrig.apirig.injiverify.utils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;

import foundation.identity.jsonld.ConfigurableDocumentLoader;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.canonicalizer.URDNA2015Canonicalizer;

public final class PresentationVpUtil {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private PresentationVpUtil() {
    }

    public static JSONObject buildLdpVp(
            JSONObject holderJwk,
            JSONObject vc,
            String nonce,
            String domain) {

        try {
            OctetKeyPair key =
                    OctetKeyPair.parse(holderJwk.toString());

            String did =
                    "did:jwk:"
                            + Base64.getUrlEncoder()
                                    .withoutPadding()
                                    .encodeToString(
                                            key.toPublicJWK()
                                                    .toJSONString()
                                                    .getBytes(StandardCharsets.UTF_8))
                            + "#0";

            JSONObject vp = new JSONObject();

            vp.put(
                    "@context",
                    new JSONArray()
                            .put("https://www.w3.org/2018/credentials/v1")
                            .put("https://w3id.org/security/suites/jws-2020/v1"));

            vp.put(
                    "type",
                    new JSONArray()
                            .put("VerifiablePresentation"));

            vp.put(
                    "id",
                    "urn:uuid:" + UUID.randomUUID());

            vp.put("holder", did);

            vp.put(
                    "verifiableCredential",
                    new JSONArray().put(vc));

            JSONObject proof = new JSONObject();

            proof.put(
                    "type",
                    "JsonWebSignature2020");

            SimpleDateFormat utcFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.US);

            utcFormat.setTimeZone(
                    TimeZone.getTimeZone("UTC"));

            proof.put(
                    "created",
                    utcFormat.format(new Date()));

            proof.put("challenge", nonce);
            proof.put("domain", domain);
            proof.put("proofPurpose", "authentication");
            proof.put("verificationMethod", did);

            vp.put("proof", proof);

            proof.put(
                    "jws",
                    sign(key, canonicalize(vp)));

            return vp;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to build LDP VP",
                    e);
        }
    }

    public static String didJwk(JSONObject jwk)
            throws Exception {

        return "did:jwk:"
                + Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                OctetKeyPair
                                        .parse(jwk.toString())
                                        .toPublicJWK()
                                        .toJSONString()
                                        .getBytes(StandardCharsets.UTF_8));
    }

    public static JSONObject issueLdpVc(
            JSONObject issuerJwk,
            JSONObject holderJwk,
            JSONObject claimsTemplate,
            String expirationDate) {

        try {
            OctetKeyPair issuerKey =
                    OctetKeyPair.parse(issuerJwk.toString());

            String issuerDid =
                    didJwk(issuerJwk);

            JSONObject vc =
                    new JSONObject(claimsTemplate.toString());

            vc.remove("proof");

            vc.put(
                    "@context",
                    new JSONArray()
                            .put("https://www.w3.org/2018/credentials/v1")
                            .put("https://inji.github.io/inji-config/contexts/mosip-identity-context.json")
                            .put("https://w3id.org/security/suites/jws-2020/v1"));

            vc.put(
                    "id",
                    "urn:uuid:" + UUID.randomUUID());

            vc.put("issuer", issuerDid);

            SimpleDateFormat utcFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.US);

            utcFormat.setTimeZone(
                    TimeZone.getTimeZone("UTC"));

            String now =
                    utcFormat.format(new Date());

            vc.put("issuanceDate", now);

            vc.put(
                    "expirationDate",
                    expirationDate != null
                            ? expirationDate
                            : "2099-01-01T00:00:00Z");

            vc.getJSONObject("credentialSubject")
                    .put("id", didJwk(holderJwk));

            JSONObject proof = new JSONObject();

            proof.put(
                    "type",
                    "JsonWebSignature2020");

            proof.put("created", now);

            proof.put(
                    "proofPurpose",
                    "assertionMethod");

            proof.put(
                    "verificationMethod",
                    issuerDid + "#0");

            vc.put("proof", proof);

            proof.put(
                    "jws",
                    sign(issuerKey, canonicalize(vc)));

            return vc;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to issue LDP VC",
                    e);
        }
    }

    private static byte[] canonicalize(JSONObject json)
            throws Exception {

        Map<String, Object> map =
                MAPPER.readValue(
                        json.toString(),
                        new TypeReference<Map<String, Object>>() {
                        });

        JsonLDObject ld =
                JsonLDObject.fromJsonObject(map);

        ConfigurableDocumentLoader loader =
                new ConfigurableDocumentLoader();

        loader.setEnableHttps(true);
        loader.setEnableHttp(true);
        loader.setEnableFile(false);

        ld.setDocumentLoader(loader);

        return new URDNA2015Canonicalizer()
                .canonicalize(
                        LdProof.getFromJsonLDObject(ld),
                        ld);
    }

    private static String sign(
            OctetKeyPair key,
            byte[] payload)
            throws Exception {

        JWSHeader header =
                new JWSHeader.Builder(
                        JWSAlgorithm.EdDSA)
                        .base64URLEncodePayload(false)
                        .criticalParams(Set.of("b64"))
                        .build();

        byte[] headerBase64 =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                header.toString()
                                        .getBytes(StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8);

        byte[] signingInput =
                new byte[
                        headerBase64.length
                                + 1
                                + payload.length];

        System.arraycopy(
                headerBase64,
                0,
                signingInput,
                0,
                headerBase64.length);

        signingInput[headerBase64.length] = '.';

        System.arraycopy(
                payload,
                0,
                signingInput,
                headerBase64.length + 1,
                payload.length);

        Base64URL signature =
                new Ed25519Signer(key)
                        .sign(header, signingInput);

        return new String(
                headerBase64,
                StandardCharsets.UTF_8)
                + ".."
                + signature;
    }
}