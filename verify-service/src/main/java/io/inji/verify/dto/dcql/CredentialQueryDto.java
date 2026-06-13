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
     * Supported formats: dc+sd-jwt, vc+sd-jwt, ldp_vc.
     */
    @NotBlank(message = "DCQL_CREDENTIAL_FORMAT_REQUIRED")
    @Schema(description = "Format of the credential being queried. Supported values: 'dc+sd-jwt' (SD-JWT VC per draft-ietf-oauth-sd-jwt-vc-10), 'vc+sd-jwt' (legacy SD-JWT VC format), and 'ldp_vc' (JSON-LD Verifiable Credential).")
    private String format;

    @Valid
    @NotNull(message = "DCQL_META_REQUIRED")
    @Schema(description = "Metadata for the credential being queried, used for matching against the credential's metadata.")
    private CredentialMetaDto meta;

    @Schema(description = "Indicates whether cryptographic holder binding is required for the credential, which means that the credential must be cryptographically bound to the holder's proof of possession, such as a signature or proof of key ownership, to ensure that only the rightful holder can present the credential. Defaults to true per the spec.")
    private boolean require_cryptographic_holder_binding = true;

    @Schema(description = "Indicates whether the Wallet is allowed to return multiple credentials matching this query. Defaults to false, meaning only one credential per query is expected.")
    private boolean multiple = false;

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
