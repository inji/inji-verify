package io.inji.verify.controller;

import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.*;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static io.inji.verify.shared.Constants.COOKIE_NAME;

@RestController
@Slf4j
public class VPResultController {

    final VerifiablePresentationRequestService verifiablePresentationRequestService;
    final VCSubmissionService vcSubmissionService;
    final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;
    @Value("${inji.verify.cookie-secure-value:#{true}}")
    boolean cookieIsSecure;
    @Value("${inji.verify.cookie-path}")
    String cookiePath;

    public VPResultController(VerifiablePresentationRequestService verifiablePresentationRequestService, VCSubmissionService vcSubmissionService, VerifiablePresentationSubmissionService verifiablePresentationSubmissionService) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.vcSubmissionService = vcSubmissionService;
        this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
    }

    @Operation(summary = "Fetch VP result for a given transactionId", description = "This endpoint is used to fetch the VP result for a given transactionId. It will first check if there is any VP request associated with the transactionId. If there is no VP request associated with the transactionId, it will check if there is any VC submission associated with the transactionId. If there is, it will return the VC submission result. If there is no VC submission associated with the transactionId, it will return 404 Not Found.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP result fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VPTokenResultDto.class)
                    )
            )
    })
    @GetMapping(path = "/vp-result/{transactionId}")
    public ResponseEntity<Object> getVPResult(
            @Parameter(description = "Unique identifier for the VP transaction, which is generated when the VP request is created. It is used to correlate the VP request and VP submission.", required = true)
            @PathVariable String transactionId) {
        log.info("Received request to fetch VP result for transactionId: {}", transactionId);
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        if (requestIds.isEmpty()) {
            log.info("No requestId found for transactionId: {}. Checking for VC submission.", transactionId);
            return Optional.ofNullable(vcSubmissionService.getVcWithVerification(transactionId))
                    .map(vc -> ResponseEntity.status(HttpStatus.OK).body((Object) vc))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID)));
        }
        VPTokenResultDto result = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "V2 version to fetch VP result for a given transactionId", description = "This endpoint is used to fetch the VP result for a given transactionId with request body. It will first check if there is any VP request associated with the transactionId. If there is no VP request associated with the transactionId, it will return 404 Not Found.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP result fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VPVerificationResultDto.class)
                    )
            )
    })
    @PostMapping(path = "/v2/vp-results/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultV2(
            @Parameter(description = "Unique identifier for the VP transaction, which is generated when the VP request is created. It is used to correlate the VP request and VP submission.", required = true)
            @PathVariable String transactionId,
            @Parameter(description = "Request body for fetching VP result, which may contain additional parameters for the verification process.", required = true)
            @Valid @RequestBody VerificationRequestDto request) {
        log.info("Received request to fetch VP result V2 for transactionId: {}", transactionId);

        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        // In case there is no requestId found for the transactionId, we will return 404 Not Found.
        if (requestIds.isEmpty()) {
            log.info("No requestId found for transactionId: {}. Returning 404 Not Found.", transactionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));
        }
        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Fetch VP session result for a given transactionId from cookie", description = "This endpoint is used to fetch the VP session result for a given transactionId from cookie. It will first check if there is any VP request associated with the transactionId. If there is no VP request associated with the transactionId, it will return 404 Not Found.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP session result fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VPVerificationResultDto.class)
                    )
            )
    })
    @PostMapping(path = "/vp-session-results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPSessionResults(
            @Parameter(description = "Request body for fetching VP session result, which may contain additional parameters for the verification process.", required = true)
            @Valid @RequestBody VerificationSessionRequestDto request,
            @Parameter(description = "Cookie containing the transactionId for the VP session. This cookie is set when the VP session request is created and is used to correlate the VP session request and VP session submission.", required = true)
            @CookieValue(value = COOKIE_NAME) String requestCookie, HttpServletResponse response) {
        log.info("Received request to fetch VP session result with transactionId from cookie");
        try {
            if (requestCookie == null || requestCookie.isEmpty())
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto(ErrorCode.VP_SESSION_INVALID));
            String transactionId = getTransactionId(requestCookie);
            List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
            // In case there is no requestId found for the transactionId, we will return 404 Not Found.
            if (requestIds.isEmpty()) {
                log.info("No requestId found for transactionId: {}. Returning 404 Not Found.", transactionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));
            }
            VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPSessionResults(request, requestIds, transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } finally {
            log.info("Cleaning up VP session cookie");
            Cookie responseCookie = new Cookie(COOKIE_NAME, "");
            responseCookie.setHttpOnly(true);
            responseCookie.setSecure(cookieIsSecure);
            responseCookie.setMaxAge(0);
            responseCookie.setPath(cookiePath);
            response.addCookie(responseCookie);
        }
    }

    private String getTransactionId(String cookie) {
        try {
            byte[] decodedCookie = Base64.getDecoder().decode(cookie);
            return new String(decodedCookie, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode cookie value: {}", e.getMessage());
            throw new MalformedCookieException(e);
        }
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

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorDto> handleMissingCookie(MissingRequestCookieException e) {
        log.warn("Required cookie is missing: {}", e.getCookieName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorDto(ErrorCode.VP_SESSION_INVALID));
    }

    @ExceptionHandler(VPVerificationException.class)
    public ResponseEntity<ErrorDto> handleVPVerificationException(VPVerificationException e) {
        log.warn("VP verification failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.VP_VERIFICATION_FAILED));
    }
}