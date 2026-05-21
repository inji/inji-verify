package io.inji.verify.exception;

import io.inji.verify.enums.ErrorCode;

public class DcqlQueryScopeNotFoundException extends RuntimeException {
    private static final String message = ErrorCode.NO_DCQL_QUERY_FOR_SCOPE.getErrorMessage();

    public DcqlQueryScopeNotFoundException() {
        super(message);
    }
}
