package io.inji.verify.dto.submission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import jakarta.validation.constraints.NotNull;


@AllArgsConstructor
@Getter
public class VCSubmissionDto {
    @NotNull
    private final String vc;
    private final String transactionId;
}
