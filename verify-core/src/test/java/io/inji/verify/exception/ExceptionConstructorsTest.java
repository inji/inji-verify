package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExceptionConstructorsTest {

    @Test
    void invalidTransactionIdExceptionUsesTheConfiguredErrorMessage() {
        InvalidTransactionIdException exception = new InvalidTransactionIdException();

        assertEquals(ErrorCode.INVALID_TRANSACTION_ID.getErrorMessage(), exception.getMessage());
    }

    @Test
    void vpAlreadySubmittedExceptionSupportsAllConstructors() {
        RuntimeException cause = new RuntimeException("cause");

        assertEquals("VP request has already been submitted", new VPAlreadySubmittedException().getMessage());
        assertEquals("already submitted", new VPAlreadySubmittedException("already submitted").getMessage());
        assertSame(cause, new VPAlreadySubmittedException("already submitted", cause).getCause());
        assertSame(cause, new VPAlreadySubmittedException(cause).getCause());
    }

    @Test
    void malformedCookieExceptionRetainsTheOriginalCauseAndMessage() {
        IllegalArgumentException cause = new IllegalArgumentException("Invalid cookie");
        MalformedCookieException exception = new MalformedCookieException(cause);

        assertEquals("Invalid cookie", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
