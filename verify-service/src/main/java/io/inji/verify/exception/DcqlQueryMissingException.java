package io.inji.verify.exception;

/**
 * Raised when an authorization request must expose {@code dcql_query} but none is present on stored details.
 */
public class DcqlQueryMissingException extends RuntimeException {

    public DcqlQueryMissingException(String message) {
        super(message);
    }
}
