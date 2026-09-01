package io.inji.verify.utils;

import io.inji.verify.enums.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OriginAudienceResolverTest {

    @Test
    void toOriginAudience_prefixesOriginWithoutTrailingSlash() {
        assertEquals("origin:https://verify.example.com",
                OriginAudienceResolver.toOriginAudience("https://verify.example.com"));
    }

    @Test
    void canonicalize_stripsPathAndNormalizes() {
        assertEquals("https://verify.example.com",
                OriginAudienceResolver.canonicalize("https://verify.example.com/path?q=1").orElseThrow());
        assertEquals("https://verify.example.com:8443",
                OriginAudienceResolver.canonicalize("https://verify.example.com:8443/").orElseThrow());
        assertTrue(OriginAudienceResolver.canonicalize("ftp://verify.example.com").isEmpty());
        assertTrue(OriginAudienceResolver.canonicalize(null).isEmpty());
        assertTrue(OriginAudienceResolver.canonicalize("not-a-uri").isEmpty());
    }

    @Test
    void audienceMatches_acceptsLegacyTrailingSlash() {
        assertTrue(OriginAudienceResolver.audienceMatches(
                "origin:https://verify.example.com",
                "origin:https://verify.example.com/"));
        assertTrue(OriginAudienceResolver.audienceMatches(
                "origin:https://verify.example.com/",
                "origin:https://verify.example.com"));
        assertFalse(OriginAudienceResolver.audienceMatches(
                "origin:https://verify.example.com",
                "origin:https://other.example.com"));
    }

    @Test
    void resolveOriginAudience_returnsOriginAudience_whenOriginInExpectedOrigins() {
        OriginAudienceResolver.ResolveResult result = OriginAudienceResolver.resolveOriginAudience(
                List.of("https://verify.example.com"), Optional.of("https://verify.example.com"));

        assertTrue(result.isOk());
        assertEquals("origin:https://verify.example.com", result.expectedAudience());
    }

    @Test
    void resolveOriginAudience_fails_whenOriginMissing() {
        OriginAudienceResolver.ResolveResult result = OriginAudienceResolver.resolveOriginAudience(
                List.of("https://verify.example.com"), Optional.empty());

        assertFalse(result.isOk());
        assertEquals(ErrorCode.VERIFIER_ORIGIN_REQUIRED, result.error());
    }

    @Test
    void resolveOriginAudience_fails_whenOriginNotAllowed() {
        OriginAudienceResolver.ResolveResult result = OriginAudienceResolver.resolveOriginAudience(
                List.of("https://verify.example.com"), Optional.of("https://evil.example.com"));

        assertFalse(result.isOk());
        assertEquals(ErrorCode.SUBMISSION_ORIGIN_NOT_ALLOWED, result.error());
    }
}
