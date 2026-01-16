package io.inji.verify.dto.result;

import io.inji.verify.dto.verification.VCVerificationResultDto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class CredentialResultsDto extends VCVerificationResultDto {
    private String verifiableCredential;
    private HolderProofCheckDto holderProofCheck;
}
