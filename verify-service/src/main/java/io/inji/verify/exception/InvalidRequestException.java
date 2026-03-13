package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class InvalidRequestException extends RuntimeException {
    private static final String message = ErrorCode.INVALID_REQUEST.getErrorMessage();

    public InvalidRequestException() {
        super(message);
    }
}