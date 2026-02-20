package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPSubmissionException extends RuntimeException {
    private static final String message = ErrorCode.VP_SUBMISSION_EXCEPTION.getErrorMessage();

    public VPSubmissionException() {
        super(message);
    }
}
