package io.inji.verify.utils;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

public class SecurityUtils {

    private static final SecureRandom random = new SecureRandom();

    public static String generateNonce()
    {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : randomBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static RSAPublicKey readX509PublicKey(String pem) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PemReader pemReader = new PemReader(new StringReader(pem));
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
        return (RSAPublicKey) factory.generatePublic(pubKeySpec);

    }
}