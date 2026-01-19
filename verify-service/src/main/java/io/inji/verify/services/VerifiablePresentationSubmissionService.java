package io.inji.verify.services;

import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import jakarta.validation.Valid;
import java.util.List;

public interface VerifiablePresentationSubmissionService {
    void submit(VPSubmissionDto vpSubmissionDto);

    VPTokenResultDto getVPResult(List<String> requestId, String transactionId);

    VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, List<String> requestIds, String transactionId);
}
