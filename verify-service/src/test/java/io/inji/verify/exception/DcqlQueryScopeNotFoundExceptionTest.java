package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DcqlQueryScopeNotFoundExceptionTest {

    @Test
    void shouldHaveCorrectMessage() {
        DcqlQueryScopeNotFoundException exception = new DcqlQueryScopeNotFoundException();
        assertEquals(ErrorCode.NO_DCQL_QUERY_FOR_SCOPE.getErrorMessage(), exception.getMessage());
    }
}
