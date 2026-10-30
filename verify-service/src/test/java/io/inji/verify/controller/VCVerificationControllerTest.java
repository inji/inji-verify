package io.inji.verify.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.verification.VCVerificationStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.inji.verify.services.VCVerificationService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class VCVerificationControllerTest {

    private final VCVerificationService vcVerificationService = mock(VCVerificationService.class);

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        VCVerificationController vcVerificationController = new VCVerificationController(vcVerificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(vcVerificationController).build();
    }

    @Test
    void testVerifyValidVC() throws Exception {
        String validVC = "validVC";
        String contentType = "application/ldp+json";

        VCVerificationStatusDto expectedStatus = createStatus("SUCCESS");

        when(vcVerificationService.verify(validVC, contentType)).thenReturn(expectedStatus);

        String result = mockMvc.perform(post("/vc-verification")
                        .contentType(contentType)
                        .content(validVC))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.writeValueAsString(expectedStatus), result);

        verify(vcVerificationService, times(1)).verify(validVC, contentType);
    }

    @Test
    void testVerifyInvalidVC() throws Exception {
        String invalidVC = "invalidVC";
        String contentType = "application/ldp+json";
        VCVerificationStatusDto expectedStatus = createStatus("INVALID");

        when(vcVerificationService.verify(invalidVC, contentType)).thenReturn(expectedStatus);

        mockMvc.perform(post("/vc-verification")
                        .contentType(contentType)
                        .content(invalidVC))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(expectedStatus)));

        verify(vcVerificationService, times(1)).verify(invalidVC, contentType);
    }

    @Test
    void testVerifyEmptyBody() throws Exception {
        mockMvc.perform(post("/vc-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private VCVerificationStatusDto createStatus(String name) throws Exception {
        Class enumClass = Class.forName("io.mosip.vercred.vcverifier.data.VerificationStatus");
        Object status = Enum.valueOf(enumClass.asSubclass(Enum.class), name);
        Constructor<VCVerificationStatusDto> constructor =
                VCVerificationStatusDto.class.getDeclaredConstructor(enumClass);
        constructor.setAccessible(true);
        return constructor.newInstance(status);
    }
}
