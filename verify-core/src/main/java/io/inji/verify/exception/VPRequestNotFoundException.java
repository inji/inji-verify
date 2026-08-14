package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPRequestNotFoundException extends Exception {
    private static final String message = ErrorCode.NO_AUTH_REQUEST.getErrorMessage();

    public VPRequestNotFoundException() {
        super(message);
    }
}