package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenMatchingFailedExceptionTest {
    @Test
    void shouldTestConstructor() {
        TokenMatchingFailedException exception = new TokenMatchingFailedException();
        assertEquals("Verifiable presentation token matching failed", exception.getMessage());
    }

}