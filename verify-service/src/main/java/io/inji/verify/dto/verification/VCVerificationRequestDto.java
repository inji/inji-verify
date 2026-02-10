package io.inji.verify.dto.verification;

import io.inji.verify.dto.result.VerificationRequestDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class VCVerificationRequestDto extends VerificationRequestDto {
    @NotNull
    private String verifiableCredential;
}
