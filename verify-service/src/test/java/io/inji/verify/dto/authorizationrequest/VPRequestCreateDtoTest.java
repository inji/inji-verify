package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VPRequestCreateDtoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testConstructor() throws Exception {
        String clientId = "client123";
        String transactionId = "tx123";
        String nonce = "nonce123";
        JsonNode dcqlQuery = MAPPER.readTree(
                "{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\"}]}");

        VPRequestCreateDto vpRequestCreateDto =
                new VPRequestCreateDto(clientId, transactionId, nonce, dcqlQuery, true, false);

        assertEquals(clientId, vpRequestCreateDto.getClientId());
        assertEquals(transactionId, vpRequestCreateDto.getTransactionId());
        assertEquals(nonce, vpRequestCreateDto.getNonce());
        assertEquals(dcqlQuery, vpRequestCreateDto.getDcqlQuery());
        assertTrue(vpRequestCreateDto.isAcceptVPWithoutHolderProof());
        assertFalse(vpRequestCreateDto.isResponseCodeValidationRequired());
    }
}
