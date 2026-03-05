package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPSubmissionResponseCodeNotFoundException extends RuntimeException {
    private static final String message = ErrorCode.NO_VP_SUBMISSION_RESPONSE_CODE.getErrorMessage();

    public VPSubmissionResponseCodeNotFoundException() {
        super(message);
    }
}
