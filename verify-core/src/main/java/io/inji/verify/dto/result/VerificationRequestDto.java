package io.inji.verify.dto.result;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Request parameters for VP verification process, allowing clients to specify options for status checks and claims inclusion.")
public class VerificationRequestDto {
    @Schema(description = "Flag to indicate whether to skip status checks during verification. If true, the verification process will not perform any status checks on the credentials.")
    private boolean skipStatusChecks = false;
    @Schema(description = "List of specific status check types to perform during verification. If provided, only the specified status checks will be executed. If empty or not provided, all status checks will be performed by default.")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> statusCheckFilters = new ArrayList<>();
    @Schema(description = "Flag to indicate whether to include claims in the verification result. If true, the claims from the verified credentials will be included in the response. If false, only the verification status will be returned without any claim details.")
    private boolean includeClaims = false;
}
