package io.inji.verify.utils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import io.inji.verify.exception.DidGenerationException;

import io.ipfs.multibase.Multibase;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;

@Slf4j
public class DIDDocumentUtil {

    private static final String MULTICODEC_PREFIX = "ed01";

    public static Map<String, Object> generateDIDDocument(PublicKey publicKey, String verifyDidURI, String verifyPublicKeyURI) {

        HashMap<String, Object> didDocument = new HashMap<>();
        didDocument.put("@context", Collections.singletonList("https://www.w3.org/ns/did/v1"));
        didDocument.put("alsoKnownAs", new ArrayList<>());
        didDocument.put("service", new ArrayList<>());
        didDocument.put("id", verifyDidURI);
        didDocument.put("authentication", Collections.singletonList(verifyPublicKeyURI));
        didDocument.put("assertionMethod", Collections.singletonList(verifyPublicKeyURI));

        Map<String, Object> verificationMethod;
        try {
            verificationMethod = generateEd25519VerificationMethod(publicKey, verifyDidURI, verifyPublicKeyURI);
        } catch (Exception e) {
            log.error("Exception occurred while generating verification method for given certificate : {}", e.getMessage());
            throw new DidGenerationException();
        }

        didDocument.put("verificationMethod", Collections.singletonList(verificationMethod));
        return didDocument;
    }

    private static Map<String, Object> generateEd25519VerificationMethod(PublicKey publicKey, String verifyDidURI, String verifyPublicKeyURI) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(publicKey.getEncoded());
        BCEdDSAPublicKey bcEdDSAPublicKey = (BCEdDSAPublicKey) KeyFactory.getInstance("EdDSA", "BC").generatePublic(pkSpec);

        byte[] rawBytes = bcEdDSAPublicKey.getPointEncoding();
        byte[] multicodecBytes = HexFormat.of().parseHex(MULTICODEC_PREFIX);
        byte[] finalBytes = new byte[multicodecBytes.length + rawBytes.length];
        System.arraycopy(multicodecBytes, 0, finalBytes, 0, multicodecBytes.length);
        System.arraycopy(rawBytes, 0, finalBytes, multicodecBytes.length, rawBytes.length);
        String publicKeyMultibase = Multibase.encode(Multibase.Base.Base58BTC, finalBytes);

        Map<String, Object> verificationMethod = new HashMap<>();
        verificationMethod.put("id", verifyPublicKeyURI);
        verificationMethod.put("type", "Ed25519VerificationKey2020");
        verificationMethod.put("@context", "https://w3id.org/security/suites/ed25519-2020/v1");
        verificationMethod.put("controller", verifyDidURI);
        verificationMethod.put("publicKeyMultibase", publicKeyMultibase);
        return verificationMethod;
    }
}