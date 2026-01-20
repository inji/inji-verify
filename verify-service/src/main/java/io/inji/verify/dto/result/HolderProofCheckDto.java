package io.inji.verify.dto.result;

import io.inji.verify.dto.core.ErrorDto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class HolderProofCheckDto {
    private boolean valid;
    private ErrorDto error;
}
