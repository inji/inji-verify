package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Represents a credential set query in DCQL, specifying the options for matching against sets of credentials in the wallet.")
public class CredentialSetQueryDto {

    /**
     * Each inner list contains credential ids.
     */
    @NotNull(message = "DCQL_CREDENTIAL_SETS_REQUIRED")
    @NotEmpty(message = "DCQL_CREDENTIAL_SETS_INVALID")
    @Schema(description = "List of options for matching against sets of credentials in the wallet.")
    private List<
            @NotEmpty(message = "DCQL_CREDENTIAL_SETS_INVALID")
                    List<
                            @NotBlank(message = "DCQL_CREDENTIAL_SETS_INVALID")
                                    String
                            >
            > options;

    @Schema(description = "Indicates whether the credential set is required for matching.")
    private boolean required = true;
}
