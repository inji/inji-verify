package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents the metadata of a credential in DCQL, including SD-JWT VC vct values and W3C VC expanded type values.")
public class CredentialMetaDto {

    /**
     * SD-JWT VC vct values
     */
    @JsonProperty("vct_values")
    @SerializedName("vct_values")
    @Size(min = 1, message = "DCQL_META_INVALID")
    @Schema(description = "List of vct values for SD-JWT verifiable credentials, used for matching against the credential's vct claim.")
    private List<@NotBlank(message = "DCQL_META_INVALID") String> vctValues;

    /**
     * W3C VC (JSON-LD): expanded type values.
     */
    @JsonProperty("type_values")
    @SerializedName("type_values")
    @Size(min = 1, message = "DCQL_META_INVALID")
    @Schema(description = "List of type values for W3C verifiable credentials, used for matching against the credential's type claim.")
    private List<@NotBlank(message = "DCQL_META_INVALID") String> typeValues;
}
