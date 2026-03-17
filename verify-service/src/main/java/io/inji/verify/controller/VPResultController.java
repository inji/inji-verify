package io.inji.verify.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.*;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.CookieValue;
import java.util.List;
import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static io.inji.verify.utils.Utils.setCookie;

@RestController
@Slf4j
public class VPResultController {

    final VerifiablePresentationRequestService verifiablePresentationRequestService;

    final VCSubmissionService vcSubmissionService;

    final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;

    public VPResultController(VerifiablePresentationRequestService verifiablePresentationRequestService, VCSubmissionService vcSubmissionService, VerifiablePresentationSubmissionService verifiablePresentationSubmissionService) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.vcSubmissionService = vcSubmissionService;
        this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
    }

    @GetMapping(path = "/vp-result/{transactionId}")
    public ResponseEntity<Object> getVPResult(@PathVariable String transactionId) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) {
            ResponseCookie deleteCookie = setCookie("", 0);
            return Optional.ofNullable(vcSubmissionService.getVcWithVerification(transactionId))
                    .map(vc -> ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.SET_COOKIE, deleteCookie.toString()).body((Object) vc))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).header(HttpHeaders.SET_COOKIE, deleteCookie.toString()).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID)));
        }

        log.info("Fetching VP result for transactionId: {}", transactionId);
        VPTokenResultDto result = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/v2/vp-results/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultV2(@PathVariable String transactionId, @Valid @RequestBody VerificationRequestDto request) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) {
            ResponseCookie deleteCookie = setCookie("", 0);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));
        }

        log.info("Fetching VP result for requestId: {}", requestIds);
        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/vp-session-results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPSessionResults(@Valid @RequestBody VerificationRequestDto request, @CookieValue(value = COOKIE_NAME, defaultValue = "") String cookie) {
        if (cookie.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto(ErrorCode.SESSION_INTERRUPTED));
        String transactionId = getTransactionId(cookie);
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) {
            ResponseCookie deleteCookie = setCookie("", 0);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                    .body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));
        }

        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId);
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(result);
    }

    private String getTransactionId(String cookie) {
        try {
            byte[] decodedCookie = Base64.getDecoder().decode(cookie);
            return new String(decodedCookie, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new MalformedCookieException(e);
        }
    }

    @ExceptionHandler(VPSubmissionNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(VPSubmissionNotFoundException e) {
        log.error(e.getMessage());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(ErrorCode.NO_VP_SUBMISSION));
    }

    @ExceptionHandler(VPWithoutProofException.class)
    public ResponseEntity<ErrorDto> handleInternalError(VPWithoutProofException e) {
        log.error(e.getMessage());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(ErrorCode.VP_WITHOUT_PROOF));
    }

    @ExceptionHandler(VPSubmissionWalletError.class)
    public ResponseEntity<ErrorDto> handleWalletError(VPSubmissionWalletError e) {
        log.error("Received wallet error: {} - {} - ", e.getErrorCode(), e.getErrorDescription());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(e.getErrorCode(), e.getErrorDescription()));
    }

    @ExceptionHandler(TokenMatchingFailedException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(TokenMatchingFailedException e) {
        log.error(e.getMessage());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(ErrorCode.TOKEN_MATCHING_FAILED));
    }

    @ExceptionHandler(InvalidVpTokenException.class)
    public ResponseEntity<ErrorDto> invalidVpToken(InvalidVpTokenException e) {
        log.error(e.getMessage());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(ErrorCode.INVALID_VP_TOKEN));
    }

    @ExceptionHandler(ResponseCodeException.class)
    public ResponseEntity<ErrorDto> handleResponseCodeException(ResponseCodeException e) {
        log.error("Response Code Error: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(errorCode.name(), errorCode.getErrorMessage()));
    }

    @ExceptionHandler(MalformedCookieException.class)
    public ResponseEntity<ErrorDto> handleMalformedCookieException(MalformedCookieException e) {
        log.warn("Invalid argument or malformed Base64: {}", e.getMessage());
        ResponseCookie deleteCookie = setCookie("", 0);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(new ErrorDto(ErrorCode.MALFORMED_COOKIE));
    }
}