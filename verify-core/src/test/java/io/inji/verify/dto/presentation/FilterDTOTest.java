package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilterDTOTest {

    @Test
    public void shouldTestConstructor() {
        String type = "string";
        String pattern = ".*example.*";

        FilterDTO filterDTO = new FilterDTO(type, pattern);

        assertEquals(type, filterDTO.getType());
        assertEquals(pattern, filterDTO.getPattern());
    }
}