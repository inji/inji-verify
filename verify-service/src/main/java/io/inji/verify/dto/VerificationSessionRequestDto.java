package io.inji.verify.dto;

import io.inji.verify.dto.result.VerificationRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Request parameters for VP verification session, extending basic verification options with additional response code for session management.")
public class VerificationSessionRequestDto extends VerificationRequestDto {
    @Schema(description = "Optional response code to be included in the verification session response.")
    private String responseCode;

    public VerificationSessionRequestDto(boolean skipStatusChecks, List<String> statusCheckFilters, boolean includeClaims, String responseCode) {
        super(skipStatusChecks, statusCheckFilters, includeClaims);
        this.responseCode = responseCode;
    }
}
