package io.inji.verify.services.impl;

import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.PathNestedDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.repository.VPSubmissionRepository;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.PresentationVerificationResult;
import io.mosip.vercred.vcverifier.data.VCResultWithCredentialStatus;
import io.mosip.vercred.vcverifier.data.VPVerificationStatus;
import io.mosip.vercred.vcverifier.data.PresentationResultWithCredentialStatus;
import io.mosip.vercred.vcverifier.data.VCResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;

public class VerifiablePresentationSubmissionServiceImplTest {

    @Mock
    private VPSubmissionRepository vpSubmissionRepository;

    @Mock
    private PresentationVerifier presentationVerifier;

    @Mock
    private VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService;

    @Mock
    private CredentialsVerifier credentialsVerifier;

    @InjectMocks
    private VerifiablePresentationSubmissionServiceImpl verifiablePresentationSubmissionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        verifiablePresentationSubmissionService = new VerifiablePresentationSubmissionServiceImpl(
            vpSubmissionRepository, credentialsVerifier, presentationVerifier, verifiablePresentationRequestService);
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
    public void testGetVPResult_Success_JSONObject() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_Success_Base64EncodedString() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_Success_JSONArray() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_Success_JSONArrayWithBase64() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_VerificationFailed_InvalidVPStatus() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_VerificationFailed_InvalidVCStatus() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_VerificationFailed_ExpiredVCStatus() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_TokenMatchingFailed_NullVpToken() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_TokenMatchingFailed_NullRequest() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_TokenMatchingFailed_EmptyDescriptorMap() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_TokenMatchingFailed_NullDescriptorMap() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_ExceptionHandling_RuntimeException() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_InvalidVPTokenFormat() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_InvalidItemInVPTokenArray() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_InvalidBase64InArray() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_InvalidBase64String() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_EmptyVpVerificationStatuses() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
        List<String> requestIds = List.of("req123");
        String transactionId = "tx123";

        VPSubmission vpSubmission = new VPSubmission("state123",
                "{\"type\":[\"VerifiablePresentation\"],\"proof\":{\"type\":\"Ed25519Signature2018\"},\"VerifiablePresentation\":[{\"type\":[\"VerifiablePresentation\"]}]}",
            new PresentationSubmissionDto("id", "dId", List.of(
                    new DescriptorMapDto("id", "format", "path", new PathNestedDto(
                            "format", "path")))), null, null);
        
        when(vpSubmissionRepository.findAllById(requestIds)).thenReturn(List.of(vpSubmission));
        when(presentationVerifier.verifyAndGetCredentialStatus(anyString(), anyList())).thenReturn(null);
        when(verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId))
            .thenReturn(new AuthorizationRequestCreateResponse());

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
    }

    @Test
    public void testIsVPTokenMatching_AllValidConditions() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_VerificationFailedException() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
        
        List<VCResult> vcResults = List.of(new VCResult("", VerificationStatus.INVALID));
        when(presentationVerifier.verify(anyString())).thenReturn(
            new PresentationVerificationResult(VPVerificationStatus.VALID, vcResults));

        VPTokenResultDto resultDto = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);

        assertNotNull(resultDto);
        assertEquals(VPResultStatus.FAILED, resultDto.getVpResultStatus());
    }

    @Test 
    public void testGetVPResult_TokenMatchingFailedException() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_MixedVerificationStatuses() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testGetVPResult_AllVerificationStatusTypes() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testProcessJsonVpTokens_SimpleVC() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
    public void testProcessSdJwtVpTokens_Success() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
        String sdJwtToken = "sdjwt-token-value";
        String arrayToken = "[\"" + base64Token + "\",\"" + sdJwtToken + "\"]";
        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();
        verifiablePresentationSubmissionService.extractTokens(arrayToken, jsonVpTokens, sdJwtVpTokens);

        assertEquals(1, jsonVpTokens.size());
        assertEquals(0, sdJwtVpTokens.size());
    }

    @Test
    public void testExtractTokens_InvalidBase64() {
        String arrayToken = "[\"invalid-base64!!!\"]";
        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();
        verifiablePresentationSubmissionService.extractTokens(arrayToken, jsonVpTokens, sdJwtVpTokens);

        assertTrue(jsonVpTokens.isEmpty());
        assertTrue(sdJwtVpTokens.isEmpty());
    }

    @Test
    public void testGetVPResult_Revoked_JSONObject() throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
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
}

