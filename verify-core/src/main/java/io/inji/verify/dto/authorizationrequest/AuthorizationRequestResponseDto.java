package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.shared.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * DTO representing the response to an authorization request, containing details about the request and its parameters.
 */
@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class AuthorizationRequestResponseDto {

    private final String responseType = Constants.RESPONSE_TYPE;
    private final String responseMode = Constants.RESPONSE_MODE;
    private final long issuedAt = Instant.now().toEpochMilli();
    private final String clientId;
    private final DCQLQueryDto dcqlQuery;
    //This is for backward compatibility, as the presentation definition for VP results.
    private final VPDefinitionResponseDto presentationDefinition;
    private final String nonce;
    private final String responseUri;
    // This is deprecated and for backward compatibility to support older VP submissions without holder proof.
    @Getter(onMethod_ = @JsonIgnore)
    private final boolean acceptVPWithoutHolderProof;
    private final boolean responseCodeValidationRequired;
}