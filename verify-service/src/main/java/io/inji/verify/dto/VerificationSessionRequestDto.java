package io.inji.verify.dto;

import io.inji.verify.dto.result.VerificationRequestDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VerificationSessionRequestDto extends VerificationRequestDto {
    @NotEmpty
    @NotNull
    private String responseCode;

    public VerificationSessionRequestDto(boolean skipStatusChecks, List<String> statusCheckFilters, boolean includeClaims, String responseCode) {
        super(skipStatusChecks, statusCheckFilters, includeClaims);
        this.responseCode = responseCode;
    }
}
