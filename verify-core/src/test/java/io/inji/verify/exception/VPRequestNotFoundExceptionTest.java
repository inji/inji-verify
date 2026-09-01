package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VPRequestNotFoundExceptionTest {
    @Test
    void shouldTestConstructor() {
        VPRequestNotFoundException exception = new VPRequestNotFoundException();
        assertEquals("No Authorization request found for given request ID.", exception.getMessage());
    }
}