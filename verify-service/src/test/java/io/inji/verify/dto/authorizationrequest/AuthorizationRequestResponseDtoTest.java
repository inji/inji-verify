package io.inji.verify.dto.authorizationrequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.models.PresentationDefinition;
import io.inji.verify.shared.Constants;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class AuthorizationRequestResponseDtoTest {

    @Test
    public void ShouldTestConstructorSetsFieldsCorrectly() {
        String clientId = "testClientId";
        PresentationDefinition presentationDefinition = mock(PresentationDefinition.class);
        String nonce = "testNonce";
        String responseUri = "testUri";

        AuthorizationRequestResponseDto responseDto =
                new AuthorizationRequestResponseDto(clientId, null,
                        new VPDefinitionResponseDto(presentationDefinition.getId(),presentationDefinition.getInputDescriptors(),presentationDefinition.getName(),presentationDefinition.getPurpose(),presentationDefinition.getFormat(),presentationDefinition.getSubmissionRequirements()),nonce,responseUri, true, false);

        assertEquals(Constants.RESPONSE_TYPE, responseDto.getResponseType());
        assertEquals(clientId, responseDto.getClientId());
        assertEquals(presentationDefinition.getURL(), responseDto.getPresentationDefinitionUri());
        assertEquals(responseUri, responseDto.getResponseUri());
        assertEquals(nonce, responseDto.getNonce());
        assertTrue(Instant.now().toEpochMilli() >= responseDto.getIssuedAt()); // Ensure issuedAt is in the past
        assertTrue(responseDto.isAcceptVPWithoutHolderProof());
    }
}