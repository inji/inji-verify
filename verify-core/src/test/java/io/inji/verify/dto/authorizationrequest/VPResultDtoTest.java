package io.inji.verify.dto.authorizationrequest;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.json.JSONObject;

public class VPResultDtoTest {

    @Test
    public void testConstructor() throws JSONException {
        String transactionId = "tx123";
        boolean verified = true;
        JSONObject claims = new JSONObject();
        claims.put("claim1", "value1");
        claims.put("claim2", "value2");

        VPResultDto vpResultDto = new VPResultDto(transactionId, verified, claims);

        assertEquals(transactionId, vpResultDto.getTransactionId());
        assertTrue(vpResultDto.isVerified());
        assertEquals(claims, vpResultDto.getClaims());
    }
}