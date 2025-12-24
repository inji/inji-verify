package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VPWithoutProofExceptionTest {
    @Test
    void shouldTestConstructor() {
        VPWithoutProofException exception = new VPWithoutProofException();
        assertEquals("Invalid VP Submission since VP is without proof", exception.getMessage());
    }
}