package io.inji.verify.config;

import io.inji.verify.dto.core.CredentialStatusErrorDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static io.inji.verify.utils.Utils.buildCredentialStatusErrorDto;

@ControllerAdvice
public class ExceptionHandlerConfig {

    @ExceptionHandler(CredentialStatusCheckException.class)
    public ResponseEntity<Object> handle(CredentialStatusCheckException ex) {
        CredentialStatusErrorDto errorDto = buildCredentialStatusErrorDto(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}
