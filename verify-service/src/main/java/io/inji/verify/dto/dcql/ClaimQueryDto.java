package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Represents a claim query in DCQL, specifying the path to the claim and optional values for matching.")
public class ClaimQueryDto {

    @Schema(description = "Unique identifier for the claim query, used for reference in credential sets.")
    private String id;

    @NotNull(message = "DCQL_CLAIM_PATH_REQUIRED")
    @NotEmpty(message = "DCQL_CLAIM_PATH_INVALID")
    @Schema(description = "Path to the claim within the credential. Per the DCQL spec, each element must be a non-blank string (object key), null (wildcard), or a non-negative integer (array index).")
    private List<Object> path;

    @Schema(description = "Optional list of values to match against the claim value in the credential. If provided, the claim value in the credential must match one of these values for the query to be satisfied.")
    @Size(min = 1, message = "DCQL_CLAIM_VALUES_INVALID")
    private List<Object> values;
}
