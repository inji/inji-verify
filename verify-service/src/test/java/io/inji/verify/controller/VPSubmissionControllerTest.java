package io.inji.verify.controller;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.dcql.CredentialMetaDto;
import io.inji.verify.dto.dcql.CredentialQueryDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.dto.result.DcqlTokensDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.validator.DcqlValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private DcqlValidator dcqlValidator;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VPSubmissionController controller;

    private static final String STATE = "state-123";

    private static final String VALID_VP_TOKEN = """
        {
          "query1": [
            {
              "type": "VerifiablePresentation",
              "proof": {
                "domain": "client-id",
                "challenge": "nonce"
              }
            }
          ]
        }
        """;

    @BeforeEach
    void setup() {
        Map<String, String[]> params = new HashMap<>();
        params.put("state", new String[]{STATE});

        when(request.getParameterMap()).thenReturn(params);
    }

    private DcqlTokensDto mockDcqlTokens() {

        Map<String, List<JSONObject>> ldpVpTokens = new HashMap<>();

        JSONObject vp = new JSONObject();
        vp.put("type", "VerifiablePresentation");

        JSONObject proof = new JSONObject();
        proof.put("domain", "client-id");
        proof.put("challenge", "nonce");

        vp.put("proof", proof);

        ldpVpTokens.put("query1", Collections.singletonList(vp));

        return new DcqlTokensDto(ldpVpTokens, new HashMap<>(), new HashMap<>());
    }

    @Test
    void shouldReturnBadRequest_whenStateMissing() {

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, "", null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertNotNull(body);

        assertEquals(
                ErrorCode.INVALID_STATE_MISSING.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenUnsupportedParameterPresent() {
        Map<String, String[]> params = new HashMap<>();
        params.put("invalid", new String[]{"x"});
        when(request.getParameterMap()).thenReturn(params);
        ResponseEntity<?> response =
                controller.submitVP(null, STATE, "err", null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        verify(vpSubmissionService, never()).submitVpToken(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenBothVpTokenAndErrorProvided() {

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, "error", null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.BOTH_VP_TOKEN_AND_ERROR_NOT_ALLOWED.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenStateNotFound() {

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(null);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.NO_MATCHING_VP_REQUEST.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenRequestExpired() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.EXPIRED);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.VP_REQUEST_EXPIRED.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenVpAlreadySubmitted() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.VP_SUBMITTED);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenInvalidJson() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        ResponseEntity<?> response =
                controller.submitVP("invalid-json", STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenAuthRequestMissing() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(null);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.NO_MATCHING_VP_REQUEST.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenClientIdValidationFails() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestCreateResponse auth =
                mock(AuthorizationRequestCreateResponse.class);

        AuthorizationRequestResponseDto authorizationDetails =
                mock(AuthorizationRequestResponseDto.class);

        when(auth.getAuthorizationDetails())
                .thenReturn(authorizationDetails);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(auth);

        when(vpSubmissionService.extractDcqlTokens(any(),any()))
                .thenReturn(mockDcqlTokens());

        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any()))
                .thenReturn(ErrorCode.CLIENT_ID_VALIDATION_FAILED);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.CLIENT_ID_VALIDATION_FAILED.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnBadRequest_whenNonceValidationFails() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestCreateResponse auth =
                mock(AuthorizationRequestCreateResponse.class);

        AuthorizationRequestResponseDto authorizationDetails =
                mock(AuthorizationRequestResponseDto.class);

        when(auth.getAuthorizationDetails())
                .thenReturn(authorizationDetails);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(auth);

        when(vpSubmissionService.extractDcqlTokens(any(), any()))
                .thenReturn(mockDcqlTokens());

        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any()))
                .thenReturn(ErrorCode.NONCE_VALIDATION_FAILED);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.NONCE_VALIDATION_FAILED.getErrorCode(),
                body.getErrorCode()
        );
    }

    @Test
    void shouldReturnInternalServerError_whenRedirectUriMissing() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestCreateResponse auth =
                mock(AuthorizationRequestCreateResponse.class);

        AuthorizationRequestResponseDto authorizationDetails =
                mock(AuthorizationRequestResponseDto.class);

        when(auth.getAuthorizationDetails())
                .thenReturn(authorizationDetails);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(auth);

        when(vpSubmissionService.extractDcqlTokens(any(), any()))
                .thenReturn(mockDcqlTokens());

        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any()))
                .thenReturn(null);

        when(vpSubmissionService.generateResponseCode(any()))
                .thenReturn("resp-code");

        when(vpSubmissionService.generateResponseCodeExpiry())
                .thenReturn(new Timestamp(System.currentTimeMillis()));

        when(vpSubmissionService.buildRedirectUri(any()))
                .thenReturn(null);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequest_whenDuplicateSubmissionOccurs() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestCreateResponse auth =
                mock(AuthorizationRequestCreateResponse.class);

        AuthorizationRequestResponseDto authorizationDetails =
                mock(AuthorizationRequestResponseDto.class);

        when(auth.getAuthorizationDetails())
                .thenReturn(authorizationDetails);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(auth);

        when(vpSubmissionService.extractDcqlTokens(any(), any()))
                .thenReturn(mockDcqlTokens());

        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any()))
                .thenReturn(null);

        doThrow(new VPAlreadySubmittedException())
                .when(vpSubmissionService)
                .submitVpToken(any(), any(), any(), any(), any(), any(), any());

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorDto body = (ErrorDto) response.getBody();

        assertEquals(
                ErrorCode.VP_ALREADY_SUBMITTED.getErrorCode(),
                body.getErrorCode()
        );
    }

    private AuthorizationRequestCreateResponse mockAuthWithDcql(String credentialId) {
        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);

        DCQLQueryDto dcqlQuery = new DCQLQueryDto(
                List.of(new CredentialQueryDto(credentialId, "ldp_vc", new CredentialMetaDto(null, null), true, false, null, null)),
                null
        );

        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(authDetails.getDcqlQuery()).thenReturn(dcqlQuery);
        return auth;
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenContainsUnknownCredentialId() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // Create the auth mock first to avoid Mockito stub-recording interference
        // caused by nested when() calls inside thenReturn().
        AuthorizationRequestCreateResponse authMock = mockAuthWithDcql("query1");
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(authMock);

        // Auth request expects "query1", but vp_token has "unknown_id"
        String vpTokenWithUnknownId = """
                {
                  "unknown_id": [{ "type": "VerifiablePresentation" }]
                }
                """;

        doThrow(new io.inji.verify.exception.VPRequestValidationException(ErrorCode.VP_TOKEN_UNKNOWN_CREDENTIAL_ID))
                .when(dcqlValidator).validateVpTokenAgainstDcql(any(), any());

        ResponseEntity<?> response = controller.submitVP(vpTokenWithUnknownId, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_UNKNOWN_CREDENTIAL_ID.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenDoesNotSatisfyDcqlCredentialSets() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // Create the auth mock first to avoid Mockito stub-recording interference
        // caused by nested when() calls inside thenReturn().
        AuthorizationRequestCreateResponse authMock = mockAuthWithDcql("query1");
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(authMock);

        // Auth request expects "query1", but vp_token has "query2" which is unknown
        String vpTokenMissingRequired = """
                {
                  "query2": [{ "type": "VerifiablePresentation" }]
                }
                """;

        doThrow(new io.inji.verify.exception.VPRequestValidationException(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED))
                .when(dcqlValidator).validateVpTokenAgainstDcql(any(), any());

        ResponseEntity<?> response = controller.submitVP(vpTokenMissingRequired, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnSuccess_whenSubmissionSucceeds() {

        VPRequestStatusDto dto =
                new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestCreateResponse auth =
                mock(AuthorizationRequestCreateResponse.class);

        AuthorizationRequestResponseDto authorizationDetails =
                mock(AuthorizationRequestResponseDto.class);

        when(auth.getAuthorizationDetails())
                .thenReturn(authorizationDetails);

        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(dto);

        when(vpSubmissionService.getAuthRequest(STATE))
                .thenReturn(auth);

        when(vpSubmissionService.extractDcqlTokens(any(),any()))
                .thenReturn(mockDcqlTokens());

        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any()))
                .thenReturn(null);

        ResponseEntity<?> response =
                controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vpSubmissionService).submitVpToken(any(), any(), any(), any(), any(), any(), any());
    }

    // ---- SD-JWT KB-JWT validation tests ----

    private DcqlTokensDto mockSdJwtTokens() {
        Map<String, List<String>> sdJwtTokens = new HashMap<>();
        sdJwtTokens.put("cred1", Collections.singletonList("header.payload.sig~kb-header.kb-payload.kb-sig"));
        return new DcqlTokensDto(new HashMap<>(), new HashMap<>(), sdJwtTokens);
    }

    private AuthorizationRequestCreateResponse mockActiveAuth() {
        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);
        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(vpRequestService.getCurrentRequestStatus(STATE))
                .thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);
        when(vpSubmissionService.extractDcqlTokens(any(), any())).thenReturn(mockSdJwtTokens());
        return auth;
    }

    @Test
    void shouldReturnBadRequest_whenSdJwtKbJwtClientIdFails() {
        mockActiveAuth();

        when(vpSubmissionService.processSdJwtClientIdAndNonce(any(), any()))
                .thenReturn(ErrorCode.CLIENT_ID_VALIDATION_FAILED);

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.CLIENT_ID_VALIDATION_FAILED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenSdJwtKbJwtNonceFails() {
        mockActiveAuth();

        when(vpSubmissionService.processSdJwtClientIdAndNonce(any(), any()))
                .thenReturn(ErrorCode.NONCE_VALIDATION_FAILED);

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.NONCE_VALIDATION_FAILED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldProceed_whenSdJwtKbJwtValid() {
        AuthorizationRequestCreateResponse auth = mockActiveAuth();
        when(auth.getAuthorizationDetails()).thenReturn(mock(AuthorizationRequestResponseDto.class));

        when(vpSubmissionService.processSdJwtClientIdAndNonce(any(), any())).thenReturn(null);

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vpSubmissionService).submitVpToken(any(), any(), any(), any(), any(), any(), any());
    }

    // ---- validateRequest missing paths ----

    @Test
    void shouldReturnBadRequest_whenNeitherVpTokenNorErrorProvided() {
        ResponseEntity<?> response = controller.submitVP(null, STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.EITHER_VP_TOKEN_OR_ERROR_REQUIRED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenErrorDescriptionProvidedWithVpToken() {
        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, "some description", request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.ERROR_DESCRIPTION_VP_TOKEN_CONFLICT.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenErrorDescriptionProvidedWithoutError() {
        ResponseEntity<?> response = controller.submitVP(null, STATE, null, "some description", request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.ERROR_DESCRIPTION_ERROR_REQUIRED.getErrorCode(), body.getErrorCode());
    }

    // ---- validateVPTokenStructure missing paths ----

    @Test
    void shouldReturnBadRequest_whenVpTokenIsLiteralNull() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitVP("null", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_REQUIRED.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenIsJsonArray() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitVP("[{\"type\":\"VP\"}]", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenIsEmptyObject() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitVP("{}", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_MUST_HAVE_KEY_VALUE_PAIR.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenValueIsNotArray() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitVP("{\"cred1\":{\"type\":\"VP\"}}", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_VALUES_MUST_BE_ARRAYS.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenArrayIsEmpty() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        ResponseEntity<?> response = controller.submitVP("{\"cred1\":[]}", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_ARRAYS_MUST_HAVE_ELEMENTS.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenFirstElementIsInvalid() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // First element is an empty JSON object (not a valid non-empty object, not a string)
        ResponseEntity<?> response = controller.submitVP("{\"cred1\":[{}]}", STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_ARRAY_ELEMENTS_INVALID.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenArrayHasMixedTypes_objectThenNonObject() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // First element valid object, second is a string
        ResponseEntity<?> response = controller.submitVP(
                "{\"cred1\":[{\"type\":\"VP\"}, \"sd-jwt-string\"]}",
                STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_OBJECTS.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenArrayHasMixedTypes_sdJwtThenNonString() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // First element is a non-empty SD-JWT string, second is a JSON object
        ResponseEntity<?> response = controller.submitVP(
                "{\"cred1\":[\"header.payload.sig~kb\", {\"type\":\"VP\"}]}",
                STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_SD_JWT.getErrorCode(), body.getErrorCode());
    }

    @Test
    void shouldReturnBadRequest_whenVpTokenHasDuplicateQueryIds() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        // Raw JSON with duplicate keys — standard ObjectMapper de-dupes them, but
        // validateDuplicateQueryIds uses streaming API which catches duplicates
        String duplicateKeys = "{\"cred1\":[{\"type\":\"VP\"}],\"cred1\":[{\"type\":\"VP\"}]}";
        ResponseEntity<?> response = controller.submitVP(duplicateKeys, STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals(ErrorCode.DUPLICATE_QUERY_IDS_NOT_ALLOWED.getErrorCode(), body.getErrorCode());
    }

    // ---- extractDcqlTokens exception path ----

    @Test
    void shouldReturnBadRequest_whenExtractDcqlTokensThrowsInvalidVpToken() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);
        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);
        when(vpSubmissionService.extractDcqlTokens(any(), any()))
                .thenThrow(new io.inji.verify.exception.InvalidVpTokenException("bad structure"));

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = (ErrorDto) response.getBody();
        assertNotNull(body);
        assertEquals("invalid_vp_token", body.getErrorCode());
    }

    // ---- Error-only submission (no vpToken) ----

    @Test
    void shouldReturnSuccess_whenErrorSubmissionSucceeds() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);
        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

        ResponseEntity<?> response = controller.submitVP(null, STATE, "access_denied", "user cancelled", request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vpSubmissionService).submitVpToken(any(), isNull(), eq(STATE), eq("access_denied"), eq("user cancelled"), any(), any());
    }

    // ---- Success path with response code ----

    @Test
    void shouldReturnSuccess_withRedirectUriWhenResponseCodeGenerated() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);
        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);
        when(vpSubmissionService.extractDcqlTokens(any(), any())).thenReturn(mockDcqlTokens());
        when(vpSubmissionService.processLdpVpClientIdAndNonce(any(), any())).thenReturn(null);
        when(vpSubmissionService.generateResponseCode(any())).thenReturn("resp-code-123");
        when(vpSubmissionService.generateResponseCodeExpiry()).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(vpSubmissionService.buildRedirectUri("resp-code-123")).thenReturn("https://example.com/cb?response_code=resp-code-123");

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("https://example.com/cb?response_code=resp-code-123", body.get("redirect_uri"));
    }

    // ---- validateClientIdNonce — no bindable tokens (both maps empty) ----

    @Test
    void shouldProceed_whenDcqlTokensHaveNoBindableTokens() {
        VPRequestStatusDto dto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);
        when(vpRequestService.getCurrentRequestStatus(STATE)).thenReturn(dto);

        AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
        AuthorizationRequestResponseDto authDetails = mock(AuthorizationRequestResponseDto.class);
        when(auth.getAuthorizationDetails()).thenReturn(authDetails);
        when(vpSubmissionService.getAuthRequest(STATE)).thenReturn(auth);

        // DcqlTokensDto with only ldpVcTokens (no LDP VP, no SD-JWT → skips clientId/nonce validation)
        Map<String, List<org.json.JSONObject>> ldpVcTokens = new HashMap<>();
        ldpVcTokens.put("cred1", List.of(new org.json.JSONObject("{\"type\":\"VerifiableCredential\"}")));
        DcqlTokensDto ldpVcOnly = new DcqlTokensDto(new HashMap<>(), ldpVcTokens, new HashMap<>());
        when(vpSubmissionService.extractDcqlTokens(any(), any())).thenReturn(ldpVcOnly);

        ResponseEntity<?> response = controller.submitVP(VALID_VP_TOKEN, STATE, null, null, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(vpSubmissionService).submitVpToken(any(), any(), any(), any(), any(), any(), any());
        verify(vpSubmissionService, never()).processLdpVpClientIdAndNonce(any(), any());
        verify(vpSubmissionService, never()).processSdJwtClientIdAndNonce(any(), any());
    }
}