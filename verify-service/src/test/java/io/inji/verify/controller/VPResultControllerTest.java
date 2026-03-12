package io.inji.verify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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
}