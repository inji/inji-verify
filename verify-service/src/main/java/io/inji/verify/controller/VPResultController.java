package io.inji.verify.controller;

import java.util.Optional;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import static io.inji.verify.utils.Utils.getResponseEntityForCredentialStatusException;

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
    public ResponseEntity<Object> getVPResult(@PathVariable String transactionId, HttpServletRequest request) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);

        if (!requestIds.isEmpty()) {
            try {
                log.info("Fetching VP result for transactionId: {}", transactionId);
                VPTokenResultDto result = verifiablePresentationSubmissionService.getVPResult(requestIds, transactionId);
                return ResponseEntity.status(HttpStatus.OK).body(result);
            } catch (VPSubmissionNotFoundException e) {
                log.error(e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_VP_SUBMISSION));
            } catch (VPWithoutProofException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.VP_WITHOUT_PROOF));
            } catch (VPSubmissionWalletError e) {
                log.error("Received wallet error for transactionId: {} - {} - {}", e.getErrorCode(), e.getErrorDescription(), transactionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(e.getErrorCode(), e.getErrorDescription()));
            } catch (CredentialStatusCheckException ex) {
                return getResponseEntityForCredentialStatusException(ex, request);
            }
        } else {
            try {
            return Optional.ofNullable(vcSubmissionService.getVcWithVerification(transactionId))
                    .map(vc -> ResponseEntity.status(HttpStatus.OK).body((Object) vc))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.INVALID_TRANSACTION_ID)));
            } catch (CredentialStatusCheckException ex) {
                return getResponseEntityForCredentialStatusException(ex, request);
            }
        }
    }

    @PostMapping(path = "/v2/vp-results/{transactionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getVPResultV2(@PathVariable String transactionId, @Valid @RequestBody VerificationRequestDto request) {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        log.info("Fetching VP result for requestId: {}", requestIds);
        try {
            VPVerificationResultDto resultDto = verifiablePresentationSubmissionService.getDetailVPResult(request, requestIds, transactionId);
            return ResponseEntity.status(HttpStatus.OK).body(resultDto);
        } catch (VPSubmissionNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_VP_SUBMISSION));
        } catch (VPWithoutProofException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.VP_WITHOUT_PROOF));
        } catch (VPSubmissionWalletError e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(e.getErrorCode(), e.getErrorDescription()));
        } catch (TokenMatchingFailedException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.TOKEN_MATCHING_FAILED));
        }
    }
}