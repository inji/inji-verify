package io.inji.verify.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.Gson;
import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.CredentialResultsDto;
import io.inji.verify.dto.result.VPTokenDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.dto.dcql.CredentialMetaDto;
import io.inji.verify.dto.dcql.CredentialQueryDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.dto.result.DcqlTokensDto;
import io.inji.verify.enums.KBJwtErrorCodes;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.*;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.testsupport.DcqlTestFixtures;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.data.*;
import io.inji.verify.repository.VPSubmissionRepository;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
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
        verifiablePresentationSubmissionService = new VerifiablePresentationSubmissionServiceImpl(vpSubmissionRepository, credentialsVerifier, presentationVerifier, verifiablePresentationRequestService, vcVerificationService, pixelPass, authorizationRequestCreateResponseRepository, gson, validator, new ObjectMapper());
    }

    private static VPSubmission vpSubmission(
            String requestId,
            String vpToken,
            String error,
            String errorDescription,
            String responseCode,
            Timestamp responseCodeExpiryAt,
            boolean responseCodeUsed) {
        return new VPSubmission(
                requestId,
                vpToken,
                null,
                error,
                errorDescription,
                responseCode,
                responseCodeExpiryAt,
                responseCodeUsed);
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

            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2020\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}]}",
                    null, "", "", null, false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null, "nonce", "responseUri", false, false);
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
            VPSubmission vpSubmission = vpSubmission("state123", "\"" + base64Token + "\"",
                    "", "", "", null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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
            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]},{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "[\"" + base64Token1 + "\", \"{\\\"type\\\":[\\\"VerifiablePresentation\\\"],\\\"proof\\\":{\\\"type\\\":\\\"Ed25519Signature2018\\\"},\\\"VerifiablePresentation\\\":[{\\\"type\\\":[\\\"VerifiablePresentation\\\"]}]}\"]",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null,  null, null
                    , false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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
        public void testGetVPResult_TokenMatchingFailed_NullRequest() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null,
                    false);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(null);

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }

        @Test
        public void testGetVPResult_TokenMatchingFailedException() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null,
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(authResponse);
            when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), any()))
                    .thenThrow(new RuntimeException("Verification error"));

            VPTokenResultDto resultDto =
                    verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

            assertNotNull(resultDto);
            assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        }



        @Test
        public void testGetVPResult_EmptyVpVerificationStatuses() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
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
        public void testGetVPResult_MixedVerificationStatuses() {
            List<String> requestIds = List.of("req123");
            String transactionId = "tx123";

            List<VCResultWithCredentialStatus> vcResults = Arrays.asList(
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.SUCCESS, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.REVOKED, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.EXPIRED, new HashMap<>()),
                    new VCResultWithCredentialStatus("Verified successfully", VerificationStatus.INVALID, new HashMap<>())
            );

            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]},{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]},{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2020\"},\"verifiableCredential\":[{\"type\":[\"VerifiablePresentation\"]}]}]}",
                    null,
                    null,
                    null, null, false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "state", true, false);

            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(authDetails);

            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(any())).thenReturn(auth);
            when(vpSubmissionRepository.findAllById(any())).thenReturn(List.of(
                    vpSubmission("st", vpToken, null, "", "",
                            null, false)));

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            VerificationResult vResult = mock(VerificationResult.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(vResult.getVerificationStatus()).thenReturn(true);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), any(), anyList(), anyBoolean()))
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
            VPSubmission vpSubmission = vpSubmission("state123",
                    "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}]}",
                    null, null, null, null,
                    false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    vc.toString(),
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
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.LDP_VC), anyList(), anyBoolean()))
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
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"dc+sd-jwt\"}".getBytes());
            // cnf claim is required when require_cryptographic_holder_binding=true (default for unknown query IDs)
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"123\",\"cnf\":{\"kid\":\"k1\"}}".getBytes());
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("signature".getBytes());
            String kbHeader = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"kb+jwt\"}".getBytes());
            String kbPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"nonce\":\"abc\"}".getBytes());
            String kbSig = Base64.getUrlEncoder().withoutPadding().encodeToString("kbsig".getBytes());
            // SD-JWT with cnf and KB-JWT: header.payload.sig~disclosure~kb-header.kb-payload.kb-sig
            String sdJwtToken = header + "." + payload + "." + signature
                    + "~disclosure~" + kbHeader + "." + kbPayload + "." + kbSig;
            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "{\"age_credential\":[\"" + sdJwtToken + "\"]}",
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
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", false, false);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.DC_SD_JWT), anyList(), anyBoolean()))
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

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce", "responseUri", true, true);
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
            String vpToken = "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"John Doe\"}}]}]}";
            VPSubmission vpSubmission = vpSubmission("state123", vpToken,
                    null,
                    null, null, null, true);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(),null, "nonce",
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
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", true, true);
            AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse(
                    "state123", transactionId, authDetails, System.currentTimeMillis() + 100000);

            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);

            assertThrows(VPSubmissionNotFoundException.class, () -> verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId));
        }

        @Test
        void testGetVPSessionResults_submissionNotFound() {
            VerificationSessionRequestDto request = new VerificationSessionRequestDto();
            request.setResponseCode("code123");
            String transactionId = "tx123";
            List<String> requestIds = List.of("req123");

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", true, false);
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
            String vpToken = "{\"age_credential\":[{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"John Doe\"}}]}]}";
            VPSubmission vpSubmission = vpSubmission("state123", vpToken,
                    null,
                    null, null, null, false);

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", false, false);
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

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", true, true);
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

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", true, true);
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

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("clientId", DcqlTestFixtures.minimalDcqlDto(), null,"nonce", "responseUri", true, true);
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
            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    null,
                    null,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiredAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    true  // responseCodeUsed = true
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    responseCode,
                    expiredAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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

            VPSubmission vpSubmission = vpSubmission(
                    requestId,
                    "vpToken",
                    null,
                    null,
                    storedResponseCode,
                    expiryAt,
                    false
            );

            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId",
                    DcqlTestFixtures.minimalDcqlDto(),
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
            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "vpToken",
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
            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "vpToken",
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
            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "vpToken",
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

            VPSubmission vpSubmission = vpSubmission(
                    "state123",
                    "vpToken",
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
        void testverifyAndGetCredentialStatusV2_NonSdJwt() {
            VerificationRequestDto request = new VerificationRequestDto();
            String vcData = "jwt.vc.data";

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setAllChecksSuccessful(true);
            mockResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class), anyBoolean())).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifyAndGetCredentialStatusV2",
                    request, vcData, false, false);

            assertNotNull(results);
            assertNull(results.getHolderProofCheck(), "HolderProof should be null for non-SD-JWT");
            assertEquals(vcData, results.getVerifiableCredential());
        }

        @Test
        void testverifyAndGetCredentialStatusV2_SdJwt_Valid() {
            VerificationRequestDto request = new VerificationRequestDto();

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class), anyBoolean())).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifyAndGetCredentialStatusV2",
                    request, "sd-jwt-content", true, true);

            assertNotNull(results);
            assertTrue(results.getHolderProofCheck().isValid());
            assertNull(results.getHolderProofCheck().getError());
        }

        @Test
        void testverifyAndGetCredentialStatusV2_SdJwt_InvalidWithError() {
            VerificationRequestDto request = new VerificationRequestDto();
            String validEnumName = KBJwtErrorCodes.ERR_INVALID_KB_SIGNATURE.name();

            ErrorDto errorDto = new ErrorDto(validEnumName, "Key binding failed");
            SchemaAndSignatureCheckDto signatureCheck = new SchemaAndSignatureCheckDto(false, errorDto);

            VCVerificationResultDto mockResult = new VCVerificationResultDto();
            mockResult.setSchemaAndSignatureCheck(signatureCheck);

            when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class), anyBoolean())).thenReturn(mockResult);

            CredentialResultsDto results = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService,
                    "verifyAndGetCredentialStatusV2",
                    request, "sd-jwt-content", true, true);

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
            String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"typ\":\"dc+sd-jwt\"}".getBytes());
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

        @Test
        public void testExtractTokens_NullVpToken() {
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(null));
        }

        @Test
        public void testExtractTokens_EmptyVpToken() {
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(""));
        }

        @Test
        public void testExtractTokens_MalformedJsonVpToken() {
            String malformedJson = "{invalid json}";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(malformedJson));
        }

        @Test
        public void testExtractTokens_MalformedArrayVpToken() {
            String malformedArray = "[invalid array format";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(malformedArray));
        }

        @Test
        public void testExtractTokens_EmptyArrayVpToken() {
            String emptyArray = "[]";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(emptyArray));
        }

        @Test
        public void testExtractTokens_ArrayWithEmptyStrings() {
            String arrayWithEmptyStrings = "[\"\", \"\"]";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(arrayWithEmptyStrings));
        }

        @Test
        public void testExtractTokens_InvalidBase64InArray() {
            String arrayToken = "[\"valid-base64-first\", \"invalid!!!base64\"]";
            assertThrows(InvalidVpTokenException.class, () -> verifiablePresentationSubmissionService.extractTokens(arrayToken));
        }
    }

    @Nested
    class ExtractDcqlTokens {

        private static final String H = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"typ\":\"dc+sd-jwt\"}".getBytes());
        private static final String PAYLOAD_NO_CNF = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"123\"}".getBytes());
        private static final String PAYLOAD_WITH_CNF = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"123\",\"cnf\":{\"kid\":\"k1\"}}".getBytes());
        private static final String SIG = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("sig".getBytes());
        private static final String KB_HEADER = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"typ\":\"kb+jwt\"}".getBytes());
        private static final String KB_PAYLOAD = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"cnf\":\"abc\"}".getBytes());
        private static final String KB_SIG = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("kbsig".getBytes());

        /** dc+sd-jwt with cnf claim and KB-JWT appended */
        private static final String SD_JWT_WITH_CNF_AND_KB =
                H + "." + PAYLOAD_WITH_CNF + "." + SIG + "~" + KB_HEADER + "." + KB_PAYLOAD + "." + KB_SIG;

        /** dc+sd-jwt with cnf claim but no KB-JWT (trailing ~) */
        private static final String SD_JWT_WITH_CNF_NO_KB =
                H + "." + PAYLOAD_WITH_CNF + "." + SIG + "~disclosure~";

        /** dc+sd-jwt without cnf claim */
        private static final String SD_JWT_WITHOUT_CNF =
                H + "." + PAYLOAD_NO_CNF + "." + SIG + "~";

        private AuthorizationRequestResponseDto buildAuthRequest(boolean holderBinding) {
            DCQLQueryDto dcql = new DCQLQueryDto(
                    List.of(new CredentialQueryDto(
                            "cred1", "dc+sd-jwt",
                            new CredentialMetaDto(List.of("cred1"), null),
                            holderBinding, false, null, null)),
                    null);
            return new AuthorizationRequestResponseDto("clientId", dcql, null, "nonce", "responseUri", false, false);
        }

        @Test
        void shouldThrow_whenSdJwtMissingCnf_andHolderBindingRequired() {
            String vpToken = "{\"cred1\":[\"" + SD_JWT_WITHOUT_CNF + "\"]}";
            AuthorizationRequestResponseDto authRequest = buildAuthRequest(true);

            InvalidVpTokenException ex = assertThrows(InvalidVpTokenException.class,
                    () -> verifiablePresentationSubmissionService.extractDcqlTokens(vpToken, authRequest));
            assertTrue(ex.getMessage().contains("missing cnf claim"));
        }

        @Test
        void shouldThrow_whenSdJwtHasCnf_butMissingKbJwt_andHolderBindingRequired() {
            String vpToken = "{\"cred1\":[\"" + SD_JWT_WITH_CNF_NO_KB + "\"]}";
            AuthorizationRequestResponseDto authRequest = buildAuthRequest(true);

            InvalidVpTokenException ex = assertThrows(InvalidVpTokenException.class,
                    () -> verifiablePresentationSubmissionService.extractDcqlTokens(vpToken, authRequest));
            assertTrue(ex.getMessage().contains("missing required Key Binding JWT"));
        }

        @Test
        void shouldSucceed_whenSdJwtHasCnfAndKbJwt_andHolderBindingRequired() throws InvalidVpTokenException {
            String vpToken = "{\"cred1\":[\"" + SD_JWT_WITH_CNF_AND_KB + "\"]}";
            AuthorizationRequestResponseDto authRequest = buildAuthRequest(true);

            DcqlTokensDto result = verifiablePresentationSubmissionService.extractDcqlTokens(vpToken, authRequest);
            assertNotNull(result);
            assertTrue(result.getSdJwtTokens().containsKey("cred1"));
            assertEquals(1, result.getSdJwtTokens().get("cred1").size());
        }

        @Test
        void shouldSucceed_whenSdJwtMissingCnf_andHolderBindingNotRequired() throws InvalidVpTokenException {
            String vpToken = "{\"cred1\":[\"" + SD_JWT_WITHOUT_CNF + "\"]}";
            AuthorizationRequestResponseDto authRequest = buildAuthRequest(false);

            DcqlTokensDto result = verifiablePresentationSubmissionService.extractDcqlTokens(vpToken, authRequest);
            assertNotNull(result);
            assertTrue(result.getSdJwtTokens().containsKey("cred1"));
            assertEquals(1, result.getSdJwtTokens().get("cred1").size());
        }
    }

    @Nested
    class TestProcessSubmissionV2 {

        // VP token with proof (signed) containing a valid VC
        private static final String SIGNED_LDP_VP_TOKEN =
                "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"}," +
                "\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"Alice\"}}]}";

        // PE-based authRequest (presentationDefinition non-null, dcqlQuery null)
        private AuthorizationRequestCreateResponse peAuthResponse(String transactionId, boolean acceptWithoutProof) {
            io.inji.verify.dto.presentation.VPDefinitionResponseDto pd =
                    new io.inji.verify.dto.presentation.VPDefinitionResponseDto("pd1", List.of(), null, null, null, null);
            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", null, pd, "nonce", "responseUri", acceptWithoutProof, false);
            return new AuthorizationRequestCreateResponse("state1", transactionId, authDetails,
                    System.currentTimeMillis() + 100000);
        }

        // DCQL-based authRequest (defaulting CHB=true for unknown queryIds)
        private AuthorizationRequestCreateResponse dcqlAuthResponse(String transactionId) {
            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null, "nonce", "responseUri", false, false);
            return new AuthorizationRequestCreateResponse("state1", transactionId, authDetails,
                    System.currentTimeMillis() + 100000);
        }

        // DCQL-based authRequest with age_credential query having require_cryptographic_holder_binding=false
        private AuthorizationRequestCreateResponse dcqlAuthResponseNoCHB(String transactionId) {
            io.inji.verify.dto.dcql.DCQLQueryDto dcql = new io.inji.verify.dto.dcql.DCQLQueryDto(
                    List.of(new io.inji.verify.dto.dcql.CredentialQueryDto(
                            "age_credential", "ldp_vc",
                            new io.inji.verify.dto.dcql.CredentialMetaDto(null, List.of(List.of("VerifiableCredential"))),
                            false, false, null, null)),
                    null);
            AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto(
                    "clientId", dcql, null, "nonce", "responseUri", false, false);
            return new AuthorizationRequestCreateResponse("state1", transactionId, authDetails,
                    System.currentTimeMillis() + 100000);
        }

        // Mock VPSubmission with a non-empty descriptor map so token matching passes
        private VPSubmission signedVpSubmission(String vpToken) {
            VPSubmission sub = mock(VPSubmission.class);
            when(sub.getVpToken()).thenReturn("[" + vpToken + "]");
            when(sub.getPresentationSubmission()).thenReturn(
                    new io.inji.verify.dto.submission.PresentationSubmissionDto(
                            "ps1", "pd1",
                            List.of(new io.inji.verify.dto.submission.DescriptorMapDto("desc1", "ldp_vp", "$.verifiableCredential[0]", null))));
            when(sub.getError()).thenReturn(null);
            when(sub.getResponseCode()).thenReturn(null);
            return sub;
        }

        @Test
        void testProcessSubmissionV2_PE_SignedVP_SkipStatusChecks_Success() {
            String transactionId = "tx1";
            List<String> requestIds = List.of("req1");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            VPSubmission sub = signedVpSubmission(SIGNED_LDP_VP_TOKEN);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(peAuthResponse(transactionId, false));

            PresentationVerificationResultV2 pvResult = mock(PresentationVerificationResultV2.class);
            VerificationResult proofResult = new VerificationResult(true, "", "");
            VerificationResult vcResult = new VerificationResult(true, "", "");
            VCResultV2 vcRes = new VCResultV2("{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"Alice\"}}", vcResult);
            when(pvResult.getProofVerificationResult()).thenReturn(proofResult);
            when(pvResult.getVcResults()).thenReturn(List.of(vcRes));
            when(presentationVerifier.verifyV2(anyString())).thenReturn(pvResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, result.getCredentialResults().size());
            assertTrue(result.getCredentialResults().getFirst().getHolderProofCheck().isValid());
        }

        @Test
        void testProcessSubmissionV2_PE_SignedVP_WithStatusChecks_Success() {
            String transactionId = "tx2";
            List<String> requestIds = List.of("req2");
            VerificationRequestDto request = new VerificationRequestDto(false, List.of("revocation"), false);

            VPSubmission sub = signedVpSubmission(SIGNED_LDP_VP_TOKEN);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(peAuthResponse(transactionId, false));

            PresentationResultWithCredentialStatusV2 pvResult = mock(PresentationResultWithCredentialStatusV2.class);
            VerificationResult proofResult = new VerificationResult(true, "", "");
            VerificationResult vcVResult = new VerificationResult(true, "", "");
            VCResultWithCredentialStatusV2 vcRes = new VCResultWithCredentialStatusV2(
                    "{\"type\":[\"VerifiableCredential\"]}", vcVResult, Map.of());
            when(pvResult.getProofVerificationResult()).thenReturn(proofResult);
            when(pvResult.getVcResults()).thenReturn(List.of(vcRes));
            when(presentationVerifier.verifyAndGetCredentialStatusV2(anyString(), anyList())).thenReturn(pvResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, result.getCredentialResults().size());
            assertTrue(result.getCredentialResults().getFirst().getHolderProofCheck().isValid());
        }

        @Test
        void testProcessSubmissionV2_PE_UnsignedVP_AcceptedWithCredentials_Success() {
            String transactionId = "tx3";
            List<String> requestIds = List.of("req3");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            String unsignedVp = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"]}]}";
            VPSubmission sub = signedVpSubmission(unsignedVp);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(peAuthResponse(transactionId, true)); // acceptVPWithoutHolderProof=true

            VCVerificationResultDto mockVcResult = new VCVerificationResultDto();
            mockVcResult.setAllChecksSuccessful(true);
            mockVcResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));
            when(vcVerificationService.verifyV2(any(), anyBoolean())).thenReturn(mockVcResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertFalse(result.getCredentialResults().isEmpty());
        }

        @Test
        void testProcessSubmissionV2_PE_UnsignedVP_NotAccepted_ThrowsVPWithoutProofException() {
            String transactionId = "tx4";
            List<String> requestIds = List.of("req4");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            String unsignedVp = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[]}";
            VPSubmission sub = signedVpSubmission(unsignedVp);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(peAuthResponse(transactionId, false)); // acceptVPWithoutHolderProof=false

            assertThrows(VPWithoutProofException.class, () ->
                    verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId));
        }

        @Test
        void testProcessSubmissionV2_PE_TokenNotMatching_ThrowsTokenMatchingFailedException() {
            String transactionId = "tx5";
            List<String> requestIds = List.of("req5");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            // No presentationSubmission → descriptorMap null → token mismatch
            VPSubmission sub = vpSubmission("state1", "[" + SIGNED_LDP_VP_TOKEN + "]",
                    null, null, null, null, false);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(peAuthResponse(transactionId, false));

            assertThrows(TokenMatchingFailedException.class, () ->
                    verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId));
        }

        @Test
        void testProcessSubmissionV2_DCQL_LDPVPSkipStatusChecks_Success() {
            String transactionId = "tx6";
            List<String> requestIds = List.of("req6");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            String vpToken = "{\"age_credential\":[" + SIGNED_LDP_VP_TOKEN + "]}";
            VPSubmission sub = vpSubmission("state1", vpToken, null, null, null, null, false);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponse(transactionId));

            PresentationVerificationResultV2 pvResult = mock(PresentationVerificationResultV2.class);
            VerificationResult proofResult = new VerificationResult(true, "", "");
            VCResultV2 vcRes = new VCResultV2("{\"type\":[\"VerifiableCredential\"]}", new VerificationResult(true, "", ""));
            when(pvResult.getProofVerificationResult()).thenReturn(proofResult);
            when(pvResult.getVcResults()).thenReturn(List.of(vcRes));
            when(presentationVerifier.verifyV2(anyString())).thenReturn(pvResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, result.getCredentialResults().size());
        }

        @Test
        void testProcessSubmissionV2_DCQL_LDPVPWithStatusChecks_Success() {
            String transactionId = "tx7";
            List<String> requestIds = List.of("req7");
            VerificationRequestDto request = new VerificationRequestDto(false, List.of("revocation"), false);

            String vpToken = "{\"age_credential\":[" + SIGNED_LDP_VP_TOKEN + "]}";
            VPSubmission sub = vpSubmission("state1", vpToken, null, null, null, null, false);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponse(transactionId));

            PresentationResultWithCredentialStatusV2 pvResult = mock(PresentationResultWithCredentialStatusV2.class);
            VerificationResult proofResult = new VerificationResult(true, "", "");
            VCResultWithCredentialStatusV2 vcRes = new VCResultWithCredentialStatusV2(
                    "{\"type\":[\"VerifiableCredential\"]}", new VerificationResult(true, "", ""), Map.of());
            when(pvResult.getProofVerificationResult()).thenReturn(proofResult);
            when(pvResult.getVcResults()).thenReturn(List.of(vcRes));
            when(presentationVerifier.verifyAndGetCredentialStatusV2(anyString(), anyList())).thenReturn(pvResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, result.getCredentialResults().size());
        }

        @Test
        void testProcessSubmissionV2_DCQL_LDPVCTokens_Success() {
            String transactionId = "tx8";
            List<String> requestIds = List.of("req8");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            // LDP VC token (no proof, not an SD-JWT) wrapped in DCQL map
            // require_cryptographic_holder_binding=false so the VC (not VP) format is accepted
            String vpToken = "{\"age_credential\":[{\"type\":[\"VerifiableCredential\"],\"credentialSubject\":{\"name\":\"Bob\"}}]}";
            VPSubmission sub = vpSubmission("state1", vpToken, null, null, null, null, false);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponseNoCHB(transactionId));

            VCVerificationResultDto mockVcResult = new VCVerificationResultDto();
            mockVcResult.setAllChecksSuccessful(true);
            mockVcResult.setSchemaAndSignatureCheck(new SchemaAndSignatureCheckDto(true, null));
            when(vcVerificationService.verifyV2(any(), anyBoolean())).thenReturn(mockVcResult);

            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);

            assertTrue(result.isAllChecksSuccessful());
            assertEquals(1, result.getCredentialResults().size());
        }

        @Test
        void testProcessSubmissionV2_GenericException_ThrowsVPVerificationException() {
            String transactionId = "tx9";
            List<String> requestIds = List.of("req9");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            String vpToken = "{\"age_credential\":[" + SIGNED_LDP_VP_TOKEN + "]}";
            VPSubmission sub = vpSubmission("state1", vpToken, null, null, null, null, false);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponse(transactionId));
            when(presentationVerifier.verifyV2(anyString())).thenThrow(new RuntimeException("unexpected"));

            assertThrows(VPVerificationException.class, () ->
                    verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId));
        }

        @Test
        void testGetVPResultV2_SubmissionNotFound_ThrowsVPSubmissionNotFoundException() {
            String transactionId = "tx10";
            List<String> requestIds = List.of("req10");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(Collections.emptyList());
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponse(transactionId));

            assertThrows(VPSubmissionNotFoundException.class, () ->
                    verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId));
        }

        @Test
        void testGetVPResultV2_WalletError_ThrowsVPSubmissionWalletError() {
            String transactionId = "tx11";
            List<String> requestIds = List.of("req11");
            VerificationRequestDto request = new VerificationRequestDto(true, List.of(), false);

            VPSubmission sub = mock(VPSubmission.class);
            when(sub.getError()).thenReturn("wallet_error");
            when(sub.getResponseCode()).thenReturn(null);
            when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(sub));
            when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                    .thenReturn(dcqlAuthResponse(transactionId));

            assertThrows(VPSubmissionWalletError.class, () ->
                    verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId));
        }
    }

    @Nested
    class TestPrivateHelpers {

        @Test
        void testIsAuthRequestWithPresentationExchange_NullAuthRequest() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAuthRequestWithPresentationExchange",
                    (AuthorizationRequestCreateResponse) null);
            assertFalse(result);
        }

        @Test
        void testIsAuthRequestWithPresentationExchange_NullAuthDetails() {
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(null);
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAuthRequestWithPresentationExchange", auth);
            assertFalse(result);
        }

        @Test
        void testIsAuthRequestWithPresentationExchange_WithPresentationDefinition() {
            io.inji.verify.dto.presentation.VPDefinitionResponseDto pd =
                    new io.inji.verify.dto.presentation.VPDefinitionResponseDto("pd-id", List.of(), null, null, null, null);
            AuthorizationRequestResponseDto details = new AuthorizationRequestResponseDto(
                    "clientId", null, pd, "nonce", "uri", false, false);
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(details);

            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAuthRequestWithPresentationExchange", auth);
            assertTrue(result);
        }

        @Test
        void testIsAuthRequestWithPresentationExchange_WithDcqlQuery() {
            AuthorizationRequestResponseDto details = new AuthorizationRequestResponseDto(
                    "clientId", DcqlTestFixtures.minimalDcqlDto(), null, "nonce", "uri", false, false);
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(details);

            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAuthRequestWithPresentationExchange", auth);
            assertFalse(result);
        }

        @Test
        void testIsAcceptVPWithoutHolderProof_True() {
            AuthorizationRequestResponseDto details = new AuthorizationRequestResponseDto(
                    "clientId", null, null, "nonce", "uri", true, false);
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(details);

            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAcceptVPWithoutHolderProof", auth);
            assertTrue(result);
        }

        @Test
        void testIsAcceptVPWithoutHolderProof_False() {
            AuthorizationRequestResponseDto details = new AuthorizationRequestResponseDto(
                    "clientId", null, null, "nonce", "uri", false, false);
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(details);

            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAcceptVPWithoutHolderProof", auth);
            assertFalse(result);
        }

        @Test
        void testIsAcceptVPWithoutHolderProof_NullAuthDetails() {
            AuthorizationRequestCreateResponse auth = mock(AuthorizationRequestCreateResponse.class);
            when(auth.getAuthorizationDetails()).thenReturn(null);

            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isAcceptVPWithoutHolderProof", auth);
            assertFalse(result);
        }

        @Test
        void testPopulateHolderProofDto_Valid() {
            VerificationResult vr = new VerificationResult(true, "", "");
            io.inji.verify.dto.result.HolderProofCheckDto result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "populateHolderProofDto", vr);
            assertNotNull(result);
            assertTrue(result.isValid());
            assertNull(result.getError());
        }

        @Test
        void testPopulateHolderProofDto_Invalid() {
            // Kotlin VerificationResult constructor order: (status, verificationMessage, verificationErrorCode)
            VerificationResult vr = new VerificationResult(false, "some error", "ERR_CODE");
            io.inji.verify.dto.result.HolderProofCheckDto result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "populateHolderProofDto", vr);
            assertNotNull(result);
            assertFalse(result.isValid());
            assertNotNull(result.getError());
            assertEquals("ERR_CODE", result.getError().getErrorCode());
        }

        @Test
        void testGetListOfVerifiableCredentials_JSONArray() {
            JSONArray arr = new JSONArray();
            arr.put(new JSONObject("{\"type\":\"vc1\"}"));
            arr.put("sd-jwt-string");
            List<Object> result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "getListOfVerifiableCredentials", arr);
            assertEquals(2, result.size());
        }

        @Test
        void testGetListOfVerifiableCredentials_EmptyJSONArray_ThrowsInvalidVpTokenException() {
            assertThrows(InvalidVpTokenException.class, () ->
                    ReflectionTestUtils.invokeMethod(verifiablePresentationSubmissionService,
                            "getListOfVerifiableCredentials", new JSONArray()));
        }

        @Test
        void testGetListOfVerifiableCredentials_JSONObject() {
            JSONObject obj = new JSONObject("{\"type\":\"vc\"}");
            List<Object> result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "getListOfVerifiableCredentials", obj);
            assertEquals(1, result.size());
        }

        @Test
        void testGetListOfVerifiableCredentials_String() {
            List<Object> result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "getListOfVerifiableCredentials", "some-sd-jwt");
            assertEquals(1, result.size());
        }

        @Test
        void testGetListOfVerifiableCredentials_NullOrOtherType_ThrowsInvalidVpTokenException() {
            assertThrows(InvalidVpTokenException.class, () ->
                    ReflectionTestUtils.invokeMethod(verifiablePresentationSubmissionService,
                            "getListOfVerifiableCredentials", 42));
        }

        @Test
        void testIsValidVerifiablePresentation_True() {
            JSONObject vp = new JSONObject("{\"type\":[\"VerifiablePresentation\"]}");
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isValidVerifiablePresentation", vp);
            assertTrue(result);
        }

        @Test
        void testIsValidVerifiablePresentation_False_WrongType() {
            JSONObject vp = new JSONObject("{\"type\":[\"SomethingElse\"]}");
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isValidVerifiablePresentation", vp);
            assertFalse(result);
        }

        @Test
        void testIsVerifiablePresentationSigned_WithProof() {
            JSONObject vp = new JSONObject("{\"proof\":{\"type\":\"Ed25519Signature2018\"}}");
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isVerifiablePresentationSigned", vp);
            assertTrue(result);
        }

        @Test
        void testIsVerifiablePresentationSigned_WithoutProof() {
            JSONObject vp = new JSONObject("{\"type\":[\"VerifiablePresentation\"]}");
            boolean result = ReflectionTestUtils.invokeMethod(
                    verifiablePresentationSubmissionService, "isVerifiablePresentationSigned", vp);
            assertFalse(result);
        }
    }

    @Nested
    class ProcessSdJwtClientIdAndNonce {

        private static final String EXPECTED_NONCE = "test-nonce-value";
        private static final String EXPECTED_CLIENT_ID = "https://verifier.example.com";

        private static String b64(String json) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        }

        private static final String CRED_HEADER = b64("{\"typ\":\"dc+sd-jwt\"}");
        private static final String CRED_PAYLOAD = b64("{\"sub\":\"123\",\"cnf\":{\"kid\":\"k1\"}}");
        private static final String CRED_SIG = b64("sig");
        private static final String KB_HEADER = b64("{\"typ\":\"kb+jwt\"}");
        private static final String KB_SIG = b64("kbsig");
        private static final long NOW_SEC = System.currentTimeMillis() / 1000;

        private static final String SD_JWT_VALID_KB =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + NOW_SEC + "}") + "." + KB_SIG;

        private static final String SD_JWT_WRONG_NONCE =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"wrong-nonce\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + NOW_SEC + "}") + "." + KB_SIG;

        private static final String SD_JWT_WRONG_AUD =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"https://wrong.example.com\",\"iat\":" + NOW_SEC + "}") + "." + KB_SIG;

        /** SD-JWT without a KB-JWT (trailing ~ only — KB-JWT payload will be undecodable). */
        private static final String SD_JWT_NO_KB =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~";

        /** SD-JWT with KB-JWT missing the iat claim. */
        private static final String SD_JWT_IAT_MISSING =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\"}") + "." + KB_SIG;

        /** SD-JWT with KB-JWT iat 2 minutes in the future (beyond 60s clock skew). */
        private static final String SD_JWT_IAT_IN_FUTURE =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + (NOW_SEC + 120) + "}") + "." + KB_SIG;

        /** Builds an auth request with require_cryptographic_holder_binding=true for "cred1". */
        private AuthorizationRequestResponseDto buildAuthRequest() {
            DCQLQueryDto dcql = new DCQLQueryDto(
                    List.of(new CredentialQueryDto(
                            "cred1", "dc+sd-jwt",
                            new CredentialMetaDto(List.of("cred1"), null),
                            true, false, null, null)),
                    null);
            return new AuthorizationRequestResponseDto(EXPECTED_CLIENT_ID, dcql, null, EXPECTED_NONCE, "responseUri", false, false);
        }

        private Map<String, List<String>> tokens(String sdJwt) {
            Map<String, List<String>> map = new HashMap<>();
            map.put("cred1", List.of(sdJwt));
            return map;
        }

        @Test
        void shouldReturnNull_whenKbJwtHasValidNonceAndAud() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(buildAuthRequest(), tokens(SD_JWT_VALID_KB));
            assertNull(result);
        }

        @Test
        void shouldReturnNull_whenSdJwtTokensMapIsEmpty() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(buildAuthRequest(), new HashMap<>());
            assertNull(result);
        }

        @Test
        void shouldReturnClientIdValidationFailed_whenKbJwtAudMismatch() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(buildAuthRequest(), tokens(SD_JWT_WRONG_AUD));
            assertEquals(ErrorCode.CLIENT_ID_VALIDATION_FAILED, result);
        }

        @Test
        void shouldReturnClientIdValidationFailed_whenKbJwtIsAbsent() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(buildAuthRequest(), tokens(SD_JWT_NO_KB));
            assertEquals(ErrorCode.CLIENT_ID_VALIDATION_FAILED, result);
        }

        @Test
        void shouldReturnNonceValidationFailed_whenKbJwtNonceMismatch() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(buildAuthRequest(), tokens(SD_JWT_WRONG_NONCE));
            assertEquals(ErrorCode.NONCE_VALIDATION_FAILED, result);
        }

        @Test
        void shouldReturnNull_whenHolderBindingNotRequired() {
            DCQLQueryDto dcql = new DCQLQueryDto(
                    List.of(new CredentialQueryDto(
                            "cred1", "dc+sd-jwt",
                            new CredentialMetaDto(List.of("cred1"), null),
                            false, false, null, null)),
                    null);
            AuthorizationRequestResponseDto authRequest = new AuthorizationRequestResponseDto(
                    EXPECTED_CLIENT_ID, dcql, null, EXPECTED_NONCE, "responseUri", false, false);

            // Even a token with wrong aud/nonce passes when binding is not required
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtClientIdAndNonce(authRequest, tokens(SD_JWT_WRONG_AUD));
            assertNull(result);
        }
    }

    @Nested
    class ProcessSdJwtKbJwtIat {

        private static final String EXPECTED_NONCE = "test-nonce-value";
        private static final String EXPECTED_CLIENT_ID = "https://verifier.example.com";

        private static String b64(String json) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        }

        private static final String CRED_HEADER = b64("{\"typ\":\"dc+sd-jwt\"}");
        private static final String CRED_PAYLOAD = b64("{\"sub\":\"123\",\"cnf\":{\"kid\":\"k1\"}}");
        private static final String CRED_SIG = b64("sig");
        private static final String KB_HEADER = b64("{\"typ\":\"kb+jwt\"}");
        private static final String KB_SIG = b64("kbsig");
        private static final long NOW_SEC = System.currentTimeMillis() / 1000;

        private static final String SD_JWT_VALID_IAT =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + NOW_SEC + "}") + "." + KB_SIG;

        private static final String SD_JWT_IAT_MISSING =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\"}") + "." + KB_SIG;

        private static final String SD_JWT_IAT_IN_FUTURE =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + (NOW_SEC + 120) + "}") + "." + KB_SIG;

        // iat = 1 year in the past (exceeds 600 s / 10 min default max-age)
        private static final String SD_JWT_IAT_TOO_OLD =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~"
                + KB_HEADER + "." + b64("{\"nonce\":\"" + EXPECTED_NONCE + "\",\"aud\":\"" + EXPECTED_CLIENT_ID + "\",\"iat\":" + (NOW_SEC - 31536000) + "}") + "." + KB_SIG;

        private static final String SD_JWT_NO_KB =
                CRED_HEADER + "." + CRED_PAYLOAD + "." + CRED_SIG + "~";

        private AuthorizationRequestResponseDto buildAuthRequest() {
            DCQLQueryDto dcql = new DCQLQueryDto(
                    List.of(new CredentialQueryDto(
                            "cred1", "dc+sd-jwt",
                            new CredentialMetaDto(List.of("cred1"), null),
                            true, false, null, null)),
                    null);
            return new AuthorizationRequestResponseDto(EXPECTED_CLIENT_ID, dcql, null, EXPECTED_NONCE, "responseUri", false, false);
        }

        private Map<String, List<String>> tokens(String sdJwt) {
            Map<String, List<String>> map = new HashMap<>();
            map.put("cred1", List.of(sdJwt));
            return map;
        }

        @Test
        void shouldReturnNull_whenKbJwtIatIsValid() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), tokens(SD_JWT_VALID_IAT));
            assertNull(result);
        }

        @Test
        void shouldReturnNull_whenSdJwtTokensMapIsEmpty() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), new HashMap<>());
            assertNull(result);
        }

        @Test
        void shouldReturnIatMissingOrInvalid_whenKbJwtIatAbsent() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), tokens(SD_JWT_IAT_MISSING));
            assertEquals(ErrorCode.KB_JWT_IAT_MISSING_OR_INVALID, result);
        }

        @Test
        void shouldReturnIatMissingOrInvalid_whenKbJwtAbsent() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), tokens(SD_JWT_NO_KB));
            assertEquals(ErrorCode.KB_JWT_IAT_MISSING_OR_INVALID, result);
        }

        @Test
        void shouldReturnIatInFuture_whenKbJwtIatIsInFuture() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), tokens(SD_JWT_IAT_IN_FUTURE));
            assertEquals(ErrorCode.KB_JWT_IAT_IN_FUTURE, result);
        }

        @Test
        void shouldReturnIatTooOld_whenKbJwtIatExceedsMaxAge() {
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(buildAuthRequest(), tokens(SD_JWT_IAT_TOO_OLD));
            assertEquals(ErrorCode.KB_JWT_IAT_TOO_OLD, result);
        }

        @Test
        void shouldReturnNull_whenHolderBindingNotRequired() {
            DCQLQueryDto dcql = new DCQLQueryDto(
                    List.of(new CredentialQueryDto(
                            "cred1", "dc+sd-jwt",
                            new CredentialMetaDto(List.of("cred1"), null),
                            false, false, null, null)),
                    null);
            AuthorizationRequestResponseDto authRequest = new AuthorizationRequestResponseDto(
                    EXPECTED_CLIENT_ID, dcql, null, EXPECTED_NONCE, "responseUri", false, false);

            // iat is not checked when holder binding is not required
            ErrorCode result = verifiablePresentationSubmissionService
                    .processSdJwtKbJwtIat(authRequest, tokens(SD_JWT_IAT_MISSING));
            assertNull(result);
        }
    }
}

