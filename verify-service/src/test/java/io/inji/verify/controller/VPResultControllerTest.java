package io.inji.verify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.VerificationSessionRequestDto;
import java.util.Base64;
import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.dto.submission.VCSubmissionVerificationStatusDto;
import io.inji.verify.exception.InvalidTransactionIdException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.ResponseCodeException;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPVerificationException;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;

/**
 * Resolving a transactionId to its VP (or, for v1, VC) result — including the "no request found"
 * case — now lives entirely in {@link VerifiablePresentationSubmissionService}; this controller
 * just delegates and translates exceptions to HTTP responses, plus handles the session cookie.
 */
public class VPResultControllerTest {

    private final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService = Mockito.mock(VerifiablePresentationSubmissionService.class);

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        VPResultController vpResultController = new VPResultController(verifiablePresentationSubmissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(vpResultController).build();
    }

    // ── v1: /vp-result/{transactionId} ──────────────────────────────────────

    @Test
    public void testGetVPResult_Success() throws Exception {
        String transactionId = "tx123";
        VPTokenResultDto resultDto = new VPTokenResultDto("tId", VPResultStatus.SUCCESS, new ArrayList<>());

        when(verifiablePresentationSubmissionService.getVPResult(transactionId)).thenReturn(resultDto);

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(resultDto)));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    public void testGetVPResult_NotFound_InvalidTransactionId() throws Exception {
        String transactionId = "tx789";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new InvalidTransactionIdException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    public void testGetVPResult_NotFound_VPSubmissionNotFound() throws Exception {
        String transactionId = "tx101";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new VPSubmissionNotFoundException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    void testGetVPResult_InternalServerError_VPWithoutProofException() throws Exception {
        String transactionId = "tx101";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new VPWithoutProofException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    void testGetVPResult_NotFound_WalletError() throws Exception {
        String transactionId = "tx_id";

        String expectedCode = "Invalid request";
        String expectedMessage = "No requests found for given transaction ID.";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new VPSubmissionWalletError(expectedCode, expectedMessage));

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    void testGetVPResult_BadRequest_TokenMatchingFailedException() throws Exception {
        String transactionId = "tx_token_mismatch";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new TokenMatchingFailedException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    void testGetVPResult_BadRequest_InvalidVpTokenException() throws Exception {
        String transactionId = "tx_invalid_token";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new InvalidVpTokenException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    @Test
    void testGetVPResult_shouldReturnBadRequest_whenResponseCodeException() throws Exception {
        String transactionId = "tx_resp_code";

        ErrorCode errorCode = ErrorCode.INVALID_TRANSACTION_ID; // use any valid enum

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new ResponseCodeException(errorCode));

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    // ── v1: vcSubmission found (fallback now handled inside the service) ──────

    @Test
    public void testGetVPResult_FoundVCSubmission() throws Exception {
        String transactionId = "tx_vc_found";
        VCSubmissionVerificationStatusDto vcDto = mock(VCSubmissionVerificationStatusDto.class);

        when(verifiablePresentationSubmissionService.getVPResult(transactionId)).thenReturn(vcDto);

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ── v1: VPVerificationException ───────────────────────────────────────────

    @Test
    void testGetVPResult_VPVerificationException() throws Exception {
        String transactionId = "tx_vp_verify_fail";

        when(verifiablePresentationSubmissionService.getVPResult(transactionId))
                .thenThrow(new VPVerificationException());

        mockMvc.perform(get("/vp-result/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResult(transactionId);
    }

    // ── v2: /v2/vp-results/{transactionId} ─────────────────────────────────────

    @Test
    void testGetVPResultV2_Success() throws Exception {
        String transactionId = "txn123";
        String requestJson = "{}"; // or add fields if required

        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void testGetVPResultV2_shouldReturnNotFound_WhenInvalidTransactionId() throws Exception {
        String transactionId = "txn404";

        VerificationRequestDto request = new VerificationRequestDto();

        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new InvalidTransactionIdException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_VPSubmissionNotFoundException() throws Exception {
        String transactionId = "txn_v2_not_found";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new VPSubmissionNotFoundException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_VPWithoutProofException() throws Exception {
        String transactionId = "txn_v2_no_proof";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new VPWithoutProofException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_VPSubmissionWalletError() throws Exception {
        String transactionId = "txn_v2_wallet_err";
        String code = "WALLET_ERR";
        String msg = "wallet error";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new VPSubmissionWalletError(code, msg));

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_TokenMatchingFailedException() throws Exception {
        String transactionId = "txn_v2_token_mismatch";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new TokenMatchingFailedException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_InvalidVpTokenException() throws Exception {
        String transactionId = "txn_v2_invalid_token";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new InvalidVpTokenException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_ResponseCodeException() throws Exception {
        String transactionId = "txn_v2_resp_code";
        ErrorCode errorCode = ErrorCode.INVALID_TRANSACTION_ID;
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new ResponseCodeException(errorCode));

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    @Test
    void testGetVPResultV2_VPVerificationException() throws Exception {
        String transactionId = "txn_v2_vp_verify_fail";
        when(verifiablePresentationSubmissionService.getVPResultV2(any(), eq(transactionId)))
                .thenThrow(new VPVerificationException());

        mockMvc.perform(post("/v2/vp-results/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1)).getVPResultV2(any(), eq(transactionId));
    }

    // ── session: /vp-session-results ────────────────────────────────────────

    @Test
    void testGetVPSessionResults_Success() throws Exception {
        String transactionId = "txn123";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());

        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isOk());

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_shouldReturnUnauthorized_whenMissingCookie() throws Exception {

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetVPSessionResults_shouldReturnUnauthorized_whenEmptyCookie() throws Exception {

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetVPSessionResults_shouldReturn_isNotFound_whenInvalidTransactionId() throws Exception {
        String transactionId = "txn404";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());

        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new InvalidTransactionIdException());

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

        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), any()))
                .thenReturn(new VPVerificationResultDto());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(COOKIE_NAME, 0)); // cookie cleared
    }

    @Test
    void testGetVPSessionResults_MalformedCookieException() throws Exception {
        String invalidCookie = "invalid_base64@@@"; // will fail decoding

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, invalidCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verifyNoInteractions(verifiablePresentationSubmissionService);
    }

    @Test
    void testGetVPSessionResults_VPSubmissionNotFoundException() throws Exception {
        String transactionId = "txn_sess_not_found";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new VPSubmissionNotFoundException());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_VPWithoutProofException() throws Exception {
        String transactionId = "txn_sess_no_proof";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new VPWithoutProofException());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_VPSubmissionWalletError() throws Exception {
        String transactionId = "txn_sess_wallet_err";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        String code = "WALLET_ERR";
        String msg = "wallet error";
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new VPSubmissionWalletError(code, msg));

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_TokenMatchingFailedException() throws Exception {
        String transactionId = "txn_sess_token_mismatch";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new TokenMatchingFailedException());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_InvalidVpTokenException() throws Exception {
        String transactionId = "txn_sess_invalid_token";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new InvalidVpTokenException());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_ResponseCodeException() throws Exception {
        String transactionId = "txn_sess_resp_code";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        ErrorCode errorCode = ErrorCode.INVALID_TRANSACTION_ID;
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new ResponseCodeException(errorCode));

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }

    @Test
    void testGetVPSessionResults_VPVerificationException() throws Exception {
        String transactionId = "txn_sess_vp_verify_fail";
        String encodedCookie = Base64.getEncoder().encodeToString(transactionId.getBytes());
        when(verifiablePresentationSubmissionService.getVPSessionResults(any(), eq(transactionId)))
                .thenThrow(new VPVerificationException());

        mockMvc.perform(post("/vp-session-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(COOKIE_NAME, encodedCookie)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(emptyOrNullString())));

        verify(verifiablePresentationSubmissionService, times(1))
                .getVPSessionResults(any(), eq(transactionId));
    }
}
