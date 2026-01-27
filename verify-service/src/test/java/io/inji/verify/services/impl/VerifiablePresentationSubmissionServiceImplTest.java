package io.inji.verify.services.impl;

import io.inji.verify.dto.result.CredentialResultsDto;
import io.inji.verify.dto.result.VPTokenDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.PathNestedDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.*;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.*;
import io.inji.verify.repository.VPSubmissionRepository;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        verifiablePresentationSubmissionService = new VerifiablePresentationSubmissionServiceImpl(vpSubmissionRepository, credentialsVerifier, presentationVerifier, verifiablePresentationRequestService, vcVerificationService);
    }

    @Test
    public void testSubmit_Success() {
        VPSubmissionDto vpSubmissionDto = new VPSubmissionDto("vpToken123", 
            new PresentationSubmissionDto("id", "dId", new ArrayList<>()),
                "state123", null, null);

        verifiablePresentationSubmissionService.submit(vpSubmissionDto);

        verify(vpSubmissionRepository, times(1)).save(any(VPSubmission.class));
        verify(verifiablePresentationRequestService, times(1)).invokeVpRequestStatusListener("state123");
    }

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
                null,
                null
        );

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
            new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());
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
                            "format", "path")))), null, null);
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
            new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());
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
                null
        );
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults1))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults2));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());
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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
            new PresentationResultWithCredentialStatus(VPVerificationStatus.INVALID, new ArrayList<>()));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
            new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
            new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(null);

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
    }

    @Test
    public void testGetVPResult_TokenMatchingFailed_EmptyDescriptorMap() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";

        VPSubmission vpSubmission = new VPSubmission("state123", 
            "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}", 
            new PresentationSubmissionDto("id", "dId", new ArrayList<>()), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
    }

    @Test
    public void testGetVPResult_TokenMatchingFailed_NullDescriptorMap() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";

        VPSubmission vpSubmission = new VPSubmission("state123", 
            "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}", 
            new PresentationSubmissionDto("id", "dId", null), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());
        when(presentationVerifier.verify(anyString())).thenThrow(new RuntimeException("Verification error"));

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                                "format", "path")))), null, null);

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
        verify(verifiablePresentationRequestService, times(1)).getLatestAuthorizationRequestFor(transactionId);
    }

    @Test
    public void testGetVPResult_VerificationFailedException() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";

        VPSubmission vpSubmission = new VPSubmission("state123",
                "{\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[]}",
                new PresentationSubmissionDto("id", "dId", List.of(
                        new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                                "format", "path")))), null, null);

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                .thenReturn(new AuthorizationRequestCreateResponse());

        List<VCResultWithCredentialStatus> vcResults = List.of(
                new VCResultWithCredentialStatus("", VerificationStatus.INVALID, new HashMap<>()));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
                .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(null);

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                null
        );
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.INVALID, new ArrayList<>()));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList()))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, successResults))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, expiredResults))
            .thenReturn(new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, invalidResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
    public void testProcessJsonVpTokens_SimpleVC() {
        // Prepare a VPSubmission with a simple VC token
        JSONArray types = new JSONArray();
        types.put("VerifiableCredential");
        JSONObject vc = new JSONObject();
        vc.put("type", types);

        List<JSONObject> jsonVpTokens = new ArrayList<>();
        jsonVpTokens.add(vc);

        VPSubmission vpSubmission = new VPSubmission(
            "state123",
            vc.toString(),
            new PresentationSubmissionDto("id", "dId", List.of(
                new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path"))
            )),
            null,
            null
        );

        String transactionId = "tx123";
        List<String> requestIds = List.of("req123");

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));

        // Mock CredentialVerificationSummary and VerificationResult
        io.mosip.vercred.vcverifier.data.CredentialVerificationSummary mockSummary = mock(io.mosip.vercred.vcverifier.data.CredentialVerificationSummary.class);
        VerificationResult mockResult = mock(VerificationResult.class);
        when(mockResult.getVerificationStatus()).thenReturn(true);
        when(mockSummary.getVerificationResult()).thenReturn(mockResult);

        when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.LDP_VC), anyList()))
            .thenReturn(mockSummary);
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

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
            null
        );

        String transactionId = "tx123";
        List<String> requestIds = List.of("req123");

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));

        // Mock CredentialVerificationSummary and VerificationResult
        io.mosip.vercred.vcverifier.data.CredentialVerificationSummary mockSummary = mock(io.mosip.vercred.vcverifier.data.CredentialVerificationSummary.class);
        VerificationResult mockResult = mock(VerificationResult.class);
        when(mockResult.getVerificationStatus()).thenReturn(true);
        when(mockSummary.getVerificationResult()).thenReturn(mockResult);

        when(credentialsVerifier.verifyAndGetCredentialStatus(anyString(), eq(io.mosip.vercred.vcverifier.constants.CredentialFormat.VC_SD_JWT), anyList()))
            .thenReturn(mockSummary);
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.SUCCESS, resultDto.getVpResultStatus());
        assertFalse(resultDto.getVcResults().isEmpty());
    }

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
                null
        );

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(
                new PresentationResultWithCredentialStatus(VPVerificationStatus.VALID, vcResults));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                .thenReturn(new AuthorizationRequestCreateResponse());
        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
        assertEquals(1, resultDto.getVcResults().size());
        assertEquals(VerificationStatus.REVOKED, resultDto.getVcResults().getFirst().getVerificationStatus());
    }

    @Test
    public void testGetVPResult_V2_success() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto(true, List.of(), false);
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"], \"credentialSubject\": {\"name\":\"John Doe\"}}]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))), null, null);
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(new AuthorizationRequestCreateResponse());

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
        List<CredentialResultsDto> credentialResults =  result.getCredentialResults();

        assertTrue(result.isAllChecksSuccessful());
        assertEquals(1, credentialResults.size());
        assertTrue(credentialResults.getFirst().isAllChecksSuccessful());
        assertTrue(credentialResults.getFirst().getExpiryCheck().isValid());
        assertTrue(credentialResults.getFirst().getSchemaAndSignatureCheck().isValid());
        assertTrue(credentialResults.getFirst().getHolderProofCheck().isValid());
    }

    @Test
    public void testGetVPResult_V2_unsignedPresentation() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[\"testVC\"]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))), null, null);
        AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("test-client-id", "https://example.com/presentation-definition", new VPDefinitionResponseDto("", List.of(), "", "", null, null), "test-nonce", "https://example.com/redirect", true);
        AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse("req123", transactionId, authDetails, System.currentTimeMillis() + 10000);

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
        VCVerificationResultDto vcResultDto = new VCVerificationResultDto();
        vcResultDto.setAllChecksSuccessful(true);
        vcResultDto.setSchemaAndSignatureCheck(new io.inji.verify.dto.verification.SchemaAndSignatureCheckDto(true, null));
        vcResultDto.setExpiryCheck(new io.inji.verify.dto.verification.ExpiryCheckDto(true));
        vcResultDto.setStatusCheck(new ArrayList<>());
        when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class))).thenReturn(vcResultDto);

        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId);

        assertTrue(result.isAllChecksSuccessful());
        assertEquals(1, result.getCredentialResults().size());
        assertNull(result.getCredentialResults().getFirst().getHolderProofCheck());
    }

    @Test
    void testGetVPResult_vpSubmissionWalletErrorV2() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        VPSubmission vpSubmission = new VPSubmission("state123", null, null, "user_cancelled", "User cancelled the operation");
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));

        assertThrows(VPSubmissionWalletError.class, () -> verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId));
    }

    @Test
    void testGetVPResult_V2_tokenMatchingFailed() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"], \"credentialSubject\": {\"name\":\"John Doe\"}}]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", new ArrayList<>()), null, null);
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(new AuthorizationRequestCreateResponse());

        assertThrows(io.inji.verify.exception.TokenMatchingFailedException.class, () -> verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId));
    }

    @Test
    void testGetVPResult_vpWithoutProofV2() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[\"testVC\"]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))), null, null);
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(new AuthorizationRequestCreateResponse());

        assertThrows(VPWithoutProofException.class, () -> verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId));
    }

    @Test
    void testGetVPResult_vpSubmissionNotFoundV2() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(new ArrayList<>());

        assertThrows(VPSubmissionNotFoundException.class, () -> verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId));
    }

    @Test
    void testGetVPResult_V2_success_withClaims() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto(true, List.of(), true);
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"verifiableCredential\":[{\"type\":[\"VerifiableCredential\"], \"credentialSubject\": {\"name\":\"John Doe\"}}]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))), null, null);
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
                .thenReturn(new AuthorizationRequestCreateResponse());

        PresentationVerificationResultV2 presentationVerificationResult = mock(PresentationVerificationResultV2.class);
        VerificationResult proofVerificationResult = mock(VerificationResult.class);
        when(presentationVerificationResult.getProofVerificationResult()).thenReturn(proofVerificationResult);
        when(proofVerificationResult.getVerificationStatus()).thenReturn(true);

        VCResultV2 vcResult = new VCResultV2(vpToken, new VerificationResult(true, "", ""));
        when(presentationVerificationResult.getVcResults()).thenReturn(List.of(vcResult));
        when(presentationVerifier.verifyV2(anyString())).thenReturn(presentationVerificationResult);

        try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
            utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
            utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
            utilsMock.when(() -> Utils.populateAllChecksSuccessful(any(), any(), any(), any())).thenCallRealMethod();

            Map<String, Object> expectedClaims = Map.of("name", "John Doe", "email", "john@example.com");
            utilsMock.when(() -> Utils.extractClaims(eq(vpToken), any(), any()))
                    .thenReturn(expectedClaims);
            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId);
            List<CredentialResultsDto> credentialResults = result.getCredentialResults();
            assertFalse(credentialResults.isEmpty());

            CredentialResultsDto firstVcResult = credentialResults.get(0);
            assertNotNull(firstVcResult.getClaims());
            assertEquals("John Doe", firstVcResult.getClaims().get("name"));
            assertEquals("john@example.com", firstVcResult.getClaims().get("email"));
        }
    }

    @Test
    public void testGetVPResult_V2_unsignedPresentation_withoutClaims() {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";
        VerificationRequestDto verificationRequestDto = new VerificationRequestDto();
        String vpToken = "{\"type\":[\"VerifiablePresentation\"],\"verifiableCredential\":[\"testVC\"]}";
        VPSubmission vpSubmission = new VPSubmission("state123", vpToken, new PresentationSubmissionDto("id", "dId", List.of(new DescriptorMapDto("id", "format", "path", new PathNestedDto("format", "path")))), null, null);
        AuthorizationRequestResponseDto authDetails = new AuthorizationRequestResponseDto("test-client-id", "https://example.com/presentation-definition", new VPDefinitionResponseDto("", List.of(), "", "", null, null), "test-nonce", "https://example.com/redirect", true);
        AuthorizationRequestCreateResponse authResponse = new AuthorizationRequestCreateResponse("req123", transactionId, authDetails, System.currentTimeMillis() + 10000);

        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId)).thenReturn(authResponse);
        VCVerificationResultDto vcResultDto = new VCVerificationResultDto();
        vcResultDto.setAllChecksSuccessful(true);
        vcResultDto.setSchemaAndSignatureCheck(new io.inji.verify.dto.verification.SchemaAndSignatureCheckDto(true, null));
        vcResultDto.setExpiryCheck(new io.inji.verify.dto.verification.ExpiryCheckDto(true));
        vcResultDto.setStatusCheck(new ArrayList<>());
        when(vcVerificationService.verifyV2(any(VCVerificationRequestDto.class))).thenReturn(vcResultDto);

        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(verificationRequestDto, requestIds, transactionId);

        assertTrue(result.isAllChecksSuccessful());
        assertEquals(1, result.getCredentialResults().size());
        assertNull(result.getCredentialResults().getFirst().getHolderProofCheck());
        assertNull(result.getCredentialResults().getFirst().getClaims());
    }
}

