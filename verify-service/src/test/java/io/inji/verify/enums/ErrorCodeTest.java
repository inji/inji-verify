package io.inji.verify.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void testEnumValues() {
        for (ErrorCode code : ErrorCode.values()) {
            assertNotNull(code.getErrorCode(), "ErrorCode should not be null for " + code.name());
            assertNotNull(code.getErrorMessage(), "ErrorMessage should not be null for " + code.name());
        }
    }

    @Test
    void testSpecificEnumFields() {
        assertEquals("INVALID_TRANSACTION_ID", ErrorCode.INVALID_TRANSACTION_ID.getErrorCode());
        assertEquals("Invalid transaction ID, No requests found for given transaction ID.",
                ErrorCode.INVALID_TRANSACTION_ID.getErrorMessage());

        assertEquals("DID_CREATION_FAILED", ErrorCode.DID_CREATION_FAILED.getErrorCode());
        assertEquals("Error while creating DID document.",
                ErrorCode.DID_CREATION_FAILED.getErrorMessage());
    }

    @Test
    void testValueOf() {
        assertEquals(ErrorCode.NO_VP_SUBMISSION, ErrorCode.valueOf("NO_VP_SUBMISSION"));
        assertEquals(ErrorCode.VP_WITHOUT_PROOF, ErrorCode.valueOf("VP_WITHOUT_PROOF"));
    }
}
