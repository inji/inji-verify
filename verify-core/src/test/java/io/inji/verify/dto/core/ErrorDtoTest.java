package io.inji.verify.dto.core;

import io.inji.verify.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDtoTest {
    @Test
    public void shouldTestConstructor() {

        ErrorDto errorDto = new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID);

        assertEquals(ErrorCode.INVALID_TRANSACTION_ID.name(), errorDto.getErrorCode());
    }

}