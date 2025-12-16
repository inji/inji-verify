package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VPWithoutProofExceptionTest {
    @Test
    void shouldTestConstructor() {
        VPWithoutProofException exception = new VPWithoutProofException();
        assertEquals("VP without proof, please provide a signed VP", exception.getMessage());
    }
}