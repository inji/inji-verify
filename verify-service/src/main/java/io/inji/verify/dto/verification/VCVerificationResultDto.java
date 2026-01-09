package io.inji.verify.dto.verification;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.json.JSONObject;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VCVerificationResultDto {
    private boolean allChecksSuccessful;
    private SchemaAndSignatureCheckDto schemaAndSignatureCheck;
    private ExpiryCheckDto expiryCheck;
    private List<StatusCheckDto> statusCheck;
    private JSONObject claims;
}
