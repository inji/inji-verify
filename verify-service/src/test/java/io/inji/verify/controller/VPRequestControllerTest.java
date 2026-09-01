package io.inji.verify.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.exception.VPRequestValidationException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.shared.Constants;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.async.DeferredResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .modules(new ParameterNamesModule())
            .build();

    private static String validVpRequestJson() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"cred1\"]}}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
    }

    private static String validVpSessionRequestJson() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"cred1\"]}}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":true}";
    }

    private static String vpRequestJsonWithoutDcql() {
        return "{\"clientId\":\"cId\",\"transactionId\":\"tId\",\"nonce\":\"nonce\","
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
    }

    @BeforeEach
    public void setUp() {
        VPRequestController vpRequestController = new VPRequestController(verifiablePresentationRequestService);
        mockMvc = MockMvcBuilders.standaloneSetup(vpRequestController)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper),
                        new StringHttpMessageConverter())
                .setValidator(noOpSpringValidator())
                .build();
    }

    /** Bypasses bean validation so controller wiring can be tested in isolation. */
    private static org.springframework.validation.Validator noOpSpringValidator() {
        return new org.springframework.validation.Validator() {
            @Override
            public boolean supports(Class<?> clazz) {
                return true;
            }

            @Override
            public void validate(Object target, org.springframework.validation.Errors errors) {
                // no-op
            }
        };
    }

    private static String validDcApiVpRequestJson() {
        return "{\"clientId\":\"decentralized_identifier:did:web:verify.example.com\","
                + "\"transactionId\":\"tId\",\"nonce\":\"nonce-value-123456\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"cred1\"]}}]},"
                + "\"responseCodeValidationRequired\":false,"
                + "\"responseMode\":\"" + Constants.RESPONSE_MODE_DC_API + "\"}";
    }

    @Test
    public void should_createVpRequest_when_requestIsValid() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "", null);

        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

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
    void should_forwardOrigin_when_dcApiVpRequest() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", null, 0L, "https://verify.example.com/v1/verify/v2/vp-request/rId", null);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://verify.example.com")
                        .content(validDcApiVpRequestJson()))
                .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Optional<String>> originCaptor = ArgumentCaptor.forClass(java.util.Optional.class);
        verify(verifiablePresentationRequestService, times(1))
                .createAuthorizationRequest(any(), originCaptor.capture());
        assertEquals(java.util.Optional.of("https://verify.example.com"), originCaptor.getValue());
    }

    @Test
    void should_forwardOrigin_when_dcApiVpSessionRequest() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", null, 0L, "https://verify.example.com/v1/verify/v2/vp-request/rId", null);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-session-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://verify.example.com")
                        .content(validDcApiVpRequestJson()))
                .andExpect(status().isCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Optional<String>> originCaptor = ArgumentCaptor.forClass(java.util.Optional.class);
        verify(verifiablePresentationRequestService, times(1))
                .createAuthorizationRequest(any(), originCaptor.capture());
        assertEquals(java.util.Optional.of("https://verify.example.com"), originCaptor.getValue());
    }

    @Test
    public void should_acceptVpRequest_when_dcqlQueryIsMissing() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "", null);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(vpRequestJsonWithoutDcql()))
                .andExpect(status().isCreated());

        verify(verifiablePresentationRequestService, times(1)).createAuthorizationRequest(any(), any());
    }

    @Test
    public void should_propagateException_when_serviceThrows() {
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any()))
                .thenThrow(new RuntimeException("unexpected"));

        assertThrows(Exception.class, () -> mockMvc.perform(post("/v2/vp-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validVpRequestJson())));
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
    public void testGetVPRequest_NotFound() throws Exception {
        String requestId = "missing-req";

        when(verifiablePresentationRequestService.getVPRequestJwt(requestId))
                .thenThrow(new io.inji.verify.exception.VPRequestNotFoundException());

        mockMvc.perform(get("/v2/vp-request/{requestId}", requestId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(
                        new ErrorDto(io.inji.verify.enums.ErrorCode.NO_AUTH_REQUEST))));

        verify(verifiablePresentationRequestService, times(1)).getVPRequestJwt(requestId);
    }

    @Test
    public void testGetStatus_NotFound() throws Exception {
        String requestId = "missing-req";
        DeferredResult<VPRequestStatusDto> deferredResult = new DeferredResult<>();

        when(verifiablePresentationRequestService.getStatus(requestId)).thenReturn(deferredResult);

        MvcResult mvcResult = mockMvc.perform(get("/vp-request/{requestId}/status", requestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                .andReturn();

        deferredResult.setErrorResult(new io.inji.verify.exception.VPRequestNotFoundException());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(
                        new ErrorDto(io.inji.verify.enums.ErrorCode.NO_AUTH_REQUEST))));

        verify(verifiablePresentationRequestService, times(1)).getStatus(requestId);
    }

    @Test
    public void testGetStatus_NotFound1() throws Exception {
        String requestId = "missing-req";
        DeferredResult<VPRequestStatusDto> deferredResult = new DeferredResult<>();

        when(verifiablePresentationRequestService.getStatus(requestId)).thenReturn(deferredResult);

        MvcResult mvcResult = mockMvc.perform(get("/vp-request/{requestId}/status", requestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                .andReturn();

        deferredResult.setErrorResult(new io.inji.verify.exception.VPRequestNotFoundException());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(content().string(objectMapper.writeValueAsString(
                        new ErrorDto(io.inji.verify.enums.ErrorCode.NO_AUTH_REQUEST))));

        verify(verifiablePresentationRequestService, times(1)).getStatus(requestId);
    }

    @Test
    public void should_setTransactionCookie_when_vpSessionRequestCreated() throws Exception {
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "", null);

        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

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
    void should_acceptVpRequest_when_credentialMetaIsMissing() throws Exception {
        String body =
                "{\"clientId\":\"c1\",\"nonce\":\"n\",\"dcqlQuery\":{\"credentials\":[{\"id\":\"x\",\"format\":\"dc+sd-jwt\"}]},"
                        + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "", null);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(verifiablePresentationRequestService, times(1)).createAuthorizationRequest(any(), any());
    }

    @Test
    void should_acceptVpRequest_when_credentialMetaIsEmpty() throws Exception {
        String body =
                "{\"clientId\":\"c1\",\"nonce\":\"n\",\"dcqlQuery\":{\"credentials\":[{\"id\":\"x\",\"format\":\"dc+sd-jwt\",\"meta\":{}}]},"
                        + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";
        VPRequestResponseDto responseDto = new VPRequestResponseDto("tId", "rId", mock(), 0L, "", null);
        when(verifiablePresentationRequestService.createAuthorizationRequest(any(), any())).thenReturn(responseDto);

        mockMvc.perform(post("/v2/vp-request").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(verifiablePresentationRequestService, times(1)).createAuthorizationRequest(any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenTypeValuesIsAFlatArrayInsteadOfArrayOfArrays() throws Exception {
        // type_values must be [[...],[...]] — a flat array like ["TypeA","TypeB"] is invalid.
        String body = "{\"clientId\":\"c1\",\"nonce\":\"n\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"x\",\"format\":\"ldp_vc\","
                + "\"meta\":{\"type_values\":"
                + "[\"https://www.w3.org/2018/credentials#VerifiableCredential\","
                + "\"https://example.org/credentials#AgeCredential\"]"
                + "}}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";

        MvcResult result = mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode responseNode =
                objectMapper.readTree(result.getResponse().getContentAsString());
        String errorMessage = responseNode.path("errorMessage").asText("");
        assertEquals("type_values must be an array of arrays.", errorMessage);
    }

    @Test
    void shouldReturnBadRequest_whenVctValuesIsAStringInsteadOfArray() throws Exception {
        // vct_values must be ["uri1","uri2"] — a plain string is invalid.
        String body = "{\"clientId\":\"c1\",\"nonce\":\"n\","
                + "\"dcqlQuery\":{\"credentials\":[{\"id\":\"x\",\"format\":\"dc+sd-jwt\","
                + "\"meta\":{\"vct_values\":\"https://example.com/AgeCredential\"}}]},"
                + "\"acceptVPWithoutHolderProof\":false,\"responseCodeValidationRequired\":false}";

        MvcResult result = mockMvc.perform(post("/v2/vp-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode responseNode =
                objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("vct_values must be an array of strings.",
                responseNode.path("errorMessage").asText(""));
    }

    // ── handleJsonErrors direct tests ──────────────────────────────────────────

    @Test
    void handleJsonErrors_UnrecognizedProperty_returnsUnknownField() {
        UnrecognizedPropertyException upe = mock(UnrecognizedPropertyException.class);
        when(upe.getPropertyName()).thenReturn("unknownField");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(upe);

        ResponseEntity<ErrorDto> response =
                new VPRequestController(verifiablePresentationRequestService)
                        .handleJsonErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNKNOWN_FIELD", response.getBody().getErrorCode());
        assertTrue(response.getBody().getErrorMessage().contains("unknownField"));
    }

    @Test
    void handleJsonErrors_InvalidFormat_returnsInvalidFieldValue() {
        InvalidFormatException ife = mock(InvalidFormatException.class);
        when(ife.getPath()).thenReturn(List.of());
        when(ife.getValue()).thenReturn("badValue");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(ife);

        ResponseEntity<ErrorDto> response =
                new VPRequestController(verifiablePresentationRequestService)
                        .handleJsonErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_FIELD_VALUE", response.getBody().getErrorCode());
        assertTrue(response.getBody().getErrorMessage().contains("badValue"));
    }

    @Test
    void handleJsonErrors_MismatchedInput_generalField_returnsMalformedRequest() {
        MismatchedInputException mie = mock(MismatchedInputException.class);
        when(mie.getPath()).thenReturn(List.of());
        when(mie.getOriginalMessage()).thenReturn("Cannot deserialize value of type");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(mie);

        ResponseEntity<ErrorDto> response =
                new VPRequestController(verifiablePresentationRequestService)
                        .handleJsonErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_REQUEST", response.getBody().getErrorCode());
        assertEquals("Cannot deserialize value of type", response.getBody().getErrorMessage());
    }

    @Test
    void handleJsonErrors_JsonParseException_returnsInvalidJson() {
        JsonParseException jpe = mock(JsonParseException.class);
        when(jpe.getOriginalMessage()).thenReturn("Unexpected character ('{')");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(jpe);

        ResponseEntity<ErrorDto> response =
                new VPRequestController(verifiablePresentationRequestService)
                        .handleJsonErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_JSON", response.getBody().getErrorCode());
        assertEquals("Unexpected character ('{')", response.getBody().getErrorMessage());
    }

    @Test
    void handleJsonErrors_unknownCause_returnsInvalidRequest() {
        RuntimeException cause = new RuntimeException("some unexpected low-level error");
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(cause);

        ResponseEntity<ErrorDto> response =
                new VPRequestController(verifiablePresentationRequestService)
                        .handleJsonErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().getErrorCode());
        assertEquals("some unexpected low-level error", response.getBody().getErrorMessage());
    }

    @Test
    void should_returnErrorDto_when_vpRequestValidationException() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "VPRequestCreateDto");
        bindingResult.addError(new FieldError(
                "VPRequestCreateDto",
                "dcqlQuery.credentials[0].format",
                null,
                false,
                new String[]{"NotBlank"},
                null,
                "DCQL_CREDENTIAL_FORMAT_REQUIRED"));

        MethodParameter parameter = new MethodParameter(
                VPRequestController.class.getDeclaredMethod("createVPSessionRequest", VPRequestCreateDto.class, jakarta.servlet.http.HttpServletRequest.class),
                0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, bindingResult);

        var response = new VPRequestController(verifiablePresentationRequestService)
                .handleVPRequestValidationException(VPRequestValidationException.from(ex));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorDto body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getErrorCode());
        assertFalse(body.getErrorCode().isBlank());
        assertNotNull(body.getErrorMessage());
        assertFalse(body.getErrorMessage().isBlank());
    }

}
