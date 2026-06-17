package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPVerificationException extends RuntimeException {
    private static final String message = ErrorCode.VP_VERIFICATION_FAILED.getErrorMessage();

    public VPVerificationException() {
        super(message);
    }

    public VPVerificationException(Throwable cause) {
        super(message, cause);
    }
}
