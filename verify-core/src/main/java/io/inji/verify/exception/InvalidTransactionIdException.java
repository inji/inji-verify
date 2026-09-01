package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

/**
 * Thrown when a transactionId does not resolve to any VP request (i.e.
 * {@code getLatestRequestIdFor} returns an empty list), and — for the plain v1 result lookup —
 * there is no matching VC submission either.
 */
public class InvalidTransactionIdException extends RuntimeException {
    private static final String message = ErrorCode.INVALID_TRANSACTION_ID.getErrorMessage();

    public InvalidTransactionIdException() {
        super(message);
    }
}
