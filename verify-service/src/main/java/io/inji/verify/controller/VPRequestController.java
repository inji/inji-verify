package io.inji.verify.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.PresentationDefinitionNotFoundException;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.services.VerifiablePresentationRequestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static io.inji.verify.shared.Constants.VP_REQUEST_URI;

@RequestMapping(VP_REQUEST_URI)
@RestController
@Validated
@Slf4j
public class VPRequestController {

    @Value("${inji.verify.cookie-duration-in-minute:#{5}}")
    int cookieDurationInMinute;

    @Value("${inji.verify.cookie-secure-value:#{true}}")
    boolean cookieIsSecure;

    @Value("${inji.verify.cookie-path}")
    String cookiePath;

    @Value("${inji.verify.cookie-same-site}")
    String cookieSameSite;

    final VerifiablePresentationRequestService verifiablePresentationRequestService;

    public VPRequestController(VerifiablePresentationRequestService verifiablePresentationRequestService) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createVPRequest(@Valid @RequestBody VPRequestCreateDto vpRequestCreate) {
        if (vpRequestCreate.getPresentationDefinitionId() == null && vpRequestCreate.getPresentationDefinition() == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.BOTH_ID_AND_PD_CANNOT_BE_NULL));
        }
        try {
            VPRequestResponseDto authorizationRequestResponse = verifiablePresentationRequestService.createAuthorizationRequest(vpRequestCreate);

            String transactionId = authorizationRequestResponse.getTransactionId();
            String cookieValue = Base64.getEncoder().encodeToString(transactionId.getBytes(StandardCharsets.UTF_8));
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, cookieValue)
                    .httpOnly(true)
                    .secure(cookieIsSecure)
                    .path(cookiePath)
                    .sameSite(cookieSameSite)
                    .maxAge(Duration.ofMinutes(cookieDurationInMinute))
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(authorizationRequestResponse);
        } catch (PresentationDefinitionNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_PRESENTATION_DEFINITION));
        }
    }

    @GetMapping(path = "/{requestId}/status")
    public DeferredResult<VPRequestStatusDto> getStatus(@PathVariable String requestId) {
        return verifiablePresentationRequestService.getStatus(requestId);
    }

    @GetMapping(path = "/{requestId}" , produces = "application/oauth-authz-req+jwt")
    public ResponseEntity<Object> getVPRequest(@PathVariable String requestId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(verifiablePresentationRequestService.getVPRequestJwt(requestId));
        } catch (VPRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_AUTH_REQUEST));
        }
    }
}