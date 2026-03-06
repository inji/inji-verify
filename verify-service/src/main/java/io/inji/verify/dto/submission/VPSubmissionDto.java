package io.inji.verify.dto.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VPSubmissionDto {
    String vpToken;
    @Valid
    PresentationSubmissionDto presentationSubmission;
    @NotNull
    String state;
    String error;
    String errorDescription;
    String responseCode;
    Timestamp responseCodeExpiryAt;
    boolean responseCodeUsed;
}