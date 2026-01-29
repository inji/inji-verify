package io.inji.verify.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReflectedXssTest.DummyController.class)
@ContextConfiguration(classes = {ErrorMvcAutoConfiguration.class})
@org.springframework.context.annotation.Import(io.inji.verify.config.CustomErrorAttributes.class)
public class ReflectedXssTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @GetMapping("/dummy")
        String ok() {
            return "ok";
        }
    }

    @Test
    @DisplayName("Should not reflect path in 404 error response for unmatched endpoint")
    void shouldNotReflectPathIn404() throws Exception {
        String xssPayload = "/does-not-exist<script>alert(1)</script>";

        mockMvc.perform(get(xssPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").doesNotExist());
    }
}
