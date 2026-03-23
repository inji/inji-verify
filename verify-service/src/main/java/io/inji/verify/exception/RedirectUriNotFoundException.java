package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class RedirectUriNotFoundException extends RuntimeException {
    private static final String message = ErrorCode.REDIRECT_URI_NOT_FOUND.getErrorMessage();

    public RedirectUriNotFoundException() {
        super(message);
    }
}
