package io.inji.verify.services;

import io.inji.verify.dto.submission.VCSubmissionDto;
import io.inji.verify.dto.submission.VCSubmissionResponseDto;
import io.inji.verify.dto.submission.VCSubmissionVerificationStatusDto;
import io.inji.verify.exception.CredentialStatusCheckException;

public interface VCSubmissionService {
    VCSubmissionResponseDto submitVC(VCSubmissionDto vcSubmissionDto);

    VCSubmissionVerificationStatusDto getVcWithVerification(String transactionId) throws CredentialStatusCheckException;
}
