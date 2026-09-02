package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@NotNull
@Schema(description = "Represents the data required to create a VP request, including client information and the DCQL query for credential matching.")
public class VPRequestCreateDto {
    @NotBlank(message = "CLIENT_ID_REQUIRED")
    @Schema(description = "Unique identifier for the client making the VP request.")
    String clientId;
    @Schema(description = "Transaction identifier for the VP request.")
    String transactionId;
    @Schema(description = "Optional nonce for the VP request. If omitted, a cryptographically random nonce is generated. When provided, must contain only ASCII URL-safe characters (A-Z, a-z, 0-9, -, ., _, ~) and be at least 16 characters.")
    String nonce;
    @Valid
    @NotNull(message = "DCQL_QUERY_REQUIRED")
    @Schema(description = "DCQL query defining the criteria for credential matching in the VP request.")
    private DCQLQueryDto dcqlQuery;
    @Schema(description = "Indicates whether response code validation is required. Same-device flows set this so the submission response includes redirect_uri. Leave false for cross-device QR.")
    boolean responseCodeValidationRequired;
    @Schema(description = "OpenID4VP response_mode. Omit or use direct_post for wallet HTTP submission; use dc_api for Digital Credentials API.")
    String responseMode;

    /** Backward-compatible constructor used by existing tests and call sites. */
    public VPRequestCreateDto(String clientId, String transactionId, String nonce, DCQLQueryDto dcqlQuery,
                              boolean responseCodeValidationRequired) {
        this(clientId, transactionId, nonce, dcqlQuery, responseCodeValidationRequired, null);
    }
}
