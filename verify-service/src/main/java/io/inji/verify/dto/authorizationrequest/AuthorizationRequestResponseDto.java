package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.shared.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationRequestResponseDto {

    private final String responseType = Constants.RESPONSE_TYPE;
    private final String responseMode = Constants.RESPONSE_MODE;
    private final long issuedAt = Instant.now().toEpochMilli();
    private final String clientId;
    private final String presentationDefinitionUri;
    private final VPDefinitionResponseDto presentationDefinition;
    private final String nonce;
    private final String responseUri;
    private final boolean acceptVPWithoutHolderProof;
    private final boolean responseCodeValidationRequired;
}
