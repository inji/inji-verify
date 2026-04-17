package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import lombok.Getter;

@Getter
public class ClientIdNonceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ClientIdNonceException(ErrorCode errorCode) {
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;
    }
}
