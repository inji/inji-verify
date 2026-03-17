package io.inji.verify.dto;

import io.inji.verify.dto.result.VerificationRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VerificationSessionRequestDto extends VerificationRequestDto {
    private String responseCode;
}
