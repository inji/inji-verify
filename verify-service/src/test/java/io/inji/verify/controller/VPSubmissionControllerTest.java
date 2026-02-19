package io.inji.verify.controller;

import com.nimbusds.jose.shaded.gson.Gson;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.VPSubmissionException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
    public void setUp() throws IllegalAccessException, NoSuchFieldException {
        VPSubmissionController vpSubmissionController = new VPSubmissionController(verifiablePresentationRequestService, verifiablePresentationSubmissionService, gson, authorizationRequestCreateResponseRepository);

        java.lang.reflect.Field redirectUriField = vpSubmissionController.getClass().getDeclaredField("redirectUri");
        redirectUriField.setAccessible(true);
        redirectUriField.set(vpSubmissionController, redirectUri);

        java.lang.reflect.Field responseCodeExpiryTimeField = vpSubmissionController.getClass().getDeclaredField("responseCodeExpiryTimeInMins");
        responseCodeExpiryTimeField.setAccessible(true);
        responseCodeExpiryTimeField.set(vpSubmissionController, 5);

        java.lang.reflect.Field validateResponseCodeWithTimeField = vpSubmissionController.getClass().getDeclaredField("includeResponseCodeTimeChecks");
        validateResponseCodeWithTimeField.setAccessible(true);
        validateResponseCodeWithTimeField.set(vpSubmissionController, true);
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

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk());

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
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
                "same_device"
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

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists());

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }

    @Test
    public void testResponseWithEmptyBodyIfRedirectUriNotPresentOnSubmitVP_Success() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"testId\"}";
        String state = "testState";

        PresentationSubmissionDto presentationSubmissionDto = new PresentationSubmissionDto("id","dId",new ArrayList<>());

        VPSubmissionController vpSubmissionController = new VPSubmissionController(verifiablePresentationRequestService, verifiablePresentationSubmissionService, gson, authorizationRequestCreateResponseRepository);
        java.lang.reflect.Field field = vpSubmissionController.getClass().getDeclaredField("redirectUri");
        field.setAccessible(true);
        field.set(vpSubmissionController, null);
        mockMvc = MockMvcBuilders.standaloneSetup(vpSubmissionController).build();


        VPRequestStatusDto requestStatusDto = new VPRequestStatusDto(VPRequestStatus.ACTIVE);

        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(presentationSubmissionDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(requestStatusDto);

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").doesNotExist());

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
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

        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
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
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
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
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
    }

    @Test
    public void testSubmitVP_PresentationSubmissionFailsValidation() throws Exception {
        String vpToken = "testToken";
        String presentationSubmission = "{\"id\":\"\"}"; // id blank, should fail @NotBlank
        String state = "testState";
        PresentationSubmissionDto invalidDto = new PresentationSubmissionDto("", "", new ArrayList<>());
        when(gson.fromJson(presentationSubmission, PresentationSubmissionDto.class)).thenReturn(invalidDto);
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
    }

    @Test
    public void testSubmitVP_OnlyErrorPresent() throws Exception {
        String error = "some_error";
        String state = "testState";
        when(verifiablePresentationRequestService.getCurrentRequestStatus(state)).thenReturn(new VPRequestStatusDto(VPRequestStatus.ACTIVE));
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("error", error)
                        .param("state", state))
                .andExpect(status().isOk());
        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
    }

    @Test
    public void testSubmitVP_MissingState() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", "token"))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
    }

    @Test
    public void testSubmitVP_BlankState() throws Exception {
        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", "token")
                        .param("state", " "))
                .andExpect(status().isBadRequest());
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
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
        verify(verifiablePresentationSubmissionService, times(0)).submit(any(VPSubmissionDto.class));
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
                "same_device"
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

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists())
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString(redirectUri.split("\\?")[0])))
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString("response_code=")));

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
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
                "cross_device"
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

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").doesNotExist());

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
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
                "same_device"
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

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect_uri").exists())
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString(redirectUri.split("\\?")[0])))
                .andExpect(jsonPath("$.redirect_uri").value(org.hamcrest.Matchers.containsString("response_code=")));

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);

        ArgumentCaptor<VPSubmissionDto> vpSubmissionDto = ArgumentCaptor.forClass(VPSubmissionDto.class);
        verify(verifiablePresentationSubmissionService).submit(vpSubmissionDto.capture());

        VPSubmissionDto submissionDtoValue = vpSubmissionDto.getValue();

        assertNotNull(submissionDtoValue.getResponseCode());
        assertNotNull(submissionDtoValue.getResponseCodeExpiryAt());
        assertFalse(submissionDtoValue.getResponseCodeUsed());
        assertEquals("testState", submissionDtoValue.getState());
    }

    @Test
    public void testProcessVPSubmission_ThrowsExceptionOnMissingExpiry() throws Exception {
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
                "same_device"
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

        doThrow(new VPSubmissionException())
                .when(verifiablePresentationSubmissionService)
                .submit(any(VPSubmissionDto.class));

        mockMvc.perform(post(Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .param("vp_token", vpToken)
                        .param("presentation_submission", presentationSubmission)
                        .param("state", state))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VP_SUBMISSION_EXCEPTION"));

        verify(verifiablePresentationSubmissionService, times(1)).submit(any(VPSubmissionDto.class));
        verify(verifiablePresentationRequestService, times(1)).getCurrentRequestStatus(state);
    }
}
