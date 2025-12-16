package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.services.VCVerificationService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VCVerificationServiceImpl implements VCVerificationService {

    private final CredentialsVerifier credentialsVerifier;

    public VCVerificationServiceImpl(CredentialsVerifier credentialsVerifier) {
        this.credentialsVerifier = credentialsVerifier;
    }

    @Override
    public VCVerificationStatusDto verify(String vc, String contentType) throws CredentialStatusCheckException {
        CredentialFormat format;
        if ("application/vc+sd-jwt".equalsIgnoreCase(contentType) || "application/dc+sd-jwt".equalsIgnoreCase(contentType)) {
            format = CredentialFormat.VC_SD_JWT;
        } else {
            format = CredentialFormat.LDP_VC;
        }

        log.info("Using credential format based on Content-Type: {}", format);

        List<String> statusPurposeList = new ArrayList<>();
        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
        CredentialVerificationSummary credentialVerificationSummary =
                credentialsVerifier.verifyAndGetCredentialStatus(vc, format, statusPurposeList);

        return new VCVerificationStatusDto(Utils.getVcVerificationStatus(credentialVerificationSummary));
    }

}
