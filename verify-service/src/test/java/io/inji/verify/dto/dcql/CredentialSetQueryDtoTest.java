package io.inji.verify.dto.dcql;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialSetQueryDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void validCredentialSet_passesValidation() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(new CredentialQueryDto(
                        "cred1", "dc+sd-jwt", new CredentialMetaDto(List.of("cred1"), null), null, null)),
                List.of(new CredentialSetQueryDto(List.of(List.of("cred1")), true)));

        assertTrue(validator.validate(query).isEmpty());
    }

    @Test
    void omittedRequired_defaultsToTrue() throws Exception {
        CredentialSetQueryDto credentialSet = new ObjectMapper().readValue(
                "{\"options\":[[\"cred1\"]],\"required\":true}",
                CredentialSetQueryDto.class);

        assertTrue(credentialSet.isRequired());
    }

    @Test
    void missingOptions_failsValidation() {
        CredentialSetQueryDto credentialSet = new CredentialSetQueryDto(null, true);

        assertFalse(validator.validate(credentialSet).isEmpty());
    }

    @Test
    void emptyOptions_failsValidation() {
        CredentialSetQueryDto credentialSet = new CredentialSetQueryDto(List.of(), true);

        assertFalse(validator.validate(credentialSet).isEmpty());
    }

    @Test
    void emptyOptionCombination_failsValidation() {
        CredentialSetQueryDto credentialSet = new CredentialSetQueryDto(List.of(List.of()), true);

        assertFalse(validator.validate(credentialSet).isEmpty());
    }

    @Test
    void blankCredentialIdInOption_failsValidation() {
        CredentialSetQueryDto credentialSet = new CredentialSetQueryDto(List.of(List.of("")), true);

        assertFalse(validator.validate(credentialSet).isEmpty());
    }
}
