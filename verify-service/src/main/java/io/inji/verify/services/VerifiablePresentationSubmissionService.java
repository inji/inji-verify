package io.inji.verify.services;

import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;

import java.util.List;

public interface VerifiablePresentationSubmissionService {
    void submit(VPSubmissionDto vpSubmissionDto);

    VPTokenResultDto getVPResult(List<String> requestId, String transactionId) throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException;
}
