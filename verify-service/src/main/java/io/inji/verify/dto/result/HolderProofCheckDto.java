package io.inji.verify.dto.result;

import io.inji.verify.dto.core.ErrorDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Results of the holder proof checks for a specific credential, indicating whether the proof provided by the holder is valid and any associated errors if the proof is invalid.")
public class HolderProofCheckDto {
    @Schema(description = "Indicates whether the holder proof is valid.")
    private boolean valid;
    @Schema(description = "Details of any errors encountered during the holder proof validation.")
    private ErrorDto error;
}
