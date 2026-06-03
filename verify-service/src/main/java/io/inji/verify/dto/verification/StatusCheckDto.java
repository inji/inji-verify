package io.inji.verify.dto.verification;

import io.inji.verify.dto.core.ErrorDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response structure for status check endpoint, indicating the purpose of the check, whether it is valid, and any associated errors if the check is invalid.")
public class StatusCheckDto {
    @Schema(description = "The purpose of the status check, indicating what aspect of the credential's status is being evaluated (e.g., revocation status, suspension status).")
    private String purpose;
    @Schema(description = "Indicates whether the status check is valid, meaning that the credential's status meets the expected criteria for the specified purpose.")
    private boolean valid;
    @Schema(description = "Details of any errors encountered during the status check, providing information on why the check may have failed if it is not valid.")
    private ErrorDto error;
}
