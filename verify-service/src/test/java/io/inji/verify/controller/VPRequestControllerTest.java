package io.inji.verify.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.DcqlQueryScopeNotFoundException;
import io.inji.verify.services.VerifiablePresentationRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.DeferredResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

public class VPRequestControllerTest {

    private final VerifiablePresentationRequestService verifiablePresentationRequestService = Mockito.mock(VerifiablePresentationRequestService.class);

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        VPRequestController vpRequestController = new VPRequestController(verifiablePresentationRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(vpRequestController).build();
    }

    @Test
    public void testCreateVPRequest_Success() throws Exception {
        JsonNode dcqlQuery = objectMapper.readTree("{\"credentials\":[]}");
        VPRequestCreateDto createDto = new VPRequestCreateDto("cId", "tId", null, "nonce", dcqlQuery, false, false);
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "");

        when(verifiablePresentationRequestService.createAuthorizationRequest(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(result -> {
                    JsonNode expected = objectMapper.readTree(
                            objectMapper.writeValueAsString(responseDto)
                    );
                    JsonNode actual = objectMapper.readTree(
                            result.getResponse().getContentAsString()
                    );
                    assertEquals(expected, actual);
                });
    }

    @Test
    public void testCreateVPRequest_BadRequest_NoDefinition() throws Exception {
        VPRequestCreateDto createDto = new VPRequestCreateDto("cId", "tId", null, "nonce", null, false, false);

        mockMvc.perform(post("/v2/vp-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.BOTH_SCOPE_AND_DCQL_CANNOT_BE_NULL))));

        verify(verifiablePresentationRequestService, never()).createAuthorizationRequest(any());
    }

    @Test
    public void testCreateVPRequest_NotFound() throws Exception {
        JsonNode dcqlQuery = objectMapper.readTree("{\"credentials\":[]}");
        VPRequestCreateDto createDto = new VPRequestCreateDto("cId", "tId", "unknown_scope", "nonce", dcqlQuery, false, false);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any()))
                .thenThrow(new DcqlQueryScopeNotFoundException());

        mockMvc.perform(post("/v2/vp-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.NO_DCQL_QUERY_FOR_SCOPE))));
    }

    @Test
    public void testGetStatus() throws Exception {
        String requestId = "req789";
        VPRequestStatusDto statusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        DeferredResult<VPRequestStatusDto> deferredResult = new DeferredResult<>();

        when(verifiablePresentationRequestService.getStatus(requestId)).thenReturn(deferredResult);

        MvcResult mvcResult = mockMvc.perform(get("/vp-request/{requestId}/status", requestId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        deferredResult.setResult(statusDto);
        Object result = mvcResult.getAsyncResult();
        assertEquals(new ObjectMapper().writeValueAsString(statusDto), new ObjectMapper().writeValueAsString(result));
        verify(verifiablePresentationRequestService, times(1)).getStatus(requestId);
    }

    @Test
    public void testGetVPRequest_Success() throws Exception {
        String requestId = "req123";
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        when(verifiablePresentationRequestService.getVPRequestJwt(requestId)).thenReturn(jwt);

        mockMvc.perform(get("/v2/vp-request/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/oauth-authz-req+jwt"))
                .andExpect(content().string(jwt));

        verify(verifiablePresentationRequestService, times(1)).getVPRequestJwt(requestId);
    }

    @Test
    public void testCreateVPSessionRequest_SetsCookie() throws Exception {
        JsonNode dcqlQuery = objectMapper.readTree("{\"credentials\":[]}");
        VPRequestCreateDto createDto = new VPRequestCreateDto("cId", "tId", null, "nonce", dcqlQuery, false, true);
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "");

        when(verifiablePresentationRequestService.createAuthorizationRequest(any())).thenReturn(responseDto);

        String expectedCookieValue = Base64.getEncoder().encodeToString("tId".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/v2/vp-session-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("transaction_id=" + expectedCookieValue)))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }
}
