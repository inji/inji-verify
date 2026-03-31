package io.inji.verify.services.impl;

import com.nimbusds.jose.shaded.gson.Gson;
import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.CredentialResultsDto;
import io.inji.verify.dto.result.VPTokenDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.PathNestedDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.enums.KBJwtErrorCodes;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.exception.ResponseCodeException;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.mosip.pixelpass.PixelPass;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.data.*;
import io.inji.verify.repository.VPSubmissionRepository;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import jakarta.validation.Validator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VerifiablePresentationSubmissionServiceImplTest {

    @Mock
    private VPSubmissionRepository vpSubmissionRepository;

    @Mock
    private PresentationVerifier presentationVerifier;

    @Mock
    private VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService;

    @Mock
    private CredentialsVerifier credentialsVerifier;

    @Mock
    private VCVerificationServiceImpl vcVerificationService;

    @InjectMocks
    private VerifiablePresentationSubmissionServiceImpl verifiablePresentationSubmissionService;

    @Mock
    private PixelPass pixelPass;

    @Mock
    private AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository;

    @Mock
    private Gson gson;

    @Mock
    private Validator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        verifiablePresentationSubmissionService = new VerifiablePresentationSubmissionServiceImpl(vpSubmissionRepository, credentialsVerifier, presentationVerifier, verifiablePresentationRequestService, vcVerificationService, pixelPass, authorizationRequestCreateResponseRepository, gson, validator);
    }

    @Nested
    class TestVPSubmission {
        @Test
        public void testSaveVPSubmission_Dto_Success() throws Exception {
            VPSubmissionDto vpSubmissionDto = new VPSubmissionDto("vpToken123",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>())
                    , "state123", "", "", null, null, false);
            vpSubmissionDto.setResponseCode("");
            vpSubmissionDto.setResponseCodeExpiryAt(null);
            vpSubmissionDto.setResponseCodeUsed(false);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("saveVPSubmissionDto", VPSubmissionDto.class);
            method.setAccessible(true);
            method.invoke(verifiablePresentationSubmissionService, vpSubmissionDto);

            verify(vpSubmissionRepository, times(1)).save(any(VPSubmission.class));
            verify(verifiablePresentationRequestService, times(1)).invokeVpRequestStatusListener("state123");
        }

        @Test
        public void testSaveVPSubmission_Dto_WithResponseCode_Success() throws Exception {
            String responseCode = "generated-code-123";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));
            VPSubmissionDto vpSubmissionDto = new VPSubmissionDto(
                    "vpToken123",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    "state123",
                    null,
                    null,
                    null,
                    null,
                    false
            );
            vpSubmissionDto.setResponseCode(responseCode);
            vpSubmissionDto.setResponseCodeExpiryAt(expiryAt);
            vpSubmissionDto.setResponseCodeUsed(false);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("saveVPSubmissionDto", VPSubmissionDto.class);
            method.setAccessible(true);
            method.invoke(verifiablePresentationSubmissionService, vpSubmissionDto);

            ArgumentCaptor<VPSubmission> captor = ArgumentCaptor.forClass(VPSubmission.class);
            verify(vpSubmissionRepository, times(1)).save(captor.capture());

            VPSubmission savedSubmission = captor.getValue();
            assertEquals(responseCode, savedSubmission.getResponseCode());
            assertEquals(expiryAt, savedSubmission.getResponseCodeExpiryAt());
            assertFalse(savedSubmission.isResponseCodeUsed());
            assertEquals("state123", savedSubmission.getRequestId());
        }

        @Test
        public void testSaveVPSubmission_Dto_WithNullResponseCode_Success() throws Exception {
            VPSubmissionDto vpSubmissionDto = new VPSubmissionDto(
                    "vpToken123",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    "state123",
                    null,
                    null,
                    null,
                    null,
                    false
            );
            vpSubmissionDto.setResponseCode(null);
            vpSubmissionDto.setResponseCodeExpiryAt(null);
            vpSubmissionDto.setResponseCodeUsed(false);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("saveVPSubmissionDto", VPSubmissionDto.class);
            method.setAccessible(true);
            method.invoke(verifiablePresentationSubmissionService, vpSubmissionDto);

            ArgumentCaptor<VPSubmission> captor = ArgumentCaptor.forClass(VPSubmission.class);
            verify(vpSubmissionRepository, times(1)).save(captor.capture());

            VPSubmission savedSubmission = captor.getValue();
            assertNull(savedSubmission.getResponseCode());
            assertNull(savedSubmission.getResponseCodeExpiryAt());
            assertFalse(savedSubmission.isResponseCodeUsed());
        }

        @Test
        public void testSubmit_GeneratesResponseCodeAndExpiry_if_ResponseCodeValidationRequired() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"nonce\",\"domain\":\"clientId\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"John Doe\"}}]}";
            String presentationSubmission = "{\"id\":\"testId\"}";
            String state = "testState";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id", "dId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );

            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
            ReflectionTestUtils.setField(verifiablePresentationSubmissionService, "redirectUri", "https://example.com/callback");

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertInstanceOf(Map.class, response.getBody());
            java.util.Map<?, ?> responseBody = (java.util.Map<?, ?>) response.getBody();
            assertTrue(responseBody.containsKey("redirect_uri"));

            ArgumentCaptor<VPSubmission> captor = ArgumentCaptor.forClass(VPSubmission.class);
            verify(vpSubmissionRepository, times(1)).save(captor.capture());

            VPSubmission savedSubmission = captor.getValue();
            assertNotNull(savedSubmission.getResponseCode(), "Response code should be generated");
            assertNotNull(savedSubmission.getResponseCodeExpiryAt(), "Response code expiry should be set");
            assertFalse(savedSubmission.isResponseCodeUsed(), "Response code should initially be unused");
            assertEquals(state, savedSubmission.getRequestId());
        }

        @Test
        public void testSubmit_DoesNotGenerateResponseCode_ifNot_ResponseCodeValidationRequired() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"nonce\",\"domain\":\"clientId\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"John Doe\"}}]}";
            String presentationSubmission = "{\"id\":\"testId\"}";
            String state = "testState";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id", "dId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    false
            );

            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertInstanceOf(Map.class, response.getBody());
            java.util.Map<?, ?> responseBody = (java.util.Map<?, ?>) response.getBody();
            assertFalse(responseBody.containsKey("redirect_uri"), "Cross-device flow should not include redirect_uri");

            ArgumentCaptor<VPSubmission> captor = ArgumentCaptor.forClass(VPSubmission.class);
            verify(vpSubmissionRepository, times(1)).save(captor.capture());

            VPSubmission savedSubmission = captor.getValue();
            assertNull(savedSubmission.getResponseCode(), "Response code should NOT be generated for cross-device flow");
            assertNull(savedSubmission.getResponseCodeExpiryAt(), "Response code expiry should NOT be set for cross-device flow");
            assertFalse(savedSubmission.isResponseCodeUsed());
            assertEquals(state, savedSubmission.getRequestId());
        }

        @Test
        public void testSubmit_WithError_GenerateResponseCode() {
            String error = "access_denied";
            String errorDescription = "User denied access";
            String state = "testState";

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );

            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            ReflectionTestUtils.setField(verifiablePresentationSubmissionService, "redirectUri", "https://example.com/callback");

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(null, null, state, error, errorDescription);

            assertEquals(200, response.getStatusCode().value());

            ArgumentCaptor<VPSubmission> captor = ArgumentCaptor.forClass(VPSubmission.class);
            verify(vpSubmissionRepository, times(1)).save(captor.capture());

            VPSubmission savedSubmission = captor.getValue();
            assertEquals(error, savedSubmission.getError());
            assertEquals(errorDescription, savedSubmission.getErrorDescription());
            assertNotNull(savedSubmission.getResponseCode(), "Response code should be generated even for error submission");
            assertNotNull(savedSubmission.getResponseCodeExpiryAt(), "Response code expiry should be set");
            assertFalse(savedSubmission.isResponseCodeUsed());
        }

        @Test
        public void testSubmit_ValidateToken_Success_NonceAndDomainMatch() {
            String nonce = "my-nonce";
            String clientId = "my-client";
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"my-nonce\",\"domain\":\"my-client\"},\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails =
                    new AuthorizationRequestResponseDto(clientId, "presentationDefinitionUri", null, nonce, "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(200, response.getStatusCode().value());
            verify(vpSubmissionRepository, times(1)).save(any(VPSubmission.class));
        }

        @Test
        public void testSubmit_ValidateToken_Returns400_WhenNonceMismatch() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"wrong-nonce\",\"domain\":\"my-client\"},\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(400, response.getStatusCode().value());
            assertInstanceOf(ErrorDto.class, response.getBody());
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_Returns400_WhenDomainMismatch() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],"
                    + "\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"my-nonce\",\"domain\":\"wrong-client\"},"
                    + "\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(400, response.getStatusCode().value());
            assertInstanceOf(ErrorDto.class, response.getBody());
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_Returns400_WhenBothNonceAndDomainMismatch() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],"
                    + "\"proof\":{\"type\":\"Ed25519Signature2018\",\"challenge\":\"bad-nonce\",\"domain\":\"bad-client\"},"
                    + "\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(400, response.getStatusCode().value());
            assertInstanceOf(ErrorDto.class, response.getBody());
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_Returns400_WhenProofNodeMissing() {
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(400, response.getStatusCode().value());
            assertInstanceOf(ErrorDto.class, response.getBody());
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_ThrowsInvalidVpTokenException_WhenVpTokenIsNull() {
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            assertThrows(InvalidVpTokenException.class,
                    () -> verifiablePresentationSubmissionService.submit(null, presentationSubmission, state, null, null));
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_ThrowsInvalidVpTokenException_WhenVpTokenIsMalformedJson() {
            String vpToken = "not-valid-json!!!";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            assertThrows(InvalidVpTokenException.class,
                    () -> verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null));
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testSubmit_ValidateToken_Skipped_WhenAcceptVPWithoutHolderProof_True() {
            // acceptVPWithoutHolderProof = true → validateVpTokens never called; mismatched proof still accepted
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"challenge\":\"wrong\",\"domain\":\"wrong\"},\"verifiableCredential\":[]}";
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri",
                    true,  // acceptVPWithoutHolderProof = true
                    false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(vpToken, presentationSubmission, state, null, null);

            assertEquals(200, response.getStatusCode().value());
            verify(vpSubmissionRepository, times(1)).save(any(VPSubmission.class));
        }

        @Test
        public void testSubmit_ValidateToken_Skipped_WhenOnlySdJwtTokens() {
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"vc+sd-jwt\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"123\"}".getBytes());
            String sig = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".getBytes());
            String sdJwtToken = header + "." + payload + "." + sig;
            String presentationSubmission = "{\"id\":\"subId\"}";
            String state = "stateABC";

            PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("subId", "defId", new ArrayList<>());

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "my-client", "presentationDefinitionUri", null, "my-nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    state, "txId", authDetails, System.currentTimeMillis() + 100000);

            when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authResponse));
            when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);

            ResponseEntity<?> response = verifiablePresentationSubmissionService.submit(sdJwtToken, presentationSubmission, state, null, null);

            assertEquals(200, response.getStatusCode().value());
            verify(vpSubmissionRepository, times(1)).save(any(VPSubmission.class));
        }
    }

    @Nested
    class TestVPResult {
        @Test
        public void testGetVPResult_Success_JSONObject() {
            List<String> requestIds = List.of("req123");
            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.SUCCESS, new HashMap<>())
            );
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path"))
                    )),
                    "", "", "", null, false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
            assertEquals(1, resultDto.getVcResults().size());
        }

        @Test
        public void testGetVPResult_Success_Base64EncodedString() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";
            String vpTokenJson = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}";
            String base64Token = Base64.getUrlEncoder().encodeToString(vpTokenJson.getBytes());
            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("", VerificationStatus.SUCCESS, new HashMap<>())
            );
            VPSubmission vpSubmission = new VPSubmission("state123", "\"" + base64Token + "\"",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), "", "", "", null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_Success_JSONArray() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";
            List<VCResultWithCredentialStatus> vcResults1 = List.of(
                    new VCResultWithCredentialStatus("vc1", VerificationStatus.SUCCESS, new HashMap<>())
            );
            List<VCResultWithCredentialStatus> vcResults2 = List.of(
                    new VCResultWithCredentialStatus("vc2", VerificationStatus.SUCCESS, new HashMap<>())
            );
            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "[" +
                            "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}," +
                            "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}" +
                            "]",
                    new PresentationSubmissionDto(
                            "id", "dId",
                            List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))
                    ),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults1))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults2));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
            assertEquals(2, resultDto.getVcResults().size());
        }

        @Test
        public void testGetVPResult_Success_JSONArrayWithBase64() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            String vpToken1Json = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}";
            String base64Token1 = Base64.getUrlEncoder().encodeToString(vpToken1Json.getBytes());

            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("", VerificationStatus.SUCCESS, new HashMap<>())
            );

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "[\"" + base64Token1 + "\", \"{\\\"type\\\":[\\\"VerifiablePresentation\\\"],\\\"proof\\\":{\\\"type\\\":\\\"Ed25519Signature2018\\\"},\\\"VerifiablePresentation\\\":[{\\\"type\\\":[\\\"VerifiablePresentation\\\"]}]}\"]",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_VPSubmissionNotFound() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(new ArrayList<>());

            assertThrows(VPSubmissionNotFoundException.class,
                    () -> verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId));
        }

        @Test
        public void testGetVPResult_VerificationFailed_InvalidVPStatus() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.INVALID, new ArrayList<>()));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_VerificationFailed_InvalidVCStatus() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("",
                            VerificationStatus.INVALID, new HashMap<>())
            );

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_VerificationFailed_ExpiredVCStatus() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("",
                            VerificationStatus.SUCCESS, new HashMap<>())
            );

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null,  null, null
                    , false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailed_NullVpToken() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123", "null",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailed_NullRequest() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(null);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailed_EmptyDescriptorMap() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>())
                    , null, null, null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailed_NullDescriptorMap() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", null), null, null,
                    null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_ExceptionHandling_RuntimeException() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            when(presentationVerifier.verify(anyString())).thenThrow(new RuntimeException("Verification error"));

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_InvalidVPTokenFormat() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123", "12345", // Invalid format (number)
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_InvalidItemInVPTokenArray() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123", "[123, \"invalid\"]", // Invalid array items
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_InvalidBase64InArray() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123", "[\"invalid-base64!!!\"]",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_InvalidBase64String() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123", "\"invalid-base64!!!\"",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null,  null, null
                    , false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_EmptyVpVerificationStatuses() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_VerificationFailedException() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("", VerificationStatus.INVALID, new HashMap<>()));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailedException() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(null);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_MixedVerificationStatuses() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> vcResults = Arrays.asList(
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.SUCCESS, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.REVOKED, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.EXPIRED, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.INVALID, new HashMap<>())
            );

            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2020\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}",
                    new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.INVALID, new ArrayList<>()));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
            assertEquals(4, resultDto.getVcResults().size());
            assertEquals(VerificationStatus.SUCCESS, resultDto.getVcResults().get(0).getVerificationStatus());
            assertEquals(VerificationStatus.REVOKED, resultDto.getVcResults().get(1).getVerificationStatus());
            assertEquals(VerificationStatus.EXPIRED, resultDto.getVcResults().get(2).getVerificationStatus());
            assertEquals(VerificationStatus.INVALID, resultDto.getVcResults().get(3).getVerificationStatus());
        }

        @Test
        public void testGetVPResult_AllVerificationStatusTypes() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> successResults = List.of(new VCResultWithCredentialStatus("vc_success", VerificationStatus.SUCCESS, new HashMap<>()));
            List<VCResultWithCredentialStatus> expiredResults = List.of(new VCResultWithCredentialStatus("vc_expired", VerificationStatus.EXPIRED, new HashMap<>()));
            List<VCResultWithCredentialStatus> invalidResults = List.of(new VCResultWithCredentialStatus("vc_invalid", VerificationStatus.INVALID, new HashMap<>()));

            VPSubmission vpSubmission = new VPSubmission("state123",
                    "[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}, " +
                            "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}, " +
                            "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}]",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, successResults))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, expiredResults))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, invalidResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
            assertEquals(3, resultDto.getVcResults().size());

            assertTrue(resultDto.getVcResults().stream()
                    .anyMatch(vc -> vc.getVerificationStatus() == VerificationStatus.SUCCESS));
            assertTrue(resultDto.getVcResults().stream()
                    .anyMatch(vc -> vc.getVerificationStatus() == VerificationStatus.EXPIRED));
            assertTrue(resultDto.getVcResults().stream()
                    .anyMatch(vc -> vc.getVerificationStatus() == VerificationStatus.INVALID));
        }

        @Test
        public void testGetVPResult_Revoked_JSONObject() {
            List<String> requestIds = List.of("req123");
            List<VCResultWithCredentialStatus> vcResults = List.of(new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.REVOKED, new HashMap<>()));
            String transactionId = "tx123";

            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2020\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path"))
                    )),
                    null,
                    null, null, null, false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                    new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
            assertEquals(1, resultDto.getVcResults().size());
            assertEquals(VerificationStatus.REVOKED, resultDto.getVcResults().getFirst().getVerificationStatus());
        }

        @Test
        void testProcessSubmission_NoProof_Accepted() {
            String vpToken = "{\"type\":\"VerifiablePresentation\",\"verifiableCredential\":[\"vc1\"]}";
            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "respType", new VPDefinitionResponseDto("",
                    new ArrayList<>(), "", "", null, new ArrayList<>()), "nonce",
                    "state", true, false);

            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(authDetails);

            PresentationSubmissionDto submissionDto = new PresentationSubmissionDto("id", "defId", new ArrayList<>());

            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(any())).thenReturn(auth);
            when(vpSubmissionRepository.findAllById(any())).thenReturn(List.of(
                    new VPSubmission("st", vpToken, submissionDto, "", "", "",
                            null, false)));

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            VerificationResult vResult = mock(VerificationResult.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(vResult.getVerificationStatus()).thenReturn(true);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), any(), anyList()))
                    .thenReturn(summary);

            assertDoesNotThrow(() -> verifiablePresentationSubmissionService.getVPResult(List.of("id"), "tx"));
        }

        @Test
        public void testIsVPTokenNotMatching_AllValidConditions() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> vcResults = List.of(
                    new VCResultWithCredentialStatus("", VerificationStatus.SUCCESS, new HashMap<>())
            );
            VPSubmission vpSubmission = new VPSubmission("state123",
                    "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                    "format", "path")))), null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                    .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
            verify(verifiablePresentationRequestService, times(1)).getLatestAuthorizationRequestFor(transactionId);
        }

        @Test
        public void testProcessJsonVpTokens_SimpleVC() {
            // Prepare a VPSubmission with a simple VC token
            JSONArray types = new JSONArray();
            types.put("VerifiableCredential");
            JSONObject vc = new JSONObject();
            vc.put("type", types);

            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    vc.toString(),
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path"))
                    )),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));

            // Mock CredentialVerificationSummary and VerificationResult
            io.mosip.vercred.vcverifier.data.CredentialVerificationSummary mockSummary = mock(io.mosip.vercred.vcverifier.data.CredentialVerificationSummary.class);
            VerificationResult mockResult = mock(VerificationResult.class);
            when(mockResult.getVerificationStatus()).thenReturn(true);
            when(mockSummary.getVerificationResult()).thenReturn(mockResult);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.LDP_VC), anyList()))
                    .thenReturn(mockSummary);
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
            assertTrue(resultDto.getVcResults().isEmpty());
        }

        @Test
        public void testProcessSdJwtVpTokens_Success() {
            String header = Base64.getUrlEncoder().encodeToString("{\"typ\":\"vc+sd-jwt\"}".getBytes());
            String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"123\"}".getBytes());
            String signature = Base64.getUrlEncoder().encodeToString("signature".getBytes());
            String sdJwtToken = header + "." + payload + "." + signature;
            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "\"" + sdJwtToken + "\"",
                    new PresentationSubmissionDto("id", "dId", List.of(
                            new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path"))
                    )),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));

            io.mosip.vercred.vcverifier.data.CredentialVerificationSummary mockSummary = mock(io.mosip.vercred.vcverifier.data.CredentialVerificationSummary.class);
            VerificationResult mockResult = mock(VerificationResult.class);
            when(mockResult.getVerificationStatus()).thenReturn(true);
            when(mockSummary.getVerificationResult()).thenReturn(mockResult);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.VC_SD_JWT), anyList()))
                    .thenReturn(mockSummary);
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
            assertFalse(resultDto.getVcResults().isEmpty());
        }

        @Test
        void testGetVPResult_throwResponseCodeNotUsed_ifResponseCodeVerificationRequiredIsTrue() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("code123");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            VPSubmission vpSubmission = mock(VPSubmission.class);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
            when(vpSubmission.getError()).thenReturn(null);
            when(vpSubmission.getResponseCode()).thenReturn("code123");
            when(vpSubmission.getResponseCodeExpiryAt()).thenReturn(new Timestamp(Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli()));
            when(vpSubmissionRepository.markResponseCodeAsUsed(any())).thenReturn(0);

            ResponseCodeException exception = assertThrows(ResponseCodeException.class, () ->
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId));
            assertEquals(ErrorCode.RESPONSE_CODE_NOT_USED, exception.getErrorCode());
        }

        @Test
        public void testGetVPResult_ifResponseCodeUsed_AndIfResponseCodeVerificationRequiredIsTrue() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";
            VerificationRequestDto verificationRequestDto = new VerificationRequestDto(true, List.of(), false);
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"], \"credentialSubject\": {\"name\":\"John Doe\"}}]}";
            VPSubmission vpSubmission = new VPSubmission("state123", vpToken,
                    new PresentationSubmissionDto("id", "dId",
                            List.of(new DescriptorMapDto("id", "format", "path",
                                    new PathNestedDto("format", "path")))), null,
                    null, null, null, true);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce",
                    "responseUri", false, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);

            PresentationVerificationResultV2 presentationVerificationResult = mock(PresentationVerificationResultV2.class);
            VerificationResult proofVerificationResult = mock(VerificationResult.class);
            when(presentationVerificationResult.getProofVerificationResult()).thenReturn(proofVerificationResult);
            when(proofVerificationResult.getVerificationStatus()).thenReturn(true);
            VerificationResult verificationResult = new VerificationResult(true, "", "");
            VCResultV2 vcResult = new VCResultV2("{\"type" + "\":[\"VerifiableCredential" + "\"], \"credentialSubject\": {\"name\":\"John Doe\"}}", verificationResult);
            List<VCResultV2> vcResults = new ArrayList<>();
            vcResults.add(vcResult);
            when(presentationVerificationResult.getVcResults()).thenReturn(vcResults);
            when(presentationVerifier.verifyV2(anyString())).thenReturn(presentationVerificationResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId);
            List<CredentialResultsDto> credentialResults = result.getCredentialResults();

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, credentialResults.size());
            assertTrue(credentialResults.getFirst().isAllChecksSuccessful());
            assertTrue(credentialResults.getFirst().getExpiryCheck().isValid());
            assertTrue(credentialResults.getFirst().getSchemaAndSignatureCheck().isValid());
            assertTrue(credentialResults.getFirst().getHolderProofCheck().isValid());
        }
    }

    @Nested
    class TestVPSessionResults {
        @Test
        void testGetVPSessionResults_responseCodeRequiredButMissing() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode(null);
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);

            VPSubmissionNotFoundException exception = assertThrows(VPSubmissionNotFoundException.class, () ->
                verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
        }

        @Test
        void testGetVPSessionResults_submissionNotFound() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("code123");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(Collections.emptyList());

            assertThrows(VPSubmissionNotFoundException.class, () ->
                    verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
        }

        @Test
        public void testGetVPSessionResults_success() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";
            VerificationSessionRequestDto verificationRequestDto = new VerificationSessionRequestDto(true, List.of(), false, "abc");
            String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"], \"credentialSubject\": {\"name\":\"John Doe\"}}]}";
            VPSubmission vpSubmission = new VPSubmission("state123", vpToken,
                    new PresentationSubmissionDto("id", "dId",
                            List.of(new DescriptorMapDto("id", "format", "path",
                                    new PathNestedDto("format", "path")))), null,
                    null, null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", "presentationDefinitionUri", null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);

            PresentationVerificationResultV2 presentationVerificationResult = mock(PresentationVerificationResultV2.class);
            VerificationResult proofVerificationResult = mock(VerificationResult.class);
            when(presentationVerificationResult.getProofVerificationResult()).thenReturn(proofVerificationResult);
            when(proofVerificationResult.getVerificationStatus()).thenReturn(true);
            VerificationResult verificationResult = new VerificationResult(true, "", "");
            VCResultV2 vcResult = new VCResultV2("{\"type" + "\":[\"VerifiableCredential" + "\"], \"credentialSubject\": {\"name\":\"John Doe\"}}", verificationResult);
            List<VCResultV2> vcResults = new ArrayList<>();
            vcResults.add(vcResult);
            when(presentationVerificationResult.getVcResults()).thenReturn(vcResults);
            when(presentationVerifier.verifyV2(anyString())).thenReturn(presentationVerificationResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPSessionResults(verificationRequestDto, requestIds, transactionId);
            List<CredentialResultsDto> credentialResults = result.getCredentialResults();

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, credentialResults.size());
            assertTrue(credentialResults.getFirst().isAllChecksSuccessful());
            assertTrue(credentialResults.getFirst().getExpiryCheck().isValid());
            assertTrue(credentialResults.getFirst().getSchemaAndSignatureCheck().isValid());
            assertTrue(credentialResults.getFirst().getHolderProofCheck().isValid());
        }

        @Test
        void testGetVPSessionResults_responseCodeMismatch() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("wrongCode");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            VPSubmission vpSubmission = mock(VPSubmission.class);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
            when(vpSubmission.getError()).thenReturn(null);
            when(vpSubmission.getResponseCode()).thenReturn("expectedCode");

            ResponseCodeException exception = assertThrows(ResponseCodeException.class, () ->
                    verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
            assertEquals(ErrorCode.RESPONSE_CODE_NOT_MATCHING, exception.getErrorCode());
        }

        @Test
        void testGetVPSessionResults_responseCodeExpired() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("code123");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            VPSubmission vpSubmission = mock(VPSubmission.class);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
            when(vpSubmission.getError()).thenReturn(null);
            when(vpSubmission.getResponseCode()).thenReturn("code123");
            when(vpSubmission.getResponseCodeExpiryAt()).thenReturn(new Timestamp(Instant.now().minus(10, ChronoUnit.MINUTES).toEpochMilli()));

            ResponseCodeException exception = assertThrows(ResponseCodeException.class, () ->
                    verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
            assertEquals(ErrorCode.RESPONSE_CODE_EXPIRED, exception.getErrorCode());
        }

        @Test
        void testGetVPSessionResults_responseCodeAlreadyUsed() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("code123");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", "presentationDefinitionUri", null, "nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            VPSubmission vpSubmission = mock(VPSubmission.class);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
            when(vpSubmission.getError()).thenReturn(null);
            when(vpSubmission.getResponseCode()).thenReturn("code123");
            when(vpSubmission.getResponseCodeExpiryAt()).thenReturn(new Timestamp(Instant.now().plus(10, ChronoUnit.MINUTES).toEpochMilli()));
            when(vpSubmissionRepository.markResponseCodeAsUsed(any())).thenReturn(0);

            ResponseCodeException exception = assertThrows(ResponseCodeException.class, () ->
                    verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
            assertEquals(ErrorCode.RESPONSE_CODE_USED, exception.getErrorCode());
        }
    }

    @Nested
    class FetchVpSubmissionIfValid {
        @Test
        public void testFetchVpSubmissionIfValid_Success_NoResponseCode() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.empty());

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class, String.class, AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);
            VPSubmission result =
                    (VPSubmission) method.invoke(verifiablePresentationSubmissionService, requestIds, null, null, false);

            assertNotNull(result);
            assertEquals(requestId, result.getRequestId());
            verify(vpSubmissionRepository, times(1)).findAllById(requestIds);
            verify(vpSubmissionRepository, never()).save(any());
        }

        @Test
        public void testFetchVpSubmissionIfValid_Success_CrossDevice() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    false
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);
            VPSubmission result =
                    (VPSubmission) method.invoke(verifiablePresentationSubmissionService, requestIds, null, authResponse, false);

            assertNotNull(result);
            assertEquals(requestId, result.getRequestId());
        }

        @Test
        public void testFetchVpSubmissionIfValid_Success_WithResponseCode_SameDevice() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));
            when(vpSubmissionRepository.markResponseCodeAsUsed(requestId)).thenReturn(1);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);
            VPSubmission result =
                    (VPSubmission) method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true);

            assertNotNull(result);
            assertEquals(requestId, result.getRequestId());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsVPSubmissionNotFoundException_WhenNoSubmissionFound() throws Exception {
            List<String> requestIds = List.of("req123");
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(Collections.emptyList());

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, null, null, true));

            assertInstanceOf(VPSubmissionNotFoundException.class, exception.getCause());
            verify(vpSubmissionRepository, times(1)).findAllById(requestIds);
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsResponseCodeException_WhenResponseCodeValidationRequiredAndSubmissionHasNoResponseCode() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_NOT_FOUND, responseCodeException.getErrorCode());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsResponseCodeException_WhenResponseCodeNotEqual() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";
            String wrongResponseCode = "wrongCode";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, wrongResponseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_NOT_MATCHING, responseCodeException.getErrorCode());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsResponseCodeException_WhenResponseCodeExpired() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";
            Timestamp expiredAt = Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiredAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_EXPIRED, responseCodeException.getErrorCode());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsResponseCodeException_WhenResponseCodeAlreadyUsed() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    true  // responseCodeUsed = true
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_USED, responseCodeException.getErrorCode());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ThrowsVPSubmissionWalletError_WhenErrorPresent() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String error = "wallet_error";
            String errorDescription = "Error from wallet";

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    error,
                    errorDescription,
                    null,
                    null,
                    false
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.empty());

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, null, null, true));

            assertInstanceOf(VPSubmissionWalletError.class, exception.getCause());
            VPSubmissionWalletError walletError = (VPSubmissionWalletError) exception.getCause();
            assertEquals(error, walletError.getErrorCode());
            assertEquals(errorDescription, walletError.getErrorDescription());
        }

        @Test
        public void testFetchVpSubmissionIfValid_DoesNotValidateExpiry_WhenResponseCodeValidationRequiredIsFalse() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "code123";
            Timestamp expiredAt = Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiredAt,
                    false
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.empty());
            when(vpSubmissionRepository.markResponseCodeAsUsed(requestId)).thenReturn(1);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);
            VPSubmission result =
                    (VPSubmission) method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, null, true);

            assertNotNull(result);
            assertEquals(requestId, result.getRequestId());
        }

        @Test
        public void testFetchVpSubmissionIfValid_ValidResponseCode_MarksAsUsed() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "valid-code-123";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );

            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));
            when(vpSubmissionRepository.markResponseCodeAsUsed(requestId)).thenReturn(1);

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class
                            , String.class,
                            AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);
            VPSubmission result =
                    (VPSubmission) method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true);

            assertNotNull(result);
            assertEquals(requestId, result.getRequestId());
            assertEquals(responseCode, result.getResponseCode());
            // Verify with requestId, not responseCode
            verify(vpSubmissionRepository, times(1)).markResponseCodeAsUsed(requestId);
        }

        @Test
        public void testFetchVpSubmissionIfValid_ResponseCodeExpired_ThrowsException() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String responseCode = "expired-code";
            Timestamp expiredAt = Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiredAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method = VerifiablePresentationSubmissionServiceImpl.class
                    .getDeclaredMethod("fetchVpSubmissionIfValid", List.class, String.class, AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class, () -> method.invoke(verifiablePresentationSubmissionService, requestIds, responseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_EXPIRED, responseCodeException.getErrorCode());
        }

        @Test
        public void testFetchVpSubmissionIfValid_MismatchedResponseCode_ThrowsException() throws Exception {
            List<String> requestIds = List.of("req123");
            String requestId = "req123";
            String storedResponseCode = "stored-code-123";
            String providedResponseCode = "different-code-456";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    requestId,
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    storedResponseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    "presentationDefinitionUri",
                    null,
                    "nonce",
                    "responseUri",
                    false,
                    true
            );
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    requestId,
                    "transactionId",
                    authDetails,
                    System.currentTimeMillis() + 100000
            );

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(authorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authResponse));

            Method method =
                    VerifiablePresentationSubmissionServiceImpl.class.getDeclaredMethod("fetchVpSubmissionIfValid", List.class, String.class, AuthorizationRequestCreateResponse.class, boolean.class);
            method.setAccessible(true);

            Exception exception =
                    assertThrows(InvocationTargetException.class,
                            () -> method.invoke(verifiablePresentationSubmissionService, requestIds, providedResponseCode, authResponse, true));

            assertInstanceOf(ResponseCodeException.class, exception.getCause());
            ResponseCodeException responseCodeException = (ResponseCodeException) exception.getCause();
            assertEquals(ErrorCode.RESPONSE_CODE_NOT_MATCHING, responseCodeException.getErrorCode());
        }
    }

    @Nested
    class TestResponseCode {
        @Test
        public void testResponseCodeUsed_InitiallyFalse() {
            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    "code123",
                    Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES)),
                    false
            );

            assertFalse(vpSubmission.isResponseCodeUsed(), "responseCodeUsed should be false initially");
        }

        @Test
        public void testResponseCodeUsed_CanBeTrue() {
            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    "code123",
                    Timestamp.from(Instant.now().plus(5, ChronoUnit.MINUTES)),
                    true
            );

            assertTrue(vpSubmission.isResponseCodeUsed(), "responseCodeUsed can be set to true");
        }

        @Test
        public void testResponseCode_CanBeNull() {
            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    null,
                    null,
                    false
            );

            assertNull(vpSubmission.getResponseCode(), "responseCode can be null for cross-device flow");
            assertNull(vpSubmission.getResponseCodeExpiryAt(), "responseCodeExpiryAt should be null when responseCode is null");
        }

        @Test
        public void testResponseCode_WithExpiry() {
            String responseCode = "code456";
            Timestamp expiryAt = Timestamp.from(Instant.now().plus(10, ChronoUnit.MINUTES));

            VPSubmission vpSubmission = new VPSubmission(
                    "state123",
                    "vpToken",
                    new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            assertEquals(responseCode, vpSubmission.getResponseCode());
            assertEquals(expiryAt, vpSubmission.getResponseCodeExpiryAt());
            assertNotNull(vpSubmission.getResponseCodeExpiryAt());
            assertTrue(vpSubmission.getResponseCodeExpiryAt().toInstant().isAfter(Instant.now()),
                    "Response code expiry should be in the future");
        }
    }

    @Nested
    class TestSingleCredential {
        @Test
        void testVerifySingleCredential_NonSdJwt() {
            VerificationRequestDto request = new VerificationRequestDto();
            String vcData = "jwt.vc.data";

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setAllChecksSuccessful(true);
            mockResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class))).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifySingleCredential",
                    request, vcData, false);

            assertNotNull(results);
            assertNull(results.getHolderProofCheck(), "HolderProof should be null for non-SD-JWT");
            assertEquals(vcData, results.getVerifiableCredential());
        }

        @Test
        void testVerifySingleCredential_SdJwt_Valid() {
            VerificationRequestDto request = new VerificationRequestDto();

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class))).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifySingleCredential",
                    request, "sd-jwt-content", true);

            assertNotNull(results);
            assertTrue(results.getHolderProofCheck().isValid());
            assertNull(results.getHolderProofCheck().getError());
        }

        @Test
        void testVerifySingleCredential_SdJwt_InvalidWithError() {
            VerificationRequestDto request = new VerificationRequestDto();
            String validEnumName = KBJwtErrorCodes.ERR_INVALID_KB_SIGNATURE.name();

            ErrorDto errorDto = new ErrorDto(validEnumName, "Key binding failed");
            SchemaAndSignatureCheckDto signatureCheck = new SchemaAndSignatureCheckDto(false, errorDto);

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setSchemaAndSignatureCheck(signatureCheck);

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class))).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifySingleCredential",
                    request, "sd-jwt-content", true);

            assertNotNull(results);
            assertNotNull(results.getHolderProofCheck(), "HolderProofCheck should not be null if the error code matched an enum");
            assertFalse(results.getHolderProofCheck().isValid());
            assertEquals(validEnumName, results.getHolderProofCheck().getError().getErrorCode());
        }
    }

    @Nested
    class ExtractTokens {
        @Test
        public void testExtractTokens_MixedArray() {
            String vcJson = "{\"type\":[\"VerifiableCredential\"]}";
            String base64Token = Base64.getUrlEncoder().encodeToString(vcJson.getBytes());
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"vc+sd-jwt\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"123\"}".getBytes());
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("sig".getBytes());
            String sdJwtToken = header + "." + payload + "." + signature;
            String arrayToken = "[\"" + base64Token + "\",\"" + sdJwtToken + "\"]";
            VPTokenDto vpTokenDto = verifiablePresentationSubmissionService.extractTokens(arrayToken);

            assertEquals(1, vpTokenDto.getJsonVpTokens().size());
            assertEquals(1, vpTokenDto.getSdJwtVpTokens().size());
        }

        @Test
        public void testExtractTokens_InvalidBase64() {
            String arrayToken = "[\"invalid-base64!!!\"]";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(arrayToken));
        }
    }
}

