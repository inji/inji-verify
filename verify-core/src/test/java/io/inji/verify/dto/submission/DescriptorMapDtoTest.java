package io.inji.verify.dto.submission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescriptorMapDtoTest {
    @Test
    public void shouldTestConstructor() {
        String id = "id1";
        String format = "string";
        String path = "path1";
        PathNestedDto pathNested = new PathNestedDto("parent", "child");

        DescriptorMapDto descriptorMapDto = new DescriptorMapDto(id, format, path, pathNested);

        assertEquals(id, descriptorMapDto.getId());
        assertEquals(format, descriptorMapDto.getFormat());
        assertEquals(path, descriptorMapDto.getPath());
        assertEquals(pathNested, descriptorMapDto.getPathNested());
    }

}