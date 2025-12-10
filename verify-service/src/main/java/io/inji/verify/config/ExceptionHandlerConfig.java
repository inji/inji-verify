package io.inji.verify.config;

import io.inji.verify.exception.CredentialStatusCheckException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static io.inji.verify.utils.Utils.getResponseEntityForCredentialStatusException;

@ControllerAdvice
public class ExceptionHandlerConfig {

    @ExceptionHandler(CredentialStatusCheckException.class)
    public ResponseEntity<Object> handle(CredentialStatusCheckException ex, HttpServletRequest request) {
        return getResponseEntityForCredentialStatusException(ex, request);
    }
}