package io.inji.verify.config;

import io.inji.verify.dto.core.CredentialStatusErrorDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.mosip.vercred.vcverifier.exception.StatusCheckErrorCode;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlingConfigTest {

    @Test
    void customErrorAttributesDoesNotExposeRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/unsafe-path");

        Map<String, Object> attributes = new CustomErrorAttributes().getErrorAttributes(
                new ServletWebRequest(request), ErrorAttributeOptions.defaults());

        assertFalse(attributes.containsKey("path"));
    }

    @Test
    void exceptionHandlerReturnsAStandardCredentialStatusError() {
        CredentialStatusCheckException exception = new CredentialStatusCheckException(
                StatusCheckErrorCode.UNKNOWN_ERROR, "Status service unavailable");

        ResponseEntity<Object> response = new ExceptionHandlerConfig().handle(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        CredentialStatusErrorDto body = (CredentialStatusErrorDto) response.getBody();
        assertEquals(500, body.getStatus());
        assertTrue(body.getError().contains("Status service unavailable"));
    }
}
