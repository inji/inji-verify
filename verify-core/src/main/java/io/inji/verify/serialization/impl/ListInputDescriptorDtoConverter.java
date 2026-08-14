package io.inji.verify.serialization.impl;

import io.inji.verify.dto.presentation.InputDescriptorDto;
import io.inji.verify.serialization.ListConverter;
import jakarta.persistence.Converter;


@Converter
public class ListInputDescriptorDtoConverter extends ListConverter<InputDescriptorDto> {
    public ListInputDescriptorDtoConverter() {
        super(InputDescriptorDto.class);
    }
}