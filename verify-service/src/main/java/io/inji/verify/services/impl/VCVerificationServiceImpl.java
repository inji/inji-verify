package io.inji.verify.services.impl;

import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.dto.verification.VCVerificationRequestBodyDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.services.VCVerificationService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        log.info("CredentialVerificationSummary: {}", credentialVerificationSummary);
        return new VCVerificationStatusDto(Utils.getVcVerificationStatus(credentialVerificationSummary));
    }

    public VCVerificationResultDto verifyV2(VCVerificationRequestBodyDto request) {
        log.debug("Processing verification request with skipStatusChecks: {}, filters: {}", request.isSkipStatusChecks(), request.getStatusCheckFilters());
        String verifiableCredential = request.getVerifiableCredential();
        boolean isSdJwt = Utils.isSdJwt(verifiableCredential);
        CredentialFormat format = isSdJwt ? CredentialFormat.VC_SD_JWT : CredentialFormat.LDP_VC;
        VerificationResult verificationResult = null;
        Map<String, CredentialStatusResult> credentialStatus = Map.of();
        ExpiryCheckDto expiryCheck = null;
        List<StatusCheckDto> statusCheck = List.of();

        boolean skipStatusChecks = request.isSkipStatusChecks();
        try {
            if (skipStatusChecks) {
                verificationResult = credentialsVerifier.verify(verifiableCredential, format);
            } else {
                List<String> filters = request.getStatusCheckFilters();
                CredentialVerificationSummary credentialVerificationSummary =
                        credentialsVerifier.verifyAndGetCredentialStatus(verifiableCredential, format, filters);
                verificationResult = credentialVerificationSummary.getVerificationResult();
                credentialStatus = credentialVerificationSummary.getCredentialStatus();
            }
        } catch (Exception e) {
            log.error("Exception during VC verification in verifyV2", e);
            SchemaAndSignatureCheckDto schemaAndSignatureCheck = new SchemaAndSignatureCheckDto(false, new ErrorDto("VERIFICATION_FAILED", e.getMessage()));
            return new VCVerificationResultDto(false, schemaAndSignatureCheck, expiryCheck, statusCheck, null);
        }

        SchemaAndSignatureCheckDto schemaAndSignatureCheck = populateSchemaAndSignature(verificationResult);
        if (schemaAndSignatureCheck.isValid()) {
            expiryCheck = populateExpiryCheck(verificationResult);
            if (!skipStatusChecks) {
                statusCheck = populateStatusCheck(credentialStatus);
            }
        }

        boolean allChecksSuccessful = isAllCheckSuccessful(schemaAndSignatureCheck, expiryCheck, statusCheck);

        return new VCVerificationResultDto(allChecksSuccessful, schemaAndSignatureCheck, expiryCheck, statusCheck, null);
    }

    private List<StatusCheckDto> populateStatusCheck(Map<String, CredentialStatusResult> credentialStatusResult) {
        if (credentialStatusResult == null) return List.of();
        
        return credentialStatusResult.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String purpose = entry.getKey();
                    CredentialStatusResult res = entry.getValue();
                    if (res == null) {
                        return new StatusCheckDto(purpose, false, new ErrorDto("NULL_STATUS_RESULT", "Credential status result was null."));
                    }
                    ErrorDto error = getErrorDto(res);
                    return new StatusCheckDto(purpose, res.isValid(), error);
                })
                .collect(Collectors.toList());
    }

    private static ErrorDto getErrorDto(CredentialStatusResult res) {
        return res.getError() != null
                ? new ErrorDto(res.getError().getErrorCode().toString(), res.getError().getMessage())
                : null;
    }

    private ExpiryCheckDto populateExpiryCheck(VerificationResult verificationResult) {
        VerificationStatus verificationStatus = Util.INSTANCE.getVerificationStatus(verificationResult);
        boolean isValid = verificationStatus != VerificationStatus.EXPIRED;

        return new ExpiryCheckDto(isValid);
    }

    private SchemaAndSignatureCheckDto populateSchemaAndSignature(VerificationResult verificationResult) {
        boolean isValid = verificationResult.getVerificationStatus();
        ErrorDto error = isValid ? null : new ErrorDto(verificationResult.getVerificationErrorCode(), verificationResult.getVerificationMessage());
        
        return new SchemaAndSignatureCheckDto(isValid, error);
    }

    private boolean isAllCheckSuccessful(
            SchemaAndSignatureCheckDto schemaAndSignatureCheckDto,
            ExpiryCheckDto expiryCheckDto,
            List<StatusCheckDto> statusCheckDto) {

        return schemaAndSignatureCheckDto != null
                && schemaAndSignatureCheckDto.isValid()
                && (expiryCheckDto == null || expiryCheckDto.isValid())
                && (statusCheckDto == null
                || statusCheckDto.isEmpty()
                || statusCheckDto.stream().allMatch(c -> c != null && c.isValid()));
    }
}
