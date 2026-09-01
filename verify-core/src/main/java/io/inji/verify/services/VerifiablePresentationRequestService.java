package io.inji.verify.services;

import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Optional;

public interface VerifiablePresentationRequestService {
    /**
     * Creates an authorization request. For DC API, pass the raw Origin/Referer from the web
     * layer so {@code expected_origins} can be derived; server-to-server callers may pass empty.
     */
    VPRequestResponseDto createAuthorizationRequest(VPRequestCreateDto vpRequestCreate, Optional<String> submissionOrigin);

    VPRequestStatusDto getCurrentRequestStatus(String requestId);

    List<String> getLatestRequestIdFor(String transactionId);

    AuthorizationRequestCreateResponse getLatestAuthorizationRequestFor(String transactionId);

    void invokeVpRequestStatusListener(@NotNull String state);

    /**
     * @return a DeferredResult that resolves with the current/eventual {@link VPRequestStatusDto},
     * or resolves as an error with {@link VPRequestNotFoundException} if no request exists for the
     * given requestId. The error is set as an exception (not an HTTP response) so callers embedding
     * this service directly get a plain exception rather than a web-layer type.
     */
    DeferredResult<VPRequestStatusDto> getStatus(@NotNull String requestId);

    String getVPRequestJwt(String requestId) throws VPRequestNotFoundException;
}
