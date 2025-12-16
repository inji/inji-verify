package io.inji.verify.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.inji.verify.dto.presentation.FormatDto;
import io.inji.verify.dto.presentation.InputDescriptorDto;
import io.inji.verify.dto.presentation.SubmissionRequirementDto;
import io.inji.verify.serialization.impl.FormatDtoConverter;
import io.inji.verify.serialization.impl.ListInputDescriptorDtoConverter;
import io.inji.verify.serialization.impl.ListSubmissionRequirementDtoConverter;
import io.inji.verify.shared.Constants;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Table(name = "presentation_definition")
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class PresentationDefinition {
    @Id
    private final String id;

    @Convert(converter = ListInputDescriptorDtoConverter.class)
    @Column(columnDefinition = "TEXT")
    private final List<InputDescriptorDto> inputDescriptors;

    private final String name;

    private final String purpose;

    @Column(columnDefinition = "TEXT", name = "vp_format")
    @Convert(converter = FormatDtoConverter.class)
    private final FormatDto format;

    @Convert(converter = ListSubmissionRequirementDtoConverter.class)
    @Column(columnDefinition = "TEXT")
    private final List<SubmissionRequirementDto> submissionRequirements;

    @JsonIgnore
    public String getURL() {
        return Constants.VP_DEFINITION_URI + this.id;
    }

}