package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a credential query in DCQL, specifying the credential id, format, metadata, claims, and claim sets for matching against credentials in the wallet.")
public class CredentialQueryDto {

    @NotBlank(message = "DCQL_CREDENTIAL_ID_REQUIRED")
    @Schema(description = "Unique identifier for the credential query, used for reference in credential sets.")
    private String id;

    /**
     * vc+sd-jwt, dc+sd-jwt etc.
     */
    @NotBlank(message = "DCQL_CREDENTIAL_FORMAT_REQUIRED")
    @Schema(description = "Format of the credential being queried, such as 'vc+sd-jwt' for SD-JWT verifiable credentials or 'dc+sd-jwt' for data credentials, used to determine how to process and match the credential against the query criteria.")
    private String format;

    @Valid
    @NotNull(message = "DCQL_META_REQUIRED")
    @Schema(description = "Metadata for the credential being queried, used for matching against the credential's metadata.")
    private CredentialMetaDto meta;

    @Valid
    @Schema(description = "List of claims to be matched against the credential.")
    private List<ClaimQueryDto> claims;

    /**
     * References claim ids.
     */
    @JsonProperty("claim_sets")
    @SerializedName("claim_sets")
    @Size(min = 1, message = "DCQL_EMPTY_CLAIM_SET")
    @Schema(description = "List of claim sets, where each inner list contains claim ids for matching against the credential.")
    private List<
            @NotEmpty(message = "DCQL_EMPTY_CLAIM_SET")
                    List<
                            @NotBlank(message = "DCQL_EMPTY_CLAIM_SET")
                                    String
                            >
            > claimSets;
}
