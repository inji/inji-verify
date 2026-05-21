package io.inji.verify.controller;

import io.inji.verify.services.DcqlQueryScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DcqlQueryScopeControllerTest {

    private final DcqlQueryScopeService dcqlQueryScopeService = Mockito.mock(DcqlQueryScopeService.class);

    private DcqlQueryScopeController dcqlQueryScopeController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        dcqlQueryScopeController = new DcqlQueryScopeController(dcqlQueryScopeService);
        mockMvc = MockMvcBuilders.standaloneSetup(dcqlQueryScopeController).build();
    }

    @Test
    public void testGetDcqlQueryForFound() throws Exception {
        String scope = "age_verification";
        String dcqlQuery = "{\"credentials\":[]}";

        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn(dcqlQuery);

        mockMvc.perform(get("/dcql-query/{scope}", scope)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(dcqlQueryScopeService, times(1)).getDcqlQuery(scope);
    }

    @Test
    public void testGetDcqlQueryForNotFound() throws Exception {
        String scope = "unknown";

        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn(null);

        mockMvc.perform(get("/dcql-query/{scope}", scope)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(dcqlQueryScopeService, times(1)).getDcqlQuery(scope);
    }

    @Test
    public void testGetDcqlQueryForControllerLogicFound() throws Exception {
        String scope = "age_verification";
        String dcqlQuery = "{\"credentials\":[]}";
        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn(dcqlQuery);

        ResponseEntity<?> responseEntity = dcqlQueryScopeController.getDcqlQueryFor(scope);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testGetDcqlQueryForControllerLogicNotFound() throws Exception {
        String scope = "unknown";
        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn(null);

        ResponseEntity<?> responseEntity = dcqlQueryScopeController.getDcqlQueryFor(scope);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @Test
    public void testGetDcqlQueryForMalformedDcqlQuery() throws Exception {
        String scope = "age_verification";
        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn("{invalid-json");

        mockMvc.perform(get("/dcql-query/{scope}", scope)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(dcqlQueryScopeService, times(1)).getDcqlQuery(scope);
    }

    @Test
    public void testGetDcqlQueryForControllerLogicMalformedDcqlQuery() throws Exception {
        String scope = "age_verification";
        when(dcqlQueryScopeService.getDcqlQuery(scope)).thenReturn("{invalid-json");

        ResponseEntity<?> responseEntity = dcqlQueryScopeController.getDcqlQueryFor(scope);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }
}
