package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.testsupport.DcqlTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import io.inji.verify.shared.Constants;
public class VPRequestResponseDtoTest {

    @Test
    public void should_populateAllFields_when_constructed() {
        String transactionId = "tx123";
        String requestId = "req123";
        AuthorizationRequestResponseDto authorizationDetails =
                new AuthorizationRequestResponseDto(
                        "client123",
                        DcqlTestFixtures.minimalDcqlDto(),
                        null,
                        "nonce123",
                        "url",
                        false,
                        false,
                        Constants.RESPONSE_MODE_DIRECT_POST,
                        null);
        long expiresAt = 1687318740000L;

        VPRequestResponseDto vpRequestResponseDto =
                new VPRequestResponseDto(transactionId, requestId, authorizationDetails, expiresAt, "url", null);

        assertEquals(transactionId, vpRequestResponseDto.getTransactionId());
        assertEquals(requestId, vpRequestResponseDto.getRequestId());
        assertEquals(authorizationDetails, vpRequestResponseDto.getAuthorizationDetails());
        assertEquals(expiresAt, vpRequestResponseDto.getExpiresAt());
        assertEquals("url", vpRequestResponseDto.getRequestUri());
        assertEquals("url", vpRequestResponseDto.getAuthorizationDetails().getResponseUri());
    }
}
