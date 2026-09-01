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
                        clientId, DcqlTestFixtures.minimalDcqlDto(), null, nonce, responseUri, true, false, Constants.RESPONSE_MODE_DIRECT_POST, null);

        assertEquals(Constants.RESPONSE_TYPE, responseDto.getResponseType());
        assertEquals(clientId, responseDto.getClientId());
        assertEquals(DcqlTestFixtures.minimalDcqlDto(), responseDto.getDcqlQuery());
        assertEquals(responseUri, responseDto.getResponseUri());
        assertEquals(nonce, responseDto.getNonce());
        assertTrue(Instant.now().toEpochMilli() >= responseDto.getIssuedAt());
    }

    @Test
    void serializedOutputOmitsLegacyPresentationDefinitionKeys() throws Exception {
        AuthorizationRequestResponseDto dto =
                new AuthorizationRequestResponseDto(
                        "c1", DcqlTestFixtures.minimalDcqlDto(), null, "n", "u", false, false, Constants.RESPONSE_MODE_DIRECT_POST, null);

        JsonNode out = MAPPER.valueToTree(dto);
        assertFalse(out.has("presentation_definition"));
        assertFalse(out.has("presentation_definition_uri"));
        assertTrue(out.has("dcqlQuery"));
        assertEquals("c1", out.get("clientId").asText());
        assertFalse(out.has("expectedOrigins"));
        assertEquals(Constants.RESPONSE_MODE_DIRECT_POST, out.get("responseMode").asText());
    }

    @Test
    void should_includeExpectedOrigins_when_dcApiResponseMode() throws Exception {
        AuthorizationRequestResponseDto dto =
                new AuthorizationRequestResponseDto(
                        "c1", DcqlTestFixtures.minimalDcqlDto(), null, "n", null, false, false,
                        Constants.RESPONSE_MODE_DC_API, java.util.List.of("https://verify.example.com"));

        JsonNode out = MAPPER.valueToTree(dto);
        assertTrue(out.has("expectedOrigins"));
        assertEquals("https://verify.example.com", out.get("expectedOrigins").get(0).asText());
    }
}