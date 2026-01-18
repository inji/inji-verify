package io.inji.verify.utils;

import io.inji.verify.dto.core.CredentialStatusErrorDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.HolderProofCheckDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.shared.Constants;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.exception.StatusCheckException;
import io.mosip.vercred.vcverifier.utils.Base64Decoder;
import io.mosip.vercred.vcverifier.utils.Util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public final class Utils {

    private static final Set<String> VALID_SD_JWT_TYPES = Set.of("vc+sd-jwt", "dc+sd-jwt");

    private Utils() {
    }

    public static String generateID(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }

    public static boolean isSdJwt(String vpToken) {
        String[] jwtParts = vpToken.split("~")[0].split("\\.");
        if (jwtParts.length != 3) {
            return false;
        }
        String header = decodeBase64Json(jwtParts[0]);
        String typ = new JSONObject(header).optString("typ", "");
        return VALID_SD_JWT_TYPES.contains(typ);
    }

    private static String decodeBase64Json(String encoded)  {
        byte[] decodedBytes = new Base64Decoder().decodeFromBase64Url(encoded);
        return new String(decodedBytes);
    }

    public static VerificationStatus getVcVerificationStatus(CredentialVerificationSummary credentialVerificationSummary) throws CredentialStatusCheckException {
        log.debug("Credential Verification Summary: {}", credentialVerificationSummary);
        VerificationResult verificationResult = credentialVerificationSummary.getVerificationResult();
        VerificationStatus verificationStatus = Util.INSTANCE.getVerificationStatus(verificationResult);
        boolean isRevoked = checkIfVCIsRevoked(credentialVerificationSummary.getCredentialStatus());
        if (isRevoked) return VerificationStatus.REVOKED;

        log.debug("VC verification status is {}", verificationStatus );
        return verificationStatus;
    }

    public static boolean checkIfVCIsRevoked(Map<String, CredentialStatusResult> credentialStatusResults) throws CredentialStatusCheckException {
        if (!credentialStatusResults.isEmpty()) {
            CredentialStatusResult credentialStatusResult = credentialStatusResults.get(Constants.STATUS_PURPOSE_REVOKED);
            if (credentialStatusResult != null) {
                StatusCheckException error = credentialStatusResult.getError();
                boolean isStatusValid = credentialStatusResult.isValid();
                if (error == null) {
                    // VC is Revoked if status is Not Valid
                    return !isStatusValid;
                } else {
                    log.error("Failed to get Credential Status due to: {} {}", error.getErrorCode(), error.getErrorMessage());
                    throw new CredentialStatusCheckException(error.getErrorCode(), error.getErrorMessage());
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public static VerificationStatus applyRevocationStatus(VerificationStatus originalStatus, Map<String, CredentialStatusResult> credentialStatus) throws CredentialStatusCheckException {
        boolean isRevoked = checkIfVCIsRevoked(credentialStatus);
        return isRevoked ? VerificationStatus.REVOKED : originalStatus;
    }

    public static ResponseEntity<Object> getResponseEntityForCredentialStatusException(CredentialStatusCheckException ex, HttpServletRequest request) {
        String errorMessage = ex.getErrorCode() + " - " + ex.getErrorDescription();
        CredentialStatusErrorDto credentialStatusErrorDto =
                new CredentialStatusErrorDto(Instant.now().toString(), 500, request.getRequestURI(), errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(credentialStatusErrorDto);
    }

    public static List<StatusCheckDto> populateStatusCheck(Map<String, CredentialStatusResult> credentialStatusResult) {
        if (credentialStatusResult == null) return List.of();

        return credentialStatusResult.entrySet().stream()
                .map(entry -> {
                    String purpose = entry.getKey();
                    CredentialStatusResult res = entry.getValue();
                    if (res == null) {
                        return new StatusCheckDto(purpose, false, new ErrorDto("NULL_STATUS_RESULT", "Credential status result was null."));
                    }
                    ErrorDto error = populateErrorDto(res);
                    return new StatusCheckDto(purpose, res.isValid(), error);
                })
                .collect(Collectors.toList());
    }

    private static ErrorDto populateErrorDto(CredentialStatusResult res) {
        return res.getError() != null
                ? new ErrorDto(res.getError().getErrorCode().toString(), res.getError().getErrorMessage())
                : null;
    }

    public static boolean populateAllChecksSuccessful(
            SchemaAndSignatureCheckDto schemaAndSignatureCheckDto,
            ExpiryCheckDto expiryCheckDto,
            List<StatusCheckDto> statusCheckDto,
            HolderProofCheckDto holderProofCheckDto) {

        return schemaAndSignatureCheckDto != null
                && schemaAndSignatureCheckDto.isValid()
                && (expiryCheckDto == null || expiryCheckDto.isValid())
                && (statusCheckDto == null
                || statusCheckDto.isEmpty()
                || statusCheckDto.stream().allMatch(c -> c != null && c.isValid()))
                && (holderProofCheckDto == null || holderProofCheckDto.isValid());
    }
}
