package io.inji.verify.serialization.impl;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.serialization.GenericConverter;
import jakarta.persistence.Converter;

@Converter
public class AuthorizationRequestResponseDtoConverter extends GenericConverter<AuthorizationRequestResponseDto> {
    public AuthorizationRequestResponseDtoConverter() {
        super(AuthorizationRequestResponseDto.class);
    }
}