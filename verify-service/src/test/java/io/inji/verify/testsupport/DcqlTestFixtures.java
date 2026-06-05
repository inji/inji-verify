package io.inji.verify.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.dcql.CredentialMetaDto;
import io.inji.verify.dto.dcql.CredentialQueryDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;

import java.util.List;

public final class DcqlTestFixtures {

    public static final JsonNode MINIMAL_DCQL;
    public static final DCQLQueryDto MINIMAL_DCQL_DTO = new DCQLQueryDto(
            List.of(new CredentialQueryDto(
                    "cred1",
                    "dc+sd-jwt",
                    new CredentialMetaDto(List.of("cred1"), null),
                    null,
                    null)),
            null);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        try {
            MINIMAL_DCQL = MAPPER.readTree(
                    "{\"credentials\":[{\"id\":\"age_credential\",\"format\":\"ldp_vc\",\"meta\":{}}]}");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DcqlTestFixtures() {
    }

    public static JsonNode minimalDcql() {
        return MINIMAL_DCQL;
    }

    public static DCQLQueryDto minimalDcqlDto() {
        return MINIMAL_DCQL_DTO;
    }
}