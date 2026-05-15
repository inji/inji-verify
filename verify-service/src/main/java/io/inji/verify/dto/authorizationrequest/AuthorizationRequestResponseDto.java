package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.inji.verify.shared.Constants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

/**
 * Authorization request details aligned with the verifier API / SDK shape (camelCase JSON),
 * matching {@code VPRequestBody} for the overlapping fields. JWT issuance still maps
 * {@link #getDcqlQuery()} to the {@code dcql_query} claim per OpenID4VP.
 */
@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizationRequestResponseDto {

    private final String responseType = Constants.RESPONSE_TYPE;
    private final String responseMode = Constants.RESPONSE_MODE;
    private final long issuedAt = Instant.now().toEpochMilli();
    private final String clientId;
    private final JsonNode dcqlQuery;
    private final String nonce;
    private final String responseUri;
    private final boolean acceptVPWithoutHolderProof;
    private final boolean responseCodeValidationRequired;
}
