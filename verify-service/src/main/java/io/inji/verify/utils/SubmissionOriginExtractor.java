package io.inji.verify.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Extracts a raw Origin / Referer value from the servlet request for DC API flows.
 * Canonicalization happens in verify-core ({@code OriginAudienceResolver.canonicalize}).
 */
public final class SubmissionOriginExtractor {

    private SubmissionOriginExtractor() {
    }

    /**
     * Prefer the {@code Origin} header; fall back to {@code Referer} when Origin is absent.
     * Returns empty if neither yields a usable value (fails closed upstream).
     */
    public static Optional<String> from(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        String originHeader = request.getHeader("Origin");
        if (StringUtils.hasText(originHeader) && !"null".equalsIgnoreCase(originHeader.trim())) {
            return Optional.of(originHeader.trim());
        }
        String referer = request.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            return Optional.of(referer.trim());
        }
        return Optional.empty();
    }
}
