package io.inji.verify.dto.core;

import io.inji.verify.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Schema(description = "Standardized error response structure for conveying error information in API responses, including an error code and a human-readable error message.")
public class ErrorDto {
    @Schema(description = "The error code associated with the error.")
    private String errorCode;
    @Schema(description = "A human-readable error message providing additional context about the error.")
    private String errorMessage;

    public ErrorDto(ErrorCode errorCodeEnum) {
        this.errorCode = errorCodeEnum.getErrorCode();
        this.errorMessage = errorCodeEnum.getErrorMessage();
    }
}
