package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPWithoutProofException extends Exception {
    private static final String message = ErrorCode.NO_VP_SUBMISSION.getErrorMessage();

    public VPWithoutProofException() {
        super(message);
    }
}
