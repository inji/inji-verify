package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JWTCreationExceptionTest {
    @Test
    void shouldTestConstructor() {
        JWTCreationException exception = new JWTCreationException();
        assertEquals("Error while creating JWT. Please check the input and try again.", exception.getMessage());
    }
}