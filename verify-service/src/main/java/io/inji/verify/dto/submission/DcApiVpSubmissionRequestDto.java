package io.inji.verify.dto.submission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JSON body for {@code POST /vp-submission/dc-api}.
 * Correlates via {@code requestId} (same role as {@code state} on direct-post).
 */
@Getter
@Setter
@NoArgsConstructor
public class DcApiVpSubmissionRequestDto {

    @NotBlank(message = "INVALID_REQUEST_ID_MISSING")
    private String requestId;

    @JsonProperty("vp_token")
    @JsonDeserialize(using = FailOnDuplicateKeyJsonNodeDeserializer.class)
    private JsonNode vpToken;

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
}
