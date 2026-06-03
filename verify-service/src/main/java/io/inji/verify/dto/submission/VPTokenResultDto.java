package io.inji.verify.dto.submission;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.inji.verify.dto.result.VCResultDto;
import io.inji.verify.enums.VPResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of the VP token submission process, including the transaction ID, overall result status, and detailed results for each credential included in the VP.")
public class VPTokenResultDto {
    @Schema(description = "Unique identifier for the VP transaction.")
    String transactionId;
    @Schema(description = "Overall status of the VP result.")
    VPResultStatus vpResultStatus;
    @Schema(description = "Detailed results for each credential included in the VP.")
    List<VCResultDto> vcResults;
}
