package io.inji.verify.serialization.impl;

import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.serialization.GenericConverter;
import jakarta.persistence.Converter;

@Converter
public class PresentationSubmissionDtoConverter extends GenericConverter<PresentationSubmissionDto> {
    public PresentationSubmissionDtoConverter() {
        super(PresentationSubmissionDto.class);
    }
}
