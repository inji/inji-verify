package io.inji.verify.dto.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputDescriptorDto {
    private final String id;
    private final String name;
    private final String purpose;
    private final List<String> group;
    private final Map<String, Map<String, List<String>>> format;
    private final ConstraintsDTO constraints;
}
