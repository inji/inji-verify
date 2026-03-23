package io.inji.verify.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class VPSubmissionControllerTest {

    String redirectUri = "https://example.com/callback";

    private final VerifiablePresentationRequestService verifiablePresentationRequestService = Mockito.mock(VerifiablePresentationRequestService.class);

    private final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService = Mockito.mock(VerifiablePresentationSubmissionService.class);

    private final Gson gson = Mockito.mock(Gson.class);

    private final AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository = Mockito.mock(AuthorizationRequestCreateResponseRepository.class);

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        VPSubmissionController vpSubmissionController = new VPSubmissionController(verifiablePresentationRequestService, verifiablePresentationSubmissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(vpSubmissionController).build();
    }

    @Test
    public void testSubmitVP_Success() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        doReturn(ResponseEntity.ok().build())
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk());

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testResponseWithRedirectUriOnSubmitVP_Success() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestResponseDto authorizationRequestResponseDto = new AuthorizationRequestResponseDto(
                "clientId",
                "presentationDefinitionUri",
                null,
                "nonce",
                "responseUri",
                false,
                true
        );

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(
                state,
                "transactionId",
                authorizationRequestResponseDto,
                System.currentTimeMillis() + 100000
        );

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authorizationRequestCreateResponse));

        java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("redirect_uri", redirectUri + "?response_code=test-response-code");
        doReturn(ResponseEntity.ok(responseBody))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists());

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testResponseWithEmptyBodyIfRedirectUriNotPresentOnSubmitVP_Success() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());



        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        doReturn(ResponseEntity.ok(new java.util.HashMap<>()))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").doesNotExist());

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testSubmitVP_NotFound() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("testId","dId",new ArrayList<>());

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isNotFound());

        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testSubmitVP_MissingParams() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSubmitVP_BothVpTokenAndErrorMissing() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("state", "testState"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid response: either vp_token and presentation_submission must be provided, or error must be provided."));
        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testSubmitVP_BothVpTokenAndErrorPresent() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", "token")
                        .param("error", "some_error")
                        .param("state", "testState"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid response: either vp_token and presentation_submission must be provided, or error must be provided."));
        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testSubmitVP_PresentationSubmissionFailsValidation() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"\"}"; // id blank, should fail @NotBlank
        String state = "testState";
        PresentationSubmissionDto invalidDto = new PresentationSubmissionDto("", "", new ArrayList<>());
        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(invalidDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        doReturn(ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body("must not be blank"))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
    }

    @Test
    public void testSubmitVP_OnlyErrorPresent() throws Exception {
        String error = "some_error";
        String state = "testState";
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        doReturn(ResponseEntity.ok(new java.util.HashMap<>()))
                .when(verifiablePresentationSubmissionService).submit(null, null, state, error, null);
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("error", error)
                        .param("state", state))
                .andExpect(status().isOk());
        verify(verifiablePresentationSubmissionService, times(1)).submit(null, null, state, error, null);
    }

    @Test
    public void testSubmitVP_MissingState() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", "token"))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testSubmitVP_BlankState() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", "token")
                        .param("state", " "))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testSubmitVP_OnlyVpTokenPresent() throws Exception {
        String vpToken = "testToken";
        String state = "testState";
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("state", state))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid response: either vp_token and presentation_submission must be provided, or error must be provided."));
        verify(verifiablePresentationSubmissionService, times(0)).submit(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testGetResponse_RedirectUri_ResponseCode_SuccessVPSubmit() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestResponseDto authorizationRequestResponseDto = new AuthorizationRequestResponseDto(
                "clientId",
                "presentationDefinitionUri",
                null,
                "nonce",
                "responseUri",
                false,
                true
        );

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(
                state,
                "transactionId",
                authorizationRequestResponseDto,
                System.currentTimeMillis() + 100000
        );

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authorizationRequestCreateResponse));

        java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("redirect_uri", redirectUri + "?response_code=test-response-code-123");
        doReturn(ResponseEntity.ok(responseBody))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists())
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString(redirectUri.split("\\?")[0])))
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString("response_code=")));

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testNoRedirectUriInResponse_CrossDevice_SuccessVPSubmit() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestResponseDto authorizationRequestResponseDto = new AuthorizationRequestResponseDto(
                "clientId",
                "presentationDefinitionUri",
                null,
                "nonce",
                "responseUri",
                false,
                false
        );

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(
                state,
                "transactionId",
                authorizationRequestResponseDto,
                System.currentTimeMillis() + 100000
        );

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authorizationRequestCreateResponse));

        doReturn(ResponseEntity.ok(new java.util.HashMap<>()))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").doesNotExist());

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testProcessVPSubmission_SameDevice_GeneratesCodes() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        AuthorizationRequestResponseDto authorizationRequestResponseDto = new AuthorizationRequestResponseDto(
                "clientId",
                "presentationDefinitionUri",
                null,
                "nonce",
                "responseUri",
                false,
                true
        );

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(
                state,
                "transactionId",
                authorizationRequestResponseDto,
                System.currentTimeMillis() + 100000
        );

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);
        when(authorizationRequestCreateResponseRepository.findById(state)).thenReturn(Optional.of(authorizationRequestCreateResponse));

        java.util.Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("redirect_uri", redirectUri + "?response_code=generated-code-456");
        doReturn(ResponseEntity.ok(responseBody))
                .when(verifiablePresentationSubmissionService).submit(vpToken, presentationSubmission, state, null, null);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists())
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString(redirectUri.split("\\?")[0])))
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString("response_code=")));

        verify(verifiablePresentationSubmissionService, times(1)).submit(vpToken, presentationSubmission, state, null, null);
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }
}
