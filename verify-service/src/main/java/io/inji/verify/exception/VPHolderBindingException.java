package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class VPHolderBindingException extends RuntimeException {

    private String errorCode;
    private String errorDescription;
    public VPHolderBindingException(String errorCode, String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription =  errorDescription;
    }

}
