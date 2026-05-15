package io.inji.verify.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.services.VerifiablePresentationRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
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

    private final VerifiablePresentationRequestService verifiablePresentationRequestService =
            Mockito.mock(VerifiablePresentationRequestService.class);

    private MockMvc mockMvc;

    /** Matches production Jackson setup (parameter names + Lombok @ConstructorProperties). */
    private final ObjectMapper objectMapper =
            Jackson2ObjectMapperBuilder.json().modules(new ParameterNamesModule()).build();

    private static String validVpRequestJson() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\"}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
    }

    private static String validVpSessionRequestJson() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\"}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":true}";
    }

    private static String vpRequestJsonWithoutDcql() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
    }

    @BeforeEach
    public void setUp() {
        VPRequestController vpRequestController = new VPRequestController(verifiablePresentationRequestService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(vpRequestController).build();
    }

    @Test
    public void testCreateVPRequest_Success() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "");

        when(verifiablePresentationRequestService.createAuthorizationRequest(any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVpRequestJson()))
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
    public void testCreateVPRequest_BadRequest_NoDcqlQuery() throws Exception {
        mockMvc.perform(post("/v2/vp-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vpRequestJsonWithoutDcql()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.DCQL_QUERY_REQUIRED))));

        verify(verifiablePresentationRequestService, never()).createAuthorizationRequest(any());
    }

    @Test
    public void testCreateVPRequest_InternalServerError_WhenServiceThrows() throws Exception {
        when(verifiablePresentationRequestService.createAuthorizationRequest(any()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVpRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.INTERNAL_SERVER_ERROR))));
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
        assertEquals(objectMapper.writeValueAsString(statusDto), objectMapper.writeValueAsString(result));
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
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "");

        when(verifiablePresentationRequestService.createAuthorizationRequest(any())).thenReturn(responseDto);

        String expectedCookieValue = Base64.getEncoder().encodeToString("tId".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/v2/vp-session-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVpSessionRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("transaction_id=" + expectedCookieValue)))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void testCreateVPRequest_LegacyPresentation_definition_snakeCaseOnly_Returns400() throws Exception {
        String body =
                "{\"clientId\":\"c1\",\"nonce\":\"n\",\"transactionId\":\"t\",\"presentation_definition\":{},"
                        + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
        mockMvc.perform(post("/v2/vp-request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(objectMapper.writeValueAsString(
                                        new ErrorDto(ErrorCode.PRESENTATION_DEFINITION_NOT_SUPPORTED))));

        verify(verifiablePresentationRequestService, never()).createAuthorizationRequest(any());
    }

    @Test
    void testCreateVPRequest_LegacyPresentation_definition_uri_snakeCaseOnly_Returns400() throws Exception {
        String body =
                "{\"clientId\":\"c1\",\"nonce\":\"n\",\"presentation_definition_uri\":\"https://example.com/pd\"}";
        mockMvc.perform(post("/v2/vp-request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(objectMapper.writeValueAsString(
                                        new ErrorDto(ErrorCode.PRESENTATION_DEFINITION_NOT_SUPPORTED))));

        verify(verifiablePresentationRequestService, never()).createAuthorizationRequest(any());
    }

    @Test
    void testCreateVPRequest_DcqlPlusLegacyPresentation_definition_Returns400Ambiguous() throws Exception {
        String body =
                "{\"clientId\":\"c1\",\"nonce\":\"n\",\"dcqlQuery\":{\"credentials\":[{\"id\":\"x\",\"format\":\"dc+sd-jwt\"}]},"
                        + "\"presentation_definition\":{\"id\":\"pd\"}}";
        mockMvc.perform(post("/v2/vp-request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(
                        content()
                                .string(objectMapper.writeValueAsString(new ErrorDto(ErrorCode.AMBIGUOUS_QUERY))));

        verify(verifiablePresentationRequestService, never()).createAuthorizationRequest(any());
    }
}
