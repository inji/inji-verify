package io.inji.verify.dto.authorizationrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VPRequestCreateDtoTest {

    @Test
    public void testVPRequestCreateDto() throws Exception {
        String clientId = "client123";
        String transactionId = "txn123";
        String scope = "age_verification";
        String nonce = "nonce123";
        ObjectMapper objectMapper = new ObjectMapper();
        var dcqlQuery = objectMapper.readTree("{\"credentials\":[]}");

        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                clientId, transactionId, scope, nonce, dcqlQuery, false, false);

        assertEquals(clientId, vpRequestCreateDto.getClientId());
        assertEquals(transactionId, vpRequestCreateDto.getTransactionId());
        assertEquals(scope, vpRequestCreateDto.getScope());
        assertEquals(nonce, vpRequestCreateDto.getNonce());
        assertEquals(dcqlQuery, vpRequestCreateDto.getDcqlQuery());
    }
}
