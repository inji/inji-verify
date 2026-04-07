package io.inji.verify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.VerificationSessionRequestDto;
import java.util.Base64;
import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.*;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class VPResultControllerTest {

    private final VerifiablePresentationRequestService verifiablePresentationRequestService = Mockito.mock(VerifiablePresentationRequestService.class);

    private final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService = Mockito.mock(VerifiablePresentationSubmissionService.class);

    private final VCSubmissionService vcSubmissionService = Mockito.mock(VCSubmissionService.class);

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        VPResultController vpResultController = new VPResultController(verifiablePresentationRequestService, vcSubmissionService, verifiablePresentationSubmissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(vpResultController).build();
    }

    @Test
    public void testGetVPResult_Success() throws Exception {
        String transactionId = "tx123";
        List<String> requestIds = new ArrayList<>();
        requestIds.add("req456");

        VPTokenResultDto resultDto = new VPTokenResultDto("tId", VPResultStatus.SUCCESS, new ArrayList<>());

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId)).thenReturn(requestIds);
        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId)).thenReturn(resultDto);

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(resultDto)));

        verify(verifiablePresentationRequestService, times(1)).getLatestRequestIdFor(transactionId);
        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    public void testGetVPResult_NotFound_RequestIdsEmpty() throws Exception {
        String transactionId = "tx789";
        List<String> requestIds = new ArrayList<>();

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId)).thenReturn(requestIds);

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID))));

        verify(verifiablePresentationRequestService, times(1)).getLatestRequestIdFor(transactionId);
        verify(verifiablePresentationSubmissionService, never()).getVPResult(any(), any());
    }

    @Test
    public void testGetVPResult_NotFound_VPSubmissionNotFound() throws Exception {
        String transactionId = "tx101";
        List<String> requestIds = new ArrayList<>();
        requestIds.add("req112");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId)).thenReturn(requestIds);
        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId)).thenThrow(new VPSubmissionNotFoundException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.NO_VP_SUBMISSION))));

        verify(verifiablePresentationRequestService, times(1)).getLatestRequestIdFor(transactionId);
        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPResult_InternalServerError_VPWithoutProofException() throws Exception {
        String transactionId = "tx101";
        List<String> requestIds = new ArrayList<>();
        requestIds.add("req112");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId)).thenReturn(requestIds);
        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId)).thenThrow(new VPWithoutProofException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.VP_WITHOUT_PROOF))));

        verify(verifiablePresentationRequestService, times(1)).getLatestRequestIdFor(transactionId);
        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPResult_NotFound_WalletError() throws Exception {
        String transactionId = "tx_id";
        List<String> requestIds = List.of("req001");

        String expectedCode = "Invalid request" ;
        String expectedMessage = "No requests found for given transaction ID.";
        ErrorDto errorDto = new ErrorDto(expectedCode, expectedMessage);
        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(requestIds);

        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId))
                .thenThrow(new VPSubmissionWalletError(expectedCode, expectedMessage));

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(errorDto)));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPResult_BadRequest_TokenMatchingFailedException() throws Exception {
        String transactionId = "tx_token_mismatch";
        List<String> requestIds = List.of("req_888");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(requestIds);
        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId))
                .thenThrow(new TokenMatchingFailedException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.TOKEN_MATCHING_FAILED))));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPResult_BadRequest_InvalidVpTokenException() throws Exception {
        String transactionId = "tx_invalid_token";
        List<String> requestIds = List.of("req_999");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId)).thenReturn(requestIds);
        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId))
                .thenThrow(new InvalidVpTokenException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.INVALID_VP_TOKEN))));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPResultV2_Success() throws Exception {
        String transactionId = "txn123";

        String requestJson = "{}"; // or add fields if required

        List<String> requestIds = List.of("req1");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(requestIds);

        when(verifiablePresentationSubmissionService.getVPResultV2(
                any(),
                eq(requestIds),
                eq(transactionId)
        )).thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void testGetVPResultV2_NotFound_WhenRequestIdsEmpty() throws Exception {
        String transactionId = "txn404";

        VerificationRequestDto request = new VerificationRequestDto();

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(List.of());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        objectMapper.writeValueAsString(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID))
                ));

        verify(verifiablePresentationSubmissionService, never())
                .getVPResultV2(any(), any(), any());
    }

    @Test
    void testGetVPSessionResults_Success() throws Exception {
        String transactionId = "txn123";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());

        VerificationSessionRequestDto request = new VerificationSessionRequestDto();
        List<String> requestIds = List.of("req1");

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(requestIds);

        when(verifiablePresentationSubmissionService.getVPSessionResults(
                any(),
                eq(requestIds),
                eq(transactionId)
        )).thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isOk());

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(requestIds), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_MissingCookie() throws Exception {

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetVPSessionResults_EmptyCookie() throws Exception {

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetVPSessionResults_RequestIdsEmpty() throws Exception {
        String transactionId = "txn404";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(List.of());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetVPSessionResults_CookieCleanup() throws Exception {
        String transactionId = "txn123";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(List.of("req1"));

        when(verifiablePresentationSubmissionService.getVPSessionResults(
                any(), any(), any()))
                .thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(COOKIE_NAME, 0)); // cookie cleared
    }

    @Test
    void testGetVPResult_ResponseCodeException() throws Exception {
        String transactionId = "tx_resp_code";
        List<String> requestIds = List.of("req123");

        ErrorCode errorCode = ErrorCode.INVALID_TRANSACTION_ID; // use any valid enum

        when(verifiablePresentationRequestService.getLatestRequestIdFor(transactionId))
                .thenReturn(requestIds);

        when(verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId))
                .thenThrow(new ResponseCodeException(errorCode));

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        objectMapper.writeValueAsString(
                                new ErrorDto(errorCode.name(), errorCode.getErrorMessage())
                        )
                ));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPResult(requestIds, transactionId);
    }

    @Test
    void testGetVPSessionResults_MalformedCookieException() throws Exception {
        String invalidCookie = "invalid_base64@@@"; // will fail decoding

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, invalidCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        objectMapper.writeValueAsString(
                                new ErrorDto(ErrorCode.MALFORMED_COOKIE)
                        )
                ));
    }
}