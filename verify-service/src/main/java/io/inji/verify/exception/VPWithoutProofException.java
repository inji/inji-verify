package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class VPWithoutProofException extends RuntimeException {
    private static final String message = ErrorCode.VP_WITHOUT_PROOF.getErrorMessage();

    public VPWithoutProofException() {
        super(message);
    }
}
