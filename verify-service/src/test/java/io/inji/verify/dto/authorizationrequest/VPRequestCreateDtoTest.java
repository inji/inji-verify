package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.presentation.FormatDto;
import io.inji.verify.dto.presentation.InputDescriptorDto;
import io.inji.verify.dto.presentation.SubmissionRequirementDto;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VPRequestCreateDtoTest {
    @Test
    public void testConstructor() {
        String clientId = "client123";
        String transactionId = "tx123";
        String presentationDefinitionId = "pd123";
        String nonce = "nonce123";
        List<InputDescriptorDto> mockInputDescriptors = mock();
        List<SubmissionRequirementDto> mockSubmissionRequirements = mock();
        FormatDto mockFormatDto = mock();
        VPDefinitionResponseDto presentationDefinition = new VPDefinitionResponseDto("pd123",mockInputDescriptors,"name","purpose" ,mockFormatDto ,mockSubmissionRequirements);

        VPRequestCreateDto vpRequestCreateDto =
                new VPRequestCreateDto(clientId, transactionId,
                        presentationDefinitionId, nonce,
                        presentationDefinition, false, false);

        assertEquals(clientId, vpRequestCreateDto.getClientId());
        assertEquals(transactionId, vpRequestCreateDto.getTransactionId());
        assertEquals(presentationDefinitionId, vpRequestCreateDto.getPresentationDefinitionId());
        assertEquals(nonce, vpRequestCreateDto.getNonce());
        assertEquals(presentationDefinition, vpRequestCreateDto.getPresentationDefinition());
        assertFalse(vpRequestCreateDto.isAcceptVPWithoutHolderProof());
    }
}