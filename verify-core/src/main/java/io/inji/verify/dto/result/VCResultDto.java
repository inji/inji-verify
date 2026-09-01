package io.inji.verify.dto.result;


import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Result of the VC verification process, including the VC content and its verification status.")
public class VCResultDto {
    @Schema(description = "The Verifiable Credential content.")
    String vc;
    @Schema(description = "The verification status of the VC.")
    VerificationStatus verificationStatus;
}