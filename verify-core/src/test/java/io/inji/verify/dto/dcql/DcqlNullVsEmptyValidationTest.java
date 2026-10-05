package io.inji.verify.dto.dcql;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A null list and an empty list must each fail exactly one constraint. If a null list
 * failed two constraints, the API would return whichever violation came first, and that
 * order is not fixed, so the returned error code would change between requests.
 */
class DcqlNullVsEmptyValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static <T> Set<String> violationMessages(T bean) {
        return validator.validate(bean).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void should_failOnlyRequiredConstraint_when_credentialsIsNull() {
        assertEquals(Set.of("DCQL_CREDENTIALS_REQUIRED"), violationMessages(new DCQLQueryDto(null, null)));
    }

    @Test
    void should_failOnlyInvalidConstraint_when_credentialsIsEmpty() {
        assertEquals(Set.of("DCQL_CREDENTIALS_INVALID"), violationMessages(new DCQLQueryDto(List.of(), null)));
    }

    @Test
    void should_failOnlyRequiredConstraint_when_claimPathIsNull() {
        assertEquals(Set.of("DCQL_CLAIM_PATH_REQUIRED"), violationMessages(new ClaimQueryDto(null, null, null)));
    }

    @Test
    void should_failOnlyInvalidConstraint_when_claimPathIsEmpty() {
        assertEquals(Set.of("DCQL_CLAIM_PATH_INVALID"), violationMessages(new ClaimQueryDto(null, List.of(), null)));
    }

    @Test
    void should_failOnlyRequiredConstraint_when_credentialSetOptionsIsNull() {
        assertEquals(Set.of("DCQL_CREDENTIAL_SETS_REQUIRED"), violationMessages(new CredentialSetQueryDto(null, true)));
    }

    @Test
    void should_failOnlyInvalidConstraint_when_credentialSetOptionsIsEmpty() {
        assertEquals(Set.of("DCQL_CREDENTIAL_SETS_INVALID"), violationMessages(new CredentialSetQueryDto(List.of(), true)));
    }

    @Test
    void should_returnExactlyOneViolation_when_credentialsIsNull() {
        assertEquals(1, validator.validate(new DCQLQueryDto(null, null)).size());
    }
}
