package io.inji.verify.controller;

import io.inji.verify.exception.DidGenerationException;
import io.inji.verify.services.DidService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DID document generation itself is now covered by DidServiceImplTest; this class only checks
 * that the controller delegates to DidService and translates DidGenerationException correctly.
 */
@WebMvcTest(DidWebController.class)
class DidWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DidService didService;

    private final String testVerifyURI = "did:example:test-issuer";
    private final String testVerifyPublicKeyURI = "did:example:test-issuer#key-0";

    @Test
    @DisplayName("Should return DID Document for /did.json with application/did+json")
    void wellKnown_Success() throws Exception {
        Map<String, Object> expectedDidDocument = new HashMap<>();
        expectedDidDocument.put("@context", Collections.singletonList("https://www.w3.org/ns/did/v1"));
        expectedDidDocument.put("id", testVerifyURI);
        Map<String, Object> verificationMethod = new HashMap<>();
        verificationMethod.put("id", testVerifyPublicKeyURI);
        verificationMethod.put("type", "Ed25519VerificationKey2020");
        expectedDidDocument.put("verificationMethod", Collections.singletonList(verificationMethod));

        when(didService.generateDidDocument()).thenReturn(expectedDidDocument);

        mockMvc.perform(get("/did.json")
                        .accept(MediaType.valueOf("application/json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(testVerifyURI))
                .andExpect(jsonPath("$['@context'][0]").value("https://www.w3.org/ns/did/v1"))
                .andExpect(jsonPath("$.verificationMethod[0].id").value(testVerifyPublicKeyURI));

        verify(didService, times(1)).generateDidDocument();
    }

    @Test
    @DisplayName("Should return error")
    void wellKnown_Error() throws Exception {
        when(didService.generateDidDocument()).thenThrow(new DidGenerationException());

        mockMvc.perform(get("/did.json"))
                .andExpect(status().isInternalServerError());
    }
}
