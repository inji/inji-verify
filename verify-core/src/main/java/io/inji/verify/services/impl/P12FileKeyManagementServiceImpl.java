package io.inji.verify.services.impl;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import io.inji.verify.services.KeyManagementService;
import io.inji.verify.key.impl.P12KeyExtractor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.EdECPrivateKey;
import java.security.spec.X509EncodedKeySpec;

@Service
@Slf4j
public class P12FileKeyManagementServiceImpl implements KeyManagementService<OctetKeyPair> {

    P12KeyExtractor extractor;

    private KeyPair ed25519KeyPair;

    public P12FileKeyManagementServiceImpl(P12KeyExtractor extractor) {
        this.extractor = extractor;
    }

    @PostConstruct
    public void loadKey() {
        this.ed25519KeyPair = extractor.extractKeyPair();
    }

    @Override
    public OctetKeyPair getKeyPair() {
        if (ed25519KeyPair == null) {
            throw new IllegalStateException("Ed25519 Key not loaded.");
        }

        try {
            EdECPrivateKey edECPrivateKey = (EdECPrivateKey) ed25519KeyPair.getPrivate();
            X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(ed25519KeyPair.getPublic().getEncoded());
            BCEdDSAPublicKey bcEdDSAPublicKey = (BCEdDSAPublicKey) KeyFactory.getInstance("EdDSA", "BC").generatePublic(pkSpec);

            byte[] privateKeyBytes = edECPrivateKey.getBytes().orElseThrow(() -> new IllegalStateException("Could not get private key"));
            byte[] publicKeyXBytes = bcEdDSAPublicKey.getPointEncoding();

            Base64URL base64UrlPrivateKeyD = Base64URL.encode(privateKeyBytes);
            Base64URL base64UrlPublicKeyX = Base64URL.encode(publicKeyXBytes);

            return new OctetKeyPair.Builder(Curve.Ed25519, base64UrlPublicKeyX)
                    .d(base64UrlPrivateKeyD)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
        } catch (Exception e) {
            log.error("Error generating KeyPair: {}", e.getMessage());
            return null;
        }
    }
}