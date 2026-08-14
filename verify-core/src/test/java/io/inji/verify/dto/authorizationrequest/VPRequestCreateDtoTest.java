package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.testsupport.DcqlTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VPRequestCreateDtoTest {

    @Test
    public void testConstructor() {
        String clientId = "client123";
        String transactionId = "tx123";
        String nonce = "nonce123";
        DCQLQueryDto dcqlQuery = DcqlTestFixtures.minimalDcqlDto();

        VPRequestCreateDto vpRequestCreateDto =
                new VPRequestCreateDto(clientId, transactionId, nonce, dcqlQuery, false);

        assertEquals(clientId, vpRequestCreateDto.getClientId());
        assertEquals(transactionId, vpRequestCreateDto.getTransactionId());
        assertEquals(nonce, vpRequestCreateDto.getNonce());
        assertEquals(dcqlQuery, vpRequestCreateDto.getDcqlQuery());
        assertEquals(false, vpRequestCreateDto.isResponseCodeValidationRequired());
    }
}
