package io.inji.verify.services;

import java.util.Map;

import io.inji.verify.exception.DidGenerationException;

public interface DidService {

    /**
     * Generates the did:web DID document for this verifier from its configured key material and
     * DID/key URIs. This is the single entry point a caller — this service's own controller, or a
     * consumer embedding this service directly to publish its own did:web identity — needs to
     * call.
     *
     * @throws DidGenerationException if the key pair cannot be extracted, or the DID document
     *                                cannot be built from it
     */
    Map<String, Object> generateDidDocument() throws DidGenerationException;
}
