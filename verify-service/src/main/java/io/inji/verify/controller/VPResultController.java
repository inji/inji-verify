package io.inji.verify.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.ResponseCodeException;
import io.inji.verify.exception.MalformedCookieException;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import java.util.List;
import static io.inji.verify.shared.Constants.COOKIE_NAME;

@RestController
@Slf4j
public class VPResultController {
    final VerifiablePresentationRequestService verifiablePresentationRequestService;
    final VCSubmissionService vcSubmissionService;

    final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;

    @Value("${inji.verify.cookie-secure-value}")
    boolean cookieIsSecure;

    @Value("${inji.verify.cookie-path}")
    String cookiePath;

    @Value("${inji.verify.cookie-same-site}")
    String cookieSameSite;

    public VPResultController(VerifiablePresentationRequestService verifiablePresentationRequestService, VCSubmissionService vcSubmissionService, VerifiablePresentationSubmissionService verifiablePresentationSubmissionService) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.vcSubmissionService = vcSubmissionService;
        this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
    }

    @GetMapping(path = "/vp-result/{transactionId}")
    public ResponseEntity<Object> getVPResult(@PathVariable String transactionId) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) {
            return Optional.ofNullable(vcSubmissionService.getVcWithVerification(transactionId))
                    .map(vc -> ResponseEntity.status(HttpStatus.OK).body((Object) vc))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID)));
        }

        log.info("Fetching VP result for transactionId: {}", transactionId);
        VPTokenResultDto result = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
        ResponseCookie deleteCookie = getDeleteCookie();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(result);
    }

    @PostMapping(path = "/v2/vp-results/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultV2(@PathVariable String transactionId, @Valid @RequestBody VerificationRequestDto request) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));

        log.info("Fetching VP result for requestId: {}", requestIds);
        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);
        ResponseCookie deleteCookie = getDeleteCookie();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(result);
    }

    @PostMapping(path = "/vp-results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultUsingResponseCode(@Valid @RequestBody VerificationRequestDto request, @RequestParam(required = false, name = "response_code") String responseCode, @CookieValue(value = COOKIE_NAME, defaultValue = "") String cookie) {
        if (cookie.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.SESSION_INTERRUPTED));
        String transactionId;
        try {
            byte[] decodedCookie = Base64.getDecoder().decode(cookie);
            transactionId = new String(decodedCookie, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new MalformedCookieException(e);
        }
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));

        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultUsingResponse(request, requestIds, transactionId, responseCode);
        ResponseCookie deleteCookie = getDeleteCookie();
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(result);
    }

    private ResponseCookie getDeleteCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieIsSecure)
                .path(cookiePath)
                .sameSite(cookieSameSite)
                .maxAge(0)
                .build();
    }

    @ExceptionHandler(VPSubmissionNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(VPSubmissionNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_VP_SUBMISSION));
    }

    @ExceptionHandler(VPWithoutProofException.class)
    public ResponseEntity<ErrorDto> handleInternalError(VPWithoutProofException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.VP_WITHOUT_PROOF));
    }

    @ExceptionHandler(VPSubmissionWalletError.class)
    public ResponseEntity<ErrorDto> handleWalletError(VPSubmissionWalletError e) {
        log.error("Received wallet error: {} - {} - ", e.getErrorCode(), e.getErrorDescription());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(e.getErrorCode(), e.getErrorDescription()));
    }

    @ExceptionHandler(TokenMatchingFailedException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(TokenMatchingFailedException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.TOKEN_MATCHING_FAILED));
    }

    @ExceptionHandler(InvalidVpTokenException.class)
    public ResponseEntity<ErrorDto> invalidVpToken(InvalidVpTokenException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.INVALID_VP_TOKEN));
    }

    @ExceptionHandler(ResponseCodeException.class)
    public ResponseEntity<ErrorDto> handleResponseCodeException(ResponseCodeException e) {
        log.error("Response Code Error: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(errorCode.name(), errorCode.getErrorMessage()));
    }

    @ExceptionHandler(MalformedCookieException.class)
    public ResponseEntity<ErrorDto> handleMalformedCookieException(MalformedCookieException e) {
        log.warn("Invalid argument or malformed Base64: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.MALFORMED_COOKIE));
    }
}