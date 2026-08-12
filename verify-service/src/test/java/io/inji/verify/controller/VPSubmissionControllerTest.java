package io.inji.verify.controller;

import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.RedirectUriGenerationException;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The submission flow itself lives in
 * {@link VerifiablePresentationSubmissionService#submitVerifiablePresentation} (see
 * VerifiablePresentationSubmissionServiceImplTest for the detailed business-rule cases and the
 * full-flow orchestration tests). This controller does exactly two things: rejects unknown form
 * parameters (an HTTP transport-shape concern, kept here rather than in the service), and
 * translates each exception type thrown by the service call into the right HTTP response.
 */
@ExtendWith(MockitoExtension.class)
class VPSubmissionControllerTest {

    @Mock
    private VerifiablePresentationSubmissionService vpSubmissionService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VPSubmissionController controller;

    private static final String STATE = "state-123";
    private static final String VALID_VP_TOKEN = "{\"query1\":[{\"type\":\"VerifiablePresentation\"}]}";

    @BeforeEach
    void setup() {
        Map<String, String[]> params = new HashMap<>();
        params.put("state", new String[]{STATE});
        params.put("vp_token", new String[]{VALID_VP_TOKEN});
        when(request.getParameterMap()).thenReturn(params);
    }

    // ---- unknown parameter check (stays in the controller) ----

    @Test
    void shouldReturnBadRequest_whenUnknownParameterPresent() {
        Map<String, String[]> params = new HashMap<>();
        params.put("state", new String[]{STATE});
        params.put("unexpected", new String[]{"x"});
        when(request.getParameterMap()).thenReturn(params);

        ResponseEntity<?> response = controller.submitVP(null, STATE, "access_denied", null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.UNKNOWN_PARAMETER.getErrorCode(), body.getErrorCode());
        assertTrue(body.getErrorMessage().contains("unexpected"));
        verify(vpSubmissionService, never()).submitVerifiablePresentation(any(), any(), any(), any());
    }

    @Test
    void shouldCallService_whenOnlyAllowedParametersPresent() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenReturn(new HashMap<>());

        controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        verify(vpSubmissionService).submitVerifiablePresentation(VALID_VP_TOKEN, STATE, null, null);
    }

    // ---- success paths ----

    @Test
    void shouldReturnSuccess_withEmptyBody_whenNoResponseCode() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of(), response.getBody());
    }

    @Test
    void shouldReturnSuccess_withRedirectUri_whenResponseCodeGenerated() {
        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("redirect_uri", "https://example.com/cb?response_code=resp-code-123");
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenReturn(serviceResponse);

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("https://example.com/cb?response_code=resp-code-123", body.get("redirect_uri"));
    }

    @Test
    void shouldReturnSuccess_forErrorOnlySubmission() {
        Map<String, String[]> params = new HashMap<>();
        params.put("state", new String[]{STATE});
        params.put("error", new String[]{"access_denied"});
        params.put("error_description", new String[]{"user cancelled"});
        when(request.getParameterMap()).thenReturn(params);
        when(vpSubmissionService.submitVerifiablePresentation(isNull(), eq(STATE), eq("access_denied"), eq("user cancelled")))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = controller.submitVP(null, STATE, "access_denied", "user cancelled", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- exception mapping ----

    @Test
    void shouldReturnBadRequest_whenServiceThrowsVPRequestValidationException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenThrow(new VPRequestValidationException(ErrorCode.VP_REQUEST_EXPIRED));

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_REQUEST_EXPIRED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnInternalServerError_whenServiceThrowsRedirectUriGenerationException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenThrow(new RedirectUriGenerationException());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.REDIRECT_URI_NOT_FOUND.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsVPAlreadySubmittedException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenThrow(new VPAlreadySubmittedException());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsInvalidVpTokenException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any()))
                .thenThrow(new InvalidVpTokenException("bad structure"));

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals("invalid_vp_token", body.getErrorCode());
        assertTrue(body.getErrorMessage().contains("bad structure"));
    }
}
