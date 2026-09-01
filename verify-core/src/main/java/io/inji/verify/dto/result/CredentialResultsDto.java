package io.inji.verify.dto.result;

import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Detailed results for an individual credential included in the VP, including the original credential and the outcome of holder proof checks.")
public class CredentialResultsDto extends VCVerificationResultDto {
    @Schema(description = "The original verifiable credential as a JSON string, included for reference in the verification results.")
    private String verifiableCredential;
    @Schema(description = "Results of the holder proof checks, indicating whether the proof provided by the holder is valid and matches the expected criteria.")
    private HolderProofCheckDto holderProofCheck;
}
