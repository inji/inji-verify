package io.inji.verify.models;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.Serializable;

@ExtendWith(MockitoExtension.class)
public class AuthorizationRequestCreateResponseTest {

    @Mock
    private AuthorizationRequestResponseDto mockAuthorizationDetails;

    @Test
    public void testConstructorAndGetters() {
        String requestId = "request123";
        String transactionId = "transaction456";
        long expiresAt = System.currentTimeMillis() + 3600000; // 1 hour from now

        AuthorizationRequestCreateResponse response = new AuthorizationRequestCreateResponse(
                requestId, transactionId, mockAuthorizationDetails, expiresAt);

        assertEquals(requestId, response.getRequestId());
        assertEquals(transactionId, response.getTransactionId());
        assertEquals(mockAuthorizationDetails, response.getAuthorizationDetails());
        assertEquals(expiresAt, response.getExpiresAt());
    }

    @Test
    public void testNoArgConstructor() {

        AuthorizationRequestCreateResponse response = new AuthorizationRequestCreateResponse();

        assertNull(response.getRequestId());
        assertNull(response.getTransactionId());
        assertNull(response.getAuthorizationDetails());
        assertEquals(0,response.getExpiresAt());
    }


    @Test
    public void testSerializable() {
        String requestId = "request123";
        String transactionId = "transaction456";
        long expiresAt = System.currentTimeMillis() + 3600000;

        AuthorizationRequestCreateResponse response = new AuthorizationRequestCreateResponse(
                requestId, transactionId, mockAuthorizationDetails, expiresAt);

        assertTrue(response instanceof Serializable);
    }

}