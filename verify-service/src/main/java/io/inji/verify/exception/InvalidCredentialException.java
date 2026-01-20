package io.inji.verify.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCredentialException extends RuntimeException {
    public InvalidCredentialException(String message, Throwable cause) {
        super(message, cause);
    }
}
