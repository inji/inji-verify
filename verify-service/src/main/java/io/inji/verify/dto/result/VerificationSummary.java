package io.inji.verify.dto.result;

import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.VPVerificationStatus;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VerificationSummary {
    VPVerificationStatus proofVerificationStatus;
    String credential;
    VerificationStatus verificationStatus;
    Map<String, CredentialStatusResult> credentialStatus;
}
