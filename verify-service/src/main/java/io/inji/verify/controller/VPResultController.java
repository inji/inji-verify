package io.inji.verify.controller;

import java.util.Optional;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
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
}