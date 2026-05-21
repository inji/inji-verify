package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.models.DcqlQueryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationRequestResponseDtoTest {

    @Test
    public void testAuthorizationRequestResponseDto() {
        DcqlQueryScope dcqlQueryScope = mock(DcqlQueryScope.class);
        when(dcqlQueryScope.getScope()).thenReturn("age_verification");
        when(dcqlQueryScope.getURL()).thenReturn("/dcql-query/age_verification");

        String nonce = "nonce123";
        String responseUri = "https://example.com/response";

        AuthorizationRequestResponseDto responseDto = new AuthorizationRequestResponseDto(
                "client123",
                dcqlQueryScope.getScope(),
                "https://verify.example.com" + dcqlQueryScope.getURL(),
                null,
                nonce,
                responseUri,
                true,
                false);

        assertEquals("client123", responseDto.getClientId());
        assertEquals("age_verification", responseDto.getScope());
        assertEquals("https://verify.example.com/dcql-query/age_verification", responseDto.getDcqlQueryUri());
        assertEquals(nonce, responseDto.getNonce());
        assertEquals(responseUri, responseDto.getResponseUri());
        assertEquals(true, responseDto.isAcceptVPWithoutHolderProof());
        assertEquals(false, responseDto.isResponseCodeValidationRequired());
    }
}
