package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class ConstraintsDTOTest {

    @Test
    public void shouldTestConstructor() {

        FieldDTO field1 = new FieldDTO(new String[]{"value1", "value2"},mock());
        FieldDTO field2 = new FieldDTO(new String[]{"value3", "value4"}, mock());
        FieldDTO[] fieldDTOS = {field1, field2};

        ConstraintsDTO constraintsDTO = new ConstraintsDTO(fieldDTOS);

        assertArrayEquals(constraintsDTO.getFields(), fieldDTOS);
    }

    @Test
    public void shouldTestConstructorWithEmptyFields() {
        ConstraintsDTO constraintsDTO = new ConstraintsDTO(new FieldDTO[0]);
        assertArrayEquals(new FieldDTO[0], constraintsDTO.getFields());
    }

    @Test
    public void shouldTestConstructorWithNullFields() {
        ConstraintsDTO constraintsDTO = new ConstraintsDTO(null);
        assertNull(constraintsDTO.getFields());
    }
}