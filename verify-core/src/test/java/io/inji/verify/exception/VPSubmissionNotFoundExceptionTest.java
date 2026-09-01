package io.inji.verify.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VPSubmissionNotFoundExceptionTest {

    @Test
    void shouldTestConstructor() {
        VPSubmissionNotFoundException exception = new VPSubmissionNotFoundException();
        assertEquals("No VP submission found for given transaction ID.", exception.getMessage());
    }

}