package io.inji.verify.controller;

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
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public ResponseEntity<Object> getVPResult(@PathVariable String transactionId, @RequestParam(required = false, name = "response_code") String responseCode) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) {
            return Optional.ofNullable(vcSubmissionService.getVcWithVerification(transactionId))
                    .map(vc -> ResponseEntity.status(HttpStatus.OK).body((Object) vc))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID)));
        }

        log.info("Fetching VP result for transactionId: {}", transactionId);
        VPTokenResultDto result = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId, responseCode);
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "/v2/vp-results/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultV2(@PathVariable String transactionId, @Valid @RequestBody VerificationRequestDto request, @RequestParam(required = false, name = "response_code") String responseCode) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (requestIds.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID));

        log.info("Fetching VP result for requestId: {}", requestIds);
        VPVerificationResultDto result = verifiablePresentationSubmissionService.getVPResultV2(request, requestIds, transactionId, responseCode);
        return ResponseEntity.ok(result);
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
}