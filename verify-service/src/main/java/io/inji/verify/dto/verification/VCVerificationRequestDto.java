package io.inji.verify.dto.verification;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class VCVerificationRequestDto {
    @NotNull
    private String verifiableCredential;
    private boolean skipStatusChecks = false;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> statusCheckFilters = new ArrayList<>();
    private boolean includeClaims = false;
}
