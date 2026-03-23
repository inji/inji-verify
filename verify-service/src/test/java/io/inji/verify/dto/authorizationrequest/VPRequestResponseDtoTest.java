package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VPRequestResponseDtoTest {

    @Test
    public void testConstructor() {
        String transactionId = "tx123";
        String requestId = "req123";
        AuthorizationRequestResponseDto authorizationDetails =
                new AuthorizationRequestResponseDto("client123", "",
                        new VPDefinitionResponseDto("pd123", mock(),"name",
                                "purpose",mock(), mock()), "nonce123","url", false, false);
        long expiresAt = 1687318740000L;

        VPRequestResponseDto vpRequestResponseDto = new VPRequestResponseDto(transactionId, requestId, authorizationDetails, expiresAt ,"url");

        assertEquals(transactionId, vpRequestResponseDto.getTransactionId());
        assertEquals(requestId, vpRequestResponseDto.getRequestId());
        assertEquals(authorizationDetails, vpRequestResponseDto.getAuthorizationDetails());
        assertEquals(expiresAt, vpRequestResponseDto.getExpiresAt());
        assertEquals("url", vpRequestResponseDto.authorizationDetails.getResponseUri());
    }
}