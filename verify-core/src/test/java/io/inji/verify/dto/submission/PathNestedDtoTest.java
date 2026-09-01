package io.inji.verify.dto.submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathNestedDtoTest {
    @Test
    public void shouldTestConstructor() {
        String format = "string";
        String path = "path1";
        PathNestedDto pathNested = new PathNestedDto("string", "path1");

        assertEquals(format, pathNested.getFormat());
        assertEquals(path, pathNested.getPath());
    }

}