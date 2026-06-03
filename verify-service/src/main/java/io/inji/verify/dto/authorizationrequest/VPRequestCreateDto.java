package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
    @Schema(description = "Nonce for the VP request.")
    String nonce;
    @Valid
    @NotNull(message = "DCQL_QUERY_REQUIRED")
    @Schema(description = "DCQL query defining the criteria for credential matching in the VP request.")
    private DCQLQueryDto dcqlQuery;
    @Schema(description = "Indicates whether to accept VP without holder proof.")
    boolean acceptVPWithoutHolderProof;
    @Schema(description = "Indicates whether response code validation is required.")
    boolean responseCodeValidationRequired;

}
