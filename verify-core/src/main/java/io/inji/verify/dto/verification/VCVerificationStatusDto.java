package io.inji.verify.dto.verification;

import io.mosip.vercred.vcverifier.data.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VCVerificationStatusDto {
    VerificationStatus verificationStatus;
}
