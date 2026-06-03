package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Represents a DCQL query, specifying the credentials and credential sets for matching against credentials in the wallet.")
public class DCQLQueryDto {

    @Valid
    @NotNull(message = "DCQL_CREDENTIALS_REQUIRED")
    @NotEmpty(message = "DCQL_CREDENTIALS_INVALID")
    @Schema(description = "List of credentials to be matched against the wallet.")
    private List<CredentialQueryDto> credentials;

    @Valid
    @JsonProperty("credential_sets")
    @SerializedName("credential_sets")
    @Size(min = 1, message = "DCQL_CREDENTIAL_SETS_INVALID")
    @Schema(description = "List of credential sets for matching against sets of credentials in the wallet.")
    private List<CredentialSetQueryDto> credentialSets;
}
