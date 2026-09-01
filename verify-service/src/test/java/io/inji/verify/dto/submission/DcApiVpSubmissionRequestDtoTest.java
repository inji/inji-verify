package io.inji.verify.dto.submission;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DcApiVpSubmissionRequestDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldBindNonDuplicateVpToken() throws Exception {
        String json = "{\"requestId\":\"req-1\",\"vp_token\":{\"cred1\":[{\"type\":\"VP\"}]}}";

        DcApiVpSubmissionRequestDto dto = mapper.readValue(json, DcApiVpSubmissionRequestDto.class);

        assertEquals("req-1", dto.getRequestId());
        assertNotNull(dto.getVpToken());
        assertTrue(dto.getVpToken().has("cred1"));
        assertEquals(1, dto.getVpToken().size());
    }

    @Test
    void shouldRejectDuplicateVpTokenQueryIds() {
        String json = "{\"requestId\":\"req-1\",\"vp_token\":{\"cred1\":[{\"type\":\"VP\"}],\"cred1\":[{\"type\":\"VP\"}]}}";

        JsonMappingException ex = assertThrows(JsonMappingException.class,
                () -> mapper.readValue(json, DcApiVpSubmissionRequestDto.class));
        assertTrue(ex.getOriginalMessage().contains("Duplicate field"));
    }
}
