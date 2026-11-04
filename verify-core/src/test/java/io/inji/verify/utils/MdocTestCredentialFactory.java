package io.inji.verify.utils;

import com.upokecenter.cbor.CBORObject;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

final class MdocTestCredentialFactory {

    private static final String NAMESPACE = "org.iso.18013.5.1";
    private static final String DOC_TYPE = "org.iso.18013.5.1.mDL";

    private MdocTestCredentialFactory() {
    }

    static String create(Instant signed, Instant validFrom, Instant validUntil, String printedExpiry)
            throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509Certificate certificate = createCertificate(keyPair);

        CBORObject issuerSignedItem = CBORObject.FromObjectAndTag(
                CBORObject.NewMap()
                        .Add("digestID", 0)
                        .Add("random", new byte[16])
                        .Add("elementIdentifier", "expiry_date")
                        .Add("elementValue", printedExpiry)
                        .EncodeToBytes(),
                24);
        byte[] itemDigest = MessageDigest.getInstance("SHA-256").digest(issuerSignedItem.EncodeToBytes());
        CBORObject namespaces = CBORObject.NewMap().Add(NAMESPACE, CBORObject.NewArray().Add(issuerSignedItem));
        CBORObject valueDigests = CBORObject.NewMap()
                .Add(NAMESPACE, CBORObject.NewMap().Add(0, itemDigest));
        CBORObject validityInfo = CBORObject.NewMap()
                .Add("signed", taggedDate(signed))
                .Add("validFrom", taggedDate(validFrom))
                .Add("validUntil", taggedDate(validUntil));
        CBORObject mso = CBORObject.NewMap()
                .Add("version", "1.0")
                .Add("digestAlgorithm", "SHA-256")
                .Add("valueDigests", valueDigests)
                .Add("deviceKeyInfo", CBORObject.NewMap().Add("deviceKey", CBORObject.NewMap()))
                .Add("docType", DOC_TYPE)
                .Add("validityInfo", validityInfo);
        byte[] payload = CBORObject.FromObjectAndTag(mso.EncodeToBytes(), 24).EncodeToBytes();
        byte[] protectedHeader = CBORObject.NewMap().Add(1, -7).EncodeToBytes();
        byte[] signature = signCosePayload(keyPair, protectedHeader, payload);
        CBORObject issuerAuth = CBORObject.NewArray()
                .Add(protectedHeader)
                .Add(CBORObject.NewMap().Add(33, certificate.getEncoded()))
                .Add(payload)
                .Add(signature);
        CBORObject credential = CBORObject.NewMap()
                .Add("issuerAuth", issuerAuth)
                .Add("nameSpaces", namespaces);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(credential.EncodeToBytes());
    }

    static String tamperFirstElement(String credential) {
        CBORObject decoded = CBORObject.DecodeFromBytes(Base64.getUrlDecoder().decode(credential));
        CBORObject namespaces = CBORObject.NewMap();
        for (CBORObject namespace : decoded.get("nameSpaces").getKeys()) {
            CBORObject items = decoded.get("nameSpaces").get(namespace);
            CBORObject changedItems = CBORObject.NewArray();
            for (int index = 0; index < items.size(); index++) {
                CBORObject taggedItem = items.get(index);
                if (index == 0) {
                    CBORObject item = CBORObject.DecodeFromBytes(taggedItem.Untag().GetByteString());
                    CBORObject changedItem = CBORObject.NewMap();
                    for (CBORObject key : item.getKeys()) {
                        changedItem.Add(key, "elementValue".equals(key.AsString())
                                ? CBORObject.FromObject("tampered")
                                : item.get(key));
                    }
                    changedItems.Add(CBORObject.FromObjectAndTag(changedItem.EncodeToBytes(), 24));
                } else {
                    changedItems.Add(taggedItem);
                }
            }
            namespaces.Add(namespace, changedItems);
        }
        CBORObject tampered = CBORObject.NewMap()
                .Add("issuerAuth", decoded.get("issuerAuth"))
                .Add("nameSpaces", namespaces);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tampered.EncodeToBytes());
    }

    private static CBORObject taggedDate(Instant instant) {
        return CBORObject.FromObjectAndTag(instant.toString(), 0);
    }

    private static X509Certificate createCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.now();
        X500Principal principal = new X500Principal("CN=mdoc-test-issuer,C=US");
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                principal,
                BigInteger.valueOf(now.toEpochMilli()),
                Date.from(now.minusSeconds(365L * 24 * 60 * 60)),
                Date.from(now.plusSeconds(86400)),
                principal,
                keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    private static byte[] signCosePayload(KeyPair keyPair, byte[] protectedHeader, byte[] payload)
            throws GeneralSecurityException, IOException {
        byte[] signatureStructure = CBORObject.NewArray()
                .Add("Signature1")
                .Add(protectedHeader)
                .Add(new byte[0])
                .Add(payload)
                .EncodeToBytes();
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(signatureStructure);
        return derToCoseSignature(signer.sign());
    }

    private static byte[] derToCoseSignature(byte[] derSignature) throws IOException {
        ASN1Sequence sequence = ASN1Sequence.getInstance(ASN1Primitive.fromByteArray(derSignature));
        byte[] rawSignature = new byte[64];
        copyInteger(ASN1Integer.getInstance(sequence.getObjectAt(0)).getValue(), rawSignature, 0);
        copyInteger(ASN1Integer.getInstance(sequence.getObjectAt(1)).getValue(), rawSignature, 32);
        return rawSignature;
    }

    private static void copyInteger(BigInteger value, byte[] destination, int offset) {
        byte[] encoded = value.toByteArray();
        int sourceOffset = encoded.length > 32 && encoded[0] == 0 ? 1 : 0;
        int length = encoded.length - sourceOffset;
        System.arraycopy(encoded, sourceOffset, destination, offset + 32 - length, length);
    }
}
