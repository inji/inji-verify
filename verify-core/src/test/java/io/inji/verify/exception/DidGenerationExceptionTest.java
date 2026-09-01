package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DidGenerationExceptionTest {
    @Test
    void shouldTestConstructor() {
        DidGenerationException exception = new DidGenerationException();
        assertEquals("Error while generating DID document. Please check the certificate and try again.", exception.getMessage());
    }
}