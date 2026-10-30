package io.inji.verify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.submission.VCSubmissionDto;
import io.inji.verify.dto.submission.VCSubmissionResponseDto;
import io.inji.verify.services.VCSubmissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VCSubmissionController.class)
class VCSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VCSubmissionService vcSubmissionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should submit VC successfully and return 200 OK")
    void submitVC_Success() throws Exception {
        VCSubmissionDto requestDto = new VCSubmissionDto("eyJvcmlnaW5hbF92YyI6InNvbWUgc3RyaW5nIn0=",null);
        VCSubmissionResponseDto serviceResponse = new VCSubmissionResponseDto("txn_1" );

        when(vcSubmissionService.submitVC(any(VCSubmissionDto.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/vc-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        verify(vcSubmissionService, times(1)).submitVC(any(VCSubmissionDto.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for invalid request body")
    void submitVC_InvalidRequestBody_BadRequest() throws Exception {
        mockMvc.perform(post("/vc-submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(vcSubmissionService, never()).submitVC(any(VCSubmissionDto.class));
    }
}
