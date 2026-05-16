package io.inji.verify.services.impl;

import io.inji.verify.models.DcqlQueryScope;
import io.inji.verify.repository.DcqlQueryScopeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DcqlQueryScopeServiceImplTest {

    @Test
    public void shouldReturnDcqlQueryForGivenScope() {
        DcqlQueryScopeRepository mockRepository = mock(DcqlQueryScopeRepository.class);
        DcqlQueryScope dcqlQueryScope = new DcqlQueryScope("test_scope", "{\"credentials\":[]}", Instant.now());
        when(mockRepository.findById("test_scope")).thenReturn(Optional.of(dcqlQueryScope));

        DcqlQueryScopeServiceImpl service = new DcqlQueryScopeServiceImpl(mockRepository);

        String result = service.getDcqlQuery("test_scope");

        assertNotNull(result);
        assertEquals("{\"credentials\":[]}", result);
    }

    @Test
    public void shouldReturnNullIfDcqlQueryScopeIsNotFound() {
        DcqlQueryScopeRepository mockRepository = mock(DcqlQueryScopeRepository.class);
        when(mockRepository.findById("non_existent_scope")).thenReturn(Optional.empty());

        DcqlQueryScopeServiceImpl service = new DcqlQueryScopeServiceImpl(mockRepository);

        String result = service.getDcqlQuery("non_existent_scope");

        assertNull(result);
    }
}
