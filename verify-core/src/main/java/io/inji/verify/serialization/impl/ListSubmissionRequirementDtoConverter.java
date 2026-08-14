package io.inji.verify.serialization.impl;

import io.inji.verify.dto.presentation.SubmissionRequirementDto;
import io.inji.verify.serialization.ListConverter;
import jakarta.persistence.Converter;

@Converter
public class ListSubmissionRequirementDtoConverter extends ListConverter<SubmissionRequirementDto> {
    public ListSubmissionRequirementDtoConverter() {
        super(SubmissionRequirementDto.class);
    }
}