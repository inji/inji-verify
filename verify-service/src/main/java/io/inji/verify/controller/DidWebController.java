package io.inji.verify.controller;

import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.DidGenerationException;
import io.inji.verify.services.DidService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves this verifier's did:web identity document. DID document generation itself — key
 * extraction and building the document per the DID spec — lives in {@link DidService}, so any
 * caller (this controller, or a consumer embedding that service directly to publish its own
 * did:web identity) gets identical behavior.
 */
@RequestMapping
@RestController
@Slf4j
public class DidWebController {

    final DidService didService;

    public DidWebController(DidService didService) {
        this.didService = didService;
    }

    @GetMapping(path = "/did.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> generateDid() {
        return ResponseEntity.status(HttpStatus.OK).body(didService.generateDidDocument());
    }

    @ExceptionHandler(DidGenerationException.class)
    public ResponseEntity<ErrorDto> handleDidGenerationException(DidGenerationException e) {
        log.error("Failed to generate DID document: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.DID_CREATION_FAILED));
    }
}
