package io.inji.verify.controller;

import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.submission.DcApiVpSubmissionRequestDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.RedirectUriGenerationException;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
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
        verify(vpSubmissionService, never()).submitVerifiablePresentation(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCallService_whenOnlyAllowedParametersPresent() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenReturn(new HashMap<>());

        controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        verify(vpSubmissionService).submitVerifiablePresentation(
                eq(VALID_VP_TOKEN), eq(STATE), isNull(), isNull(), eq(java.util.Optional.empty()));
    }

    // ---- success paths ----

    @Test
    void shouldReturnSuccess_withEmptyBody_whenNoResponseCode() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of(), response.getBody());
    }

    @Test
    void shouldReturnSuccess_withRedirectUri_whenResponseCodeGenerated() {
        Map<String, Object> serviceResponse = new HashMap<>();
        serviceResponse.put("redirect_uri", "https://example.com/cb?response_code=resp-code-123");
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
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
        when(vpSubmissionService.submitVerifiablePresentation(
                isNull(), eq(STATE), eq("access_denied"), eq("user cancelled"), eq(java.util.Optional.empty())))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = controller.submitVP(null, STATE, "access_denied", "user cancelled", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vpSubmissionService).submitVerifiablePresentation(
                isNull(), eq(STATE), eq("access_denied"), eq("user cancelled"), eq(java.util.Optional.empty()));
    }

    // ---- exception mapping ----

    @Test
    void shouldReturnBadRequest_whenServiceThrowsVPRequestValidationException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new VPRequestValidationException(ErrorCode.VP_REQUEST_EXPIRED));

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_REQUEST_EXPIRED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnInternalServerError_whenServiceThrowsRedirectUriGenerationException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new RedirectUriGenerationException());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.REDIRECT_URI_NOT_FOUND.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsVPAlreadySubmittedException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new VPAlreadySubmittedException());

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenServiceThrowsInvalidVpTokenException() {
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidVpTokenException("bad structure"));

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorCode(), body.getErrorCode());
        assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorMessage(), body.getErrorMessage());
        assertFalse(body.getErrorMessage().contains("bad structure"));
    }

    // ---- DC API JSON endpoint ----

    @Test
    void shouldReturnBadRequest_whenDcApiRequestIdMissing() {
        DcApiVpSubmissionRequestDto body = new DcApiVpSubmissionRequestDto();
        body.setVpToken(new ObjectMapper().createObjectNode().put("x", "y"));

        ResponseEntity<?> response = controller.submitVpDcApi(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto error = (ErrorDto) response.getBody();
        assertNotNull(error);
        assertEquals(ErrorCode.INVALID_REQUEST_ID_MISSING.getErrorCode(), error.getErrorCode());
        verify(vpSubmissionService, never()).submitVerifiablePresentation(any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnAccepted_whenDcApiSubmissionSucceeds() {
        DcApiVpSubmissionRequestDto body = new DcApiVpSubmissionRequestDto();
        body.setRequestId(STATE);
        body.setVpToken(new ObjectMapper().createObjectNode().putArray("query1").addObject().put("type", "VerifiablePresentation"));
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenReturn(new HashMap<>());

        ResponseEntity<?> response = controller.submitVpDcApi(body, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(vpSubmissionService).submitVerifiablePresentation(
                eq(body.getVpToken().toString()), eq(STATE), isNull(), isNull(), any(java.util.Optional.class));
    }

    @Test
    void shouldMapValidationException_whenDcApiServiceRejects() {
        DcApiVpSubmissionRequestDto body = new DcApiVpSubmissionRequestDto();
        body.setRequestId(STATE);
        body.setError("access_denied");
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new VPRequestValidationException(ErrorCode.SUBMISSION_ORIGIN_NOT_ALLOWED));

        ResponseEntity<?> response = controller.submitVpDcApi(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto error = (ErrorDto) response.getBody();
        assertNotNull(error);
        assertEquals(ErrorCode.SUBMISSION_ORIGIN_NOT_ALLOWED.getErrorCode(), error.getErrorCode());
    }

    @Test
    void shouldMapAlreadySubmitted_whenDcApiServiceRejectsDuplicate() {
        DcApiVpSubmissionRequestDto body = new DcApiVpSubmissionRequestDto();
        body.setRequestId(STATE);
        body.setError("access_denied");
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new VPAlreadySubmittedException());

        ResponseEntity<?> response = controller.submitVpDcApi(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto error = (ErrorDto) response.getBody();
        assertNotNull(error);
        assertEquals(ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(), error.getErrorCode());
    }

    @Test
    void shouldMapInvalidVpToken_whenDcApiServiceRejectsToken() {
        DcApiVpSubmissionRequestDto body = new DcApiVpSubmissionRequestDto();
        body.setRequestId(STATE);
        body.setVpToken(new ObjectMapper().createObjectNode().putArray("query1").addObject());
        when(vpSubmissionService.submitVerifiablePresentation(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidVpTokenException("bad structure"));

        ResponseEntity<?> response = controller.submitVpDcApi(body, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto error = (ErrorDto) response.getBody();
        assertNotNull(error);
        assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorCode(), error.getErrorCode());
        assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorMessage(), error.getErrorMessage());
        assertFalse(error.getErrorMessage().contains("bad structure"));
    }
}
