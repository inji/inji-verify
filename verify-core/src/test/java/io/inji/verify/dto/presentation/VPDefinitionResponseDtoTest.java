package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class VPDefinitionResponseDtoTest {

    @Test
    public void testSerializationAndDeserialization() {
        InputDescriptorDto inputDescriptorDto = new InputDescriptorDto("id1", "name1", "purpose1", List.of("group1"), null, null);
        SubmissionRequirementDto submissionRequirementDto = mock();
        FormatDto formatDto = mock();

        VPDefinitionResponseDto presentationDefinitionDto = new VPDefinitionResponseDto(
                "pd1",
                List.of(inputDescriptorDto),
                "name",
                "purpose",
                formatDto,
                List.of(submissionRequirementDto)
        );

        assertEquals("pd1", presentationDefinitionDto.getId());
        assertEquals(presentationDefinitionDto.getInputDescriptors(), List.of(inputDescriptorDto));
        assertEquals("name", presentationDefinitionDto.getName());
        assertEquals("purpose", presentationDefinitionDto.getPurpose());
        assertEquals(presentationDefinitionDto.getFormat(), formatDto);
        assertEquals(presentationDefinitionDto.getSubmissionRequirements(), List.of(submissionRequirementDto));
    }
}