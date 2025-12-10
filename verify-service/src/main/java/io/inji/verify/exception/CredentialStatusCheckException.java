package io.inji.verify.exception;

import io.mosip.vercred.vcverifier.exception.StatusCheckErrorCode;
import lombok.Getter;

@Getter
public class CredentialStatusCheckException extends Exception {
    private final StatusCheckErrorCode errorCode;
    private final String errorDescription;

    public CredentialStatusCheckException(StatusCheckErrorCode errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
}
