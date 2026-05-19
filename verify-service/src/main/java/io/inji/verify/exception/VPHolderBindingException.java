package io.inji.verify.exception;

import lombok.Getter;

@Getter
public class VPHolderBindingException extends RuntimeException {

    private final String errorCode;
    private final String errorDescription;

    public VPHolderBindingException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription =  errorDescription;
    }

}
