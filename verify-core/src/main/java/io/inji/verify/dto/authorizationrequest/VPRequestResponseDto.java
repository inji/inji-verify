package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response DTO for a Verifiable Presentation Request, containing details about the request and its status.")
public class VPRequestResponseDto {
    @Schema (description = "Unique identifier for the transaction associated with this VP request.")
    String transactionId;
    @Schema (description = "Unique identifier for the VP request.")
    String requestId;
    @Schema (description = "Details about the authorization request.")
    AuthorizationRequestResponseDto authorizationDetails;
    @Schema (description = "Timestamp indicating when the VP request expires.")
    Long expiresAt;
    @Schema (description = "URI for the VP request.")
    String requestUri;
    @Schema (description = "URI for the SDK to submit the VP. Present when response_mode is dc_api.")
    String responseUri;
}