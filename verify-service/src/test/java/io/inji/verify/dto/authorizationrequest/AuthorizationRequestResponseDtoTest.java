package io.inji.verify.dto.authorizationrequest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.inji.verify.shared.Constants;
import io.inji.verify.testsupport.DcqlTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Instant;

public class AuthorizationRequestResponseDtoTest {

    private static final ObjectMapper MAPPER =
            Jackson2ObjectMapperBuilder.json().modules(new ParameterNamesModule()).build();

    @Test
    public void shouldCreateDcqlOnlyInstance() {
        String clientId = "testClientId";
        String nonce = "testNonce";
        String responseUri = "testUri";

        AuthorizationRequestResponseDto responseDto =
                new AuthorizationRequestResponseDto(
                        clientId, DcqlTestFixtures.minimalDcql(), nonce, responseUri, true, false);

        assertEquals(Constants.RESPONSE_TYPE, responseDto.getResponseType());
        assertEquals(clientId, responseDto.getClientId());
        assertEquals(DcqlTestFixtures.minimalDcql(), responseDto.getDcqlQuery());
        assertEquals(responseUri, responseDto.getResponseUri());
        assertEquals(nonce, responseDto.getNonce());
        assertTrue(Instant.now().toEpochMilli() >= responseDto.getIssuedAt());
        assertTrue(responseDto.isAcceptVPWithoutHolderProof());
    }

    @Test
    void serializedOutputOmitsLegacyPresentationDefinitionKeys() throws Exception {
        AuthorizationRequestResponseDto dto =
                new AuthorizationRequestResponseDto(
                        "c1", DcqlTestFixtures.minimalDcql(), "n", "u", false, false);

        JsonNode out = MAPPER.valueToTree(dto);
        assertFalse(out.has("presentation_definition"));
        assertFalse(out.has("presentation_definition_uri"));
        assertTrue(out.has("dcqlQuery"));
        assertEquals("c1", out.get("clientId").asText());
    }
}
