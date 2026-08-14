package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class FieldDTOTest {

    @Test
    public void shouldTestConstructor() {
        String[] path = {"path1", "path2"};
        FilterDTO filter = new FilterDTO("","");

        FieldDTO fieldDTO = new FieldDTO(path, filter);

        assertArrayEquals(path, fieldDTO.getPath());
        assertEquals(filter, fieldDTO.getFilter());
    }
}