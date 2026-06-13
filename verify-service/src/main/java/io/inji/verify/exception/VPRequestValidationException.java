package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import lombok.Getter;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Getter
public class VPRequestValidationException extends RuntimeException {

    private final ErrorCode errorCode;

    /** Short constructor — message is the enum's fixed error message. */
    public VPRequestValidationException(ErrorCode errorCode) {
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;
    }

    /** Detailed constructor — message is used verbatim. */
    public VPRequestValidationException(ErrorCode errorCode, String detailedMessage) {
        super(detailedMessage);
        this.errorCode = errorCode;
    }

    /**
     * Returns a new exception with the credential query ID prepended to the message, e.g.:
     * {@code [credential_id: my_cred] <original message>}
     */
    public VPRequestValidationException withCredentialId(String credentialId) {
        return new VPRequestValidationException(
                this.errorCode,
                "[credential_id: " + credentialId + "] " + this.getMessage());
    }

    public static VPRequestValidationException from(MethodArgumentNotValidException ex) {
        ErrorCode errorCode = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(VPRequestValidationException::toErrorCode)
                .orElse(ErrorCode.INVALID_REQUEST_FORMAT);
        return new VPRequestValidationException(errorCode);
    }

    private static ErrorCode toErrorCode(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (message == null || message.isBlank()) {
            return ErrorCode.INVALID_REQUEST_FORMAT;
        }
        try {
            return ErrorCode.valueOf(message);
        } catch (IllegalArgumentException ignored) {
            return ErrorCode.INVALID_REQUEST_FORMAT;
        }
    }
}
