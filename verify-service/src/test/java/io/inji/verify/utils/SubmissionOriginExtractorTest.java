package io.inji.verify.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionOriginExtractorTest {

    @Test
    void prefersOriginHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://verify.example.com");
        request.addHeader("Referer", "https://other.example.com/page");
        assertEquals("https://verify.example.com", SubmissionOriginExtractor.from(request).orElseThrow());
    }

    @Test
    void fallsBackToReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://verify.example.com/path");
        assertEquals("https://verify.example.com/path", SubmissionOriginExtractor.from(request).orElseThrow());
    }

    @Test
    void emptyWhenMissingOrNullOrigin() {
        assertTrue(SubmissionOriginExtractor.from(new MockHttpServletRequest()).isEmpty());
        assertTrue(SubmissionOriginExtractor.from(null).isEmpty());
        MockHttpServletRequest nullOrigin = new MockHttpServletRequest();
        nullOrigin.addHeader("Origin", "null");
        assertTrue(SubmissionOriginExtractor.from(nullOrigin).isEmpty());
    }
}
