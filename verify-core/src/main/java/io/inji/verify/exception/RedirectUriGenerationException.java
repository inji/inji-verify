package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

/**
 * Thrown when a redirect_uri cannot be built for a VP submission that requires one
 * (i.e. {@code inji.verify.redirect-uri} is missing/blank while a response code was
 * generated). This is a server configuration problem, not a client input validation
 * failure, so it is mapped to HTTP 500 rather than the generic 400 validation handler.
 */
public class RedirectUriGenerationException extends RuntimeException {

    public RedirectUriGenerationException() {
        super(ErrorCode.REDIRECT_URI_NOT_FOUND.getErrorMessage());
    }
}
