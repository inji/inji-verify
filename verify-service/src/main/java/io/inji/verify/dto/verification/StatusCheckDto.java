package io.inji.verify.dto.verification;

import io.inji.verify.dto.core.ErrorDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusCheckDto {
    private String purpose;
    private boolean valid;
    private ErrorDto error;
}
