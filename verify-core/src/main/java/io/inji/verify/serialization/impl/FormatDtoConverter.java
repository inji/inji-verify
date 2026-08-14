package io.inji.verify.serialization.impl;

import io.inji.verify.dto.presentation.FormatDto;
import io.inji.verify.serialization.GenericConverter;
import jakarta.persistence.Converter;

@Converter
public class FormatDtoConverter extends GenericConverter<FormatDto> {
    public FormatDtoConverter() {
        super(FormatDto.class);
    }
}