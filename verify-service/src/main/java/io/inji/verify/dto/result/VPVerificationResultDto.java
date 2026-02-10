package io.inji.verify.dto.result;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VPVerificationResultDto {
    private String transactionId;
    private boolean allChecksSuccessful;
    private List<CredentialResultsDto> credentialResults;
}
