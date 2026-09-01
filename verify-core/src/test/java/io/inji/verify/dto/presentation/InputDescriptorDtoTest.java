package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class InputDescriptorDtoTest {

    @Test
    public void shouldTestConstructor() {
        String id = "input1";
        String name = "Input 1";
        String purpose = "Purpose of Input 1";
        List<String> group = List.of("group1", "group2");
        Map<String, Map<String, List<String>>> format = new HashMap<>();
        format.put("type", Map.of("enum", List.of("value1", "value2")));
        ConstraintsDTO constraints = mock();

        InputDescriptorDto inputDescriptorDto = new InputDescriptorDto(id, name, purpose, group, format, constraints);

        assertEquals(id, inputDescriptorDto.getId());
        assertEquals(name, inputDescriptorDto.getName());
        assertEquals(purpose, inputDescriptorDto.getPurpose());
        assertEquals(inputDescriptorDto.getGroup(), group);
        assertEquals(inputDescriptorDto.getFormat(), format);
        assertEquals(inputDescriptorDto.getConstraints(), constraints);
    }

}