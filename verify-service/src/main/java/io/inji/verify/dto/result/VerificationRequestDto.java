package io.inji.verify.dto.result;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VerificationRequestDto {
    private boolean skipStatusChecks = false;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> statusCheckFilters = new ArrayList<>();
    private boolean includeClaims = false;
}
