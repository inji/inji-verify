package io.inji.verify.services.impl;

import io.inji.verify.exception.CredentialStatusCheckException;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.inji.verify.dto.submission.VCSubmissionDto;
import io.inji.verify.dto.submission.VCSubmissionResponseDto;
import io.inji.verify.dto.submission.VCSubmissionVerificationStatusDto;
import io.inji.verify.models.VCSubmission;
import io.inji.verify.repository.VCSubmissionRepository;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static io.inji.verify.utils.Utils.isSdJwt;

@Slf4j
@Service
public class VCSubmissionServiceImpl implements VCSubmissionService {

    final VCSubmissionRepository vcSubmissionRepository;
    final CredentialsVerifier credentialsVerifier;

    public VCSubmissionServiceImpl(VCSubmissionRepository vcSubmissionRepository, CredentialsVerifier credentialsVerifier) {
        this.vcSubmissionRepository = vcSubmissionRepository;
        this.credentialsVerifier = credentialsVerifier;
    }

    @Override
    public VCSubmissionResponseDto submitVC(VCSubmissionDto vcSubmitted) {
        String transactionId = vcSubmitted.getTransactionId() != null ? vcSubmitted.getTransactionId() : Utils.generateID(Constants.TRANSACTION_ID_PREFIX);
        String vc = vcSubmitted.getVc();
        VCSubmission vcSubmission = new VCSubmission(transactionId, vc);
        vcSubmissionRepository.save(vcSubmission);
        return new VCSubmissionResponseDto(vcSubmission.getTransactionId());
    }

    @Override
    public VCSubmissionVerificationStatusDto getVcWithVerification(String transactionId) throws CredentialStatusCheckException {
        Optional<VCSubmission> vcSubmission = vcSubmissionRepository.findById(transactionId);
        if (vcSubmission.isPresent()) {
            String vc = vcSubmission.get().getVc();
            boolean isSdJwtVc = isSdJwt(vc);
            CredentialFormat credentialFormat = isSdJwtVc ? CredentialFormat.VC_SD_JWT : CredentialFormat.LDP_VC;
            List<String> statusPurposeList = new ArrayList<>();
            statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
            CredentialVerificationSummary credentialVerificationSummary = credentialsVerifier.verifyAndGetCredentialStatus(vc, credentialFormat, statusPurposeList);
            VerificationStatus vcVerificationStatus = Utils.getVcVerificationStatus(credentialVerificationSummary);

            return new VCSubmissionVerificationStatusDto(vc, vcVerificationStatus);
        } else {
            return null;
        }
    }
}
