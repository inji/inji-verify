package io.inji.verify.controller;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.services.VerifiablePresentationRequestService;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VPSubmissionControllerTest {

	@Mock
	private VerifiablePresentationRequestService vpRequestService;

	@Mock
	private VerifiablePresentationSubmissionService vpSubmissionService;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private VPSubmissionController controller;

	private final String STATE = "state-123";

	@BeforeEach
	void setup() {
		Map<String, String[]> params = new HashMap<>();
		params.put("state", new String[] { STATE });

		when(request.getParameterMap()).thenReturn(params);
	}

	@Test
	void shouldReturnBadRequest_whenStateMissing() {

		ResponseEntity<?> response = controller.submitVP(null, "", null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertNotNull(body);

		assertEquals(ErrorCode.EITHER_VP_TOKEN_OR_ERROR_REQUIRED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenUnsupportedParameterPresent() {

		Map<String, String[]> params = new HashMap<>();
		params.put("invalid", new String[] { "x" });

		when(request.getParameterMap()).thenReturn(params);

		ResponseEntity<?> response = controller.submitVP(null, STATE, "err", null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void shouldReturnBadRequest_whenBothVpTokenAndErrorProvided() {

		ResponseEntity<?> response = controller.submitVP("{\"a\":[]}", STATE, "error", null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.BOTH_VP_TOKEN_AND_ERROR_NOT_ALLOWED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenStateNotFound() {

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(null);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.NO_MATCHING_VP_REQUEST.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenRequestExpired() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.EXPIRED);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.VP_REQUEST_EXPIRED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenVpAlreadySubmitted() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.VP_SUBMITTED);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenVpTokenInvalidJson() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		ResponseEntity<?> response = controller.submitVP("invalid-json", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenAuthRequestMissing() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(null);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.NO_MATCHING_VP_REQUEST.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenClientIdValidationFails() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);

		AuthorizationRequestResponseDto authorizationRequestResponseDto = mock(AuthorizationRequestResponseDto.class);

		when(auth.getAuthorizationDetails()).thenReturn(authorizationRequestResponseDto);
		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

		when(vpSubmissionService.isClientIdValid(any(), any())).thenReturn(false);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.CLIENT_ID_VALIDATION_FAILED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnBadRequest_whenNonceValidationFails() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);

		AuthorizationRequestResponseDto authorizationRequestResponseDto = mock(AuthorizationRequestResponseDto.class);

		when(auth.getAuthorizationDetails()).thenReturn(authorizationRequestResponseDto);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

		when(vpSubmissionService.isClientIdValid(any(), any())).thenReturn(true);

		when(vpSubmissionService.isNonceValid(any(), any())).thenReturn(false);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.NONCE_VALIDATION_FAILED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnInternalServerError_whenRedirectUriMissing() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);

		AuthorizationRequestResponseDto authorizationRequestResponseDto = mock(AuthorizationRequestResponseDto.class);

		when(auth.getAuthorizationDetails()).thenReturn(authorizationRequestResponseDto);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

		when(vpSubmissionService.isClientIdValid(any(), any())).thenReturn(true);

		when(vpSubmissionService.isNonceValid(any(), any())).thenReturn(true);

		when(vpSubmissionService.generateResponseCode(any())).thenReturn("resp-code");

		when(vpSubmissionService.generateResponseCodeExpiry()).thenReturn(new Timestamp(System.currentTimeMillis()));

		when(vpSubmissionService.buildRedirectUri(any())).thenReturn(null);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	void shouldReturnBadRequest_whenDuplicateSubmissionOccurs() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);

		AuthorizationRequestResponseDto authorizationRequestResponseDto = mock(AuthorizationRequestResponseDto.class);

		when(auth.getAuthorizationDetails()).thenReturn(authorizationRequestResponseDto);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

		when(vpSubmissionService.isClientIdValid(any(), any())).thenReturn(true);

		when(vpSubmissionService.isNonceValid(any(), any())).thenReturn(true);

		doThrow(new VPAlreadySubmittedException()).when(vpSubmissionService).submitVpToken(any(), any(), any(), any(),
				any(), any(), any());

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

		ErrorDto body = (ErrorDto) response.getBody();

		assertEquals(ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(), body.getErrorCode());
	}

	@Test
	void shouldReturnSuccess_whenSubmissionSucceeds() {

		VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

		AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);

		AuthorizationRequestResponseDto authorizationRequestResponseDto = mock(AuthorizationRequestResponseDto.class);

		when(auth.getAuthorizationDetails()).thenReturn(authorizationRequestResponseDto);

		when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

		when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

		when(vpSubmissionService.isClientIdValid(any(), any())).thenReturn(true);

		when(vpSubmissionService.isNonceValid(any(), any())).thenReturn(true);

		ResponseEntity<?> response = controller.submitVP("{\"a\":[{\"x\":\"y\"}]}", STATE, null, null, request);

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}
}