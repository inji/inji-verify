package io.inji.verify.services;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import io.inji.verify.dto.result.DcqlVPTokenDto;
import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;

import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.ResponseCodeException;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import jakarta.validation.Valid;

public interface VerifiablePresentationSubmissionService {
    
    AuthorizationRequestCreateResponse getAuthRequest(String state);

    DcqlVPTokenDto extractDcqlVpTokens(String vpTokenString) throws InvalidVpTokenException;

    boolean isClientIdValid(AuthorizationRequestResponseDto authRequest, Map<String, JSONObject> ldpVpTokens);
    
    boolean isNonceValid(AuthorizationRequestResponseDto authRequest, Map<String, JSONObject> ldpVpTokens);
    
    String generateResponseCode(AuthorizationRequestResponseDto authRequest);
    
    Timestamp generateResponseCodeExpiry();
    
    String buildRedirectUri(String responseCode);
    
    void submitVpToken(AuthorizationRequestResponseDto authRequest, String vpToken, String state, String error, String errorDescription,
   		 String responseCode, Timestamp responseCodeExpiryAt) throws VPAlreadySubmittedException;

    VPTokenResultDto getVPResult(List<String> requestId, String transactionId) throws VPSubmissionWalletError, InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException;

    VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, List<String> requestIds, String transactionId);

    @Transactional
    VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, List<String> requestIds, String transactionId);

}
