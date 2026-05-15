package io.inji.verify.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DcqlTestFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final JsonNode MINIMAL_DCQL;

    static {
        try {
            MINIMAL_DCQL = MAPPER.readTree(
                    "{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\"}]}");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static JsonNode minimalDcql() {
        return MINIMAL_DCQL;
    }

    private DcqlTestFixtures() {}
}
