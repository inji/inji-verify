package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import lombok.Getter;

@Getter
public class ResponseCodeException extends RuntimeException {
    private final ErrorCode errorCode;

    public ResponseCodeException(ErrorCode errorCode) {
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;
    }
}
