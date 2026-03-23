package io.inji.verify.services;

import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.exception.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VerifiablePresentationSubmissionService {
    ResponseEntity<?> submit(String vpToken, String presentationSubmission, String state, String error, String errorDescription);

    VPTokenResultDto getVPResult(List<String> requestId, String transactionId) throws VPSubmissionWalletError, InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException;

    VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, List<String> requestIds, String transactionId);

    @Transactional
    VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, List<String> requestIds, String transactionId);
}
