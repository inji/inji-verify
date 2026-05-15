package io.inji.verify.services;

import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

public interface VerifiablePresentationRequestService {
    VPRequestResponseDto createAuthorizationRequest(VPRequestCreateDto vpRequestCreate);

    VPRequestStatusDto getCurrentRequestStatus(String requestId);

    List<String> getLatestRequestIdFor(String transactionId);

    AuthorizationRequestCreateResponse getLatestAuthorizationRequestFor(String transactionId);

    void invokeVpRequestStatusListener(@NotNull String state);

    DeferredResult<VPRequestStatusDto> getStatus(@NotNull String requestId);

    String getVPRequestJwt(String requestId) throws VPRequestNotFoundException;
}
