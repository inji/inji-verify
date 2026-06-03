package io.inji.verify.dto.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Result of VP verification process, including overall status and details for each credential.")
public class VPVerificationResultDto {
    @Schema(description = "Unique identifier for the verification transaction.")
    private String transactionId;
    @Schema(description = "Indicates whether all verification checks were successful.")
    private boolean allChecksSuccessful;
    @Schema(description = "List of results for each credential included in the VP, detailing the outcome of each verification check.")
    private List<CredentialResultsDto> credentialResults;
}
