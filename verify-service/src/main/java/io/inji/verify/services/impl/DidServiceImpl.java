package io.inji.verify.services.impl;

import java.security.KeyPair;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.inji.verify.exception.DidGenerationException;
import io.inji.verify.key.Extractor;
import io.inji.verify.services.DidService;
import io.inji.verify.utils.DIDDocumentUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DidServiceImpl implements DidService {

    @Value("${inji.did.verify.uri}")
    String verifyDidURI;

    @Value("${inji.did.verify.public.key.uri}")
    String verifyPublicKeyURI;

    final Extractor extractor;

    public DidServiceImpl(Extractor extractor) {
        this.extractor = extractor;
    }

    @Override
    public Map<String, Object> generateDidDocument() throws DidGenerationException {
        try {
            KeyPair keyPair = extractor.extractKeyPair();
            return DIDDocumentUtil.generateDIDDocument(keyPair.getPublic(), verifyDidURI, verifyPublicKeyURI);
        } catch (DidGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate DID document", e);
            throw new DidGenerationException(e);
        }
    }
}
