package io.inji.verify.dto.submission;

import io.mosip.vercred.vcverifier.data.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VCSubmissionVerificationStatusDto {
    private String vc;
    private VerificationStatus verificationStatus;
}
