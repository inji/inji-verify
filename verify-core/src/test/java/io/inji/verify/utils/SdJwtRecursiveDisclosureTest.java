package io.inji.verify.utils;

import io.inji.verify.dto.dcql.ClaimQueryDto;
import io.inji.verify.dto.dcql.CredentialMetaDto;
import io.inji.verify.dto.dcql.CredentialQueryDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.shared.Constants;
import io.inji.verify.validator.DcqlValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multipaz EU PID uses recursive selective disclosure for age_equal_or_over:
 * parent disclosure embeds nested {@code _sd}, child disclosure is claim {@code "18"}.
 */
class SdJwtRecursiveDisclosureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Parent disclosure embeds nested _sd; child disclosure is claim "18"
    private static final String RECURSIVE =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJKYUhrQjVjZmVLUzNWMUYyYUhGZFYxb2NiZUNQRXNMUC1JeUhDSWJTYXBvIl19" +
            ".sig~WyJzYWx0UGFyZW50IiwiYWdlX2VxdWFsX29yX292ZXIiLHsiX3NkIjpbIllncVV6QjlNWnZNd2NRWEotWEQ3aFc2TGpwYkRiNDR1eXN2MVNtUmNFMHciXX1d~WyJzYWx0Q2hpbGQiLCIxOCIsdHJ1ZV0~";

    @Test
    void extractSdJwtClaims_expandsNestedSdInsideDisclosedObject() {
        Map<String, Object> claims = Utils.extractSdJwtClaims(RECURSIVE, null);
        assertTrue(claims.containsKey("age_equal_or_over"));
        Object age = claims.get("age_equal_or_over");
        assertInstanceOf(Map.class, age);
        @SuppressWarnings("unchecked")
        Map<String, Object> ageMap = (Map<String, Object>) age;
        assertFalse(ageMap.containsKey("_sd"), "nested _sd should be resolved");
        assertEquals(Boolean.TRUE, ageMap.get("18"));
    }

    @Test
    void dcqlPath_ageEqualOrOver18_passesForRecursiveDisclosure() {
        DcqlValidator validator = new DcqlValidator();
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("age_over_18", List.of("age_equal_or_over", "18"), List.of(true)));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(new CredentialQueryDto("cred1", Constants.FORMAT_DC_SD_JWT,
                        new CredentialMetaDto(null, null), false, false, claims, null)), null);
        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("cred1").add(RECURSIVE);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, vp));
    }
}
