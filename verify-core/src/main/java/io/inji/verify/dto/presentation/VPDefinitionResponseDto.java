package io.inji.verify.dto.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VPDefinitionResponseDto {
    @NotNull(message = "ID cannot be null")
    @NotBlank(message = "ID cannot be blank")
    @NotEmpty(message = "ID cannot be empty")
    String id;
    @NotNull(message = "Input Descriptors cannot be null")
    @JsonProperty("input_descriptors")
    @SerializedName("input_descriptors")
    List<InputDescriptorDto> inputDescriptors;


    private String name;

    private String purpose;

    private FormatDto format;

    @JsonProperty("submission_requirements")
    @SerializedName("submission_requirements")
    List<SubmissionRequirementDto> submissionRequirements;
}
