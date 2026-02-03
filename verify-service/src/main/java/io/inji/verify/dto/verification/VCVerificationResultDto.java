package io.inji.verify.dto.verification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VCVerificationResultDto {
    private boolean allChecksSuccessful;
    private SchemaAndSignatureCheckDto schemaAndSignatureCheck;
    private ExpiryCheckDto expiryCheck;
    private List<StatusCheckDto> statusCheck;
    private Map<String, Object> claims;
}
