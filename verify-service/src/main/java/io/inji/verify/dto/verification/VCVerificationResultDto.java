package io.inji.verify.dto.verification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive result of the verification process for a verifiable credential, including the overall success status, detailed results of schema and signature checks, expiry checks, status checks, and extracted claims from the credential.")
public class VCVerificationResultDto {
    @Schema(description = "Indicates whether all verification checks were successful.")
    private boolean allChecksSuccessful;
    @Schema(description = "Results of the schema and signature checks, indicating whether the credential's structure and cryptographic signature are valid.")
    private SchemaAndSignatureCheckDto schemaAndSignatureCheck;
    @Schema(description = "Result of the expiry check, indicating whether the credential is still valid.")
    private ExpiryCheckDto expiryCheck;
    @Schema(description = "List of results from the status checks, indicating the current status of the credential.")
    private List<StatusCheckDto> statusCheck;
    @Schema(description = "Map of extracted claims from the credential.")
    private Map<String, Object> claims;
}
