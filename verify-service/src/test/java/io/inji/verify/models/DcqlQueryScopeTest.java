package io.inji.verify.models;

import io.inji.verify.shared.Constants;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DcqlQueryScopeTest {

    @Test
    void shouldReturnDcqlQueryUri() {
        DcqlQueryScope dcqlQueryScope = new DcqlQueryScope("age_verification", "{\"credentials\":[]}", Instant.now());

        assertEquals(Constants.DCQL_QUERY_URI + "age_verification", dcqlQueryScope.getURL());
        assertEquals("age_verification", dcqlQueryScope.getScope());
        assertEquals("{\"credentials\":[]}", dcqlQueryScope.getDcqlQuery());
    }
}
