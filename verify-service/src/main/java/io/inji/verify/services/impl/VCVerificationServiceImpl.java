package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.services.VCVerificationService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static io.inji.verify.utils.Utils.populateAllChecksSuccessful;
import static io.inji.verify.utils.Utils.populateStatusCheckDtoList;
import static io.inji.verify.utils.Utils.populateSchemaAndSignature;
import static io.inji.verify.utils.Utils.populateExpiryCheck;
import static io.inji.verify.utils.Utils.extractClaims;

@Slf4j
@Service
public class VCVerificationServiceImpl implements VCVerificationService {

    @Value("${inji.verify.claims-with-meta-deta}")
    List<String> metaClaims;

    private final CredentialsVerifier credentialsVerifier;
    private final PixelPass pixelPass;

    public VCVerificationServiceImpl(CredentialsVerifier credentialsVerifier, PixelPass pixelPass) {
        this.credentialsVerifier = credentialsVerifier;
        this.pixelPass = pixelPass;
    }

    @Override
    public VCVerificationStatusDto verify(String vc, String contentType) throws CredentialStatusCheckException {
        CredentialFormat format;
        if ("application/vc+sd-jwt".equalsIgnoreCase(contentType) || "application/dc+sd-jwt".equalsIgnoreCase(contentType)) {
            format = CredentialFormat.VC_SD_JWT;
        } else if ("application/vc+cwt".equalsIgnoreCase(contentType)) {
            format = CredentialFormat.CWT_VC;
        } else {
            format = CredentialFormat.LDP_VC;
        }

        log.info("Using credential format based on Content-Type: {}", format);

        List<String> statusPurposeList = new ArrayList<>();
        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
        CredentialVerificationSummary credentialVerificationSummary =
                credentialsVerifier.verifyAndGetCredentialStatus(vc, format, statusPurposeList);
        log.debug("CredentialVerificationResult: {}", credentialVerificationSummary.getVerificationResult());
        return new VCVerificationStatusDto(Utils.getVcVerificationStatus(credentialVerificationSummary));
    }

    @Override
    public VCVerificationResultDto verifyV2(VCVerificationRequestDto request) {
        log.debug("Processing verification request with skipStatusChecks: {}, filters: {}", request.isSkipStatusChecks(), request.getStatusCheckFilters());
        String verifiableCredential = request.getVerifiableCredential();
        CredentialFormat format = Utils.getCredentialFormat(verifiableCredential);
        VerificationResult verificationResult;
        Map<String, CredentialStatusResult> credentialStatus = null;
        ExpiryCheckDto expiryCheck = null;
        List<StatusCheckDto> statusCheck = List.of();
        Map<String, Object> claims = Map.of();

        boolean skipStatusChecks = request.isSkipStatusChecks();
            if (skipStatusChecks) {
                verificationResult = credentialsVerifier.verify(verifiableCredential, format);
            } else {
                List<String> statusCheckFilters = request.getStatusCheckFilters();
                CredentialVerificationSummary credentialVerificationSummary =
                        credentialsVerifier.verifyAndGetCredentialStatus(verifiableCredential, format, statusCheckFilters);
                verificationResult = credentialVerificationSummary.getVerificationResult();
                credentialStatus = credentialVerificationSummary.getCredentialStatus();
            }

        SchemaAndSignatureCheckDto schemaAndSignatureCheck = populateSchemaAndSignature(verificationResult);
        if (schemaAndSignatureCheck.isValid()) {
            expiryCheck = populateExpiryCheck(verificationResult);
            statusCheck = (!skipStatusChecks) ? populateStatusCheckDtoList(credentialStatus) : List.of();
            claims = request.isIncludeClaims() ? extractClaims(verifiableCredential, format, metaClaims, pixelPass) : Map.of();
        }

        boolean allChecksSuccessful = populateAllChecksSuccessful(schemaAndSignatureCheck, expiryCheck, statusCheck, null);

        return new VCVerificationResultDto(allChecksSuccessful, schemaAndSignatureCheck, expiryCheck, statusCheck, claims);
    }
}
