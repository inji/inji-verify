package io.inji.verify.controller;

import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.InvalidCredentialException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.services.VCVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class VCVerificationController {
    final VCVerificationService VCVerificationService;

    public VCVerificationController(VCVerificationService vcVerificationService) {
        this.VCVerificationService = vcVerificationService;
    }

    @PostMapping(path = "/vc-verification")
    public VCVerificationStatusDto verify(@RequestBody String vc, @RequestHeader("Content-Type") String contentType) throws CredentialStatusCheckException {
        return VCVerificationService.verify(vc, contentType);
    }

    @PostMapping(path = "/v2/vc-verification", consumes = MediaType.APPLICATION_JSON_VALUE)
    public VCVerificationResultDto verifyV2(@Valid @RequestBody VCVerificationRequestDto request) {
        // Plain VC verification endpoint — no VP session context, KB-JWT validation not applicable.
        return VCVerificationService.verifyV2(request, false);
    }

    /**
     * A credential that cannot be decoded (corrupt mdoc CBOR, an mdoc with no documents, an
     * oversized mdoc, ...) is a 400 with the reason, so callers can tell the cases apart.
     * Every InvalidCredentialException message is a fixed string without credential data.
     */
    @ExceptionHandler(InvalidCredentialException.class)
    public ResponseEntity<ErrorDto> handleInvalidCredential(InvalidCredentialException e) {
        log.warn("Invalid credential: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDto(ErrorCode.INVALID_CREDENTIAL.getErrorCode(), e.getMessage()));
    }
}