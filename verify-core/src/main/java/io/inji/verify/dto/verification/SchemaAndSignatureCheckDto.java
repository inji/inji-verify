package io.inji.verify.dto.verification;

import io.inji.verify.dto.core.ErrorDto;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchemaAndSignatureCheckDto {
    private boolean valid;
    private ErrorDto error;
}
