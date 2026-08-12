package io.inji.verify.controller;

import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.RedirectUriGenerationException;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * Controller to handle Verifiable Presentation (VP) submission requests.
 * This controller validates incoming VP submissions and processes them according to the VP request details.
 */
@RestController
@Slf4j
public class VPSubmissionController {

    private static final Set<String> ALLOWED_PARAMS = Set.of("vp_token", "state", "error", "error_description");

    final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;

    public VPSubmissionController(VerifiablePresentationSubmissionService verifiablePresentationSubmissionService) {
        this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
    }

    /**
     * Endpoint to handle VP submission via POST request. Validates the incoming
     * request
     *
     * @param vpToken          - The vp_token parameter from the request, which may
     *                         be null or empty
     * @param state            - The state parameter from the request, which is
     *                         required and must not be empty
     * @param error            - The error parameter from the request, which may be
     *                         null or empty
     * @param errorDescription - The error_description parameter from the request,
     *                         which may be null or empty
     * @param request          - The HttpServletRequest object containing the
     *                         request parameters to be validated
     * @return
     */
    @Operation(summary = "Submit Verifiable Presentation (VP) in response to a VP request. Validates the submission and processes it according to the VP request details.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP submission processed successfully. If a response code was generated, the response will include a redirect_uri for the client to be redirected to."
            )
    })
    @PostMapping(path = Constants.VP_RESPONSE_SUBMISSION_URI, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitVP(
            @Parameter(description = "The vp_token containing the Verifiable Presentation data. This parameter is optional but either this or the error parameter must be provided. If provided, it must be a valid JSON object with specific structure rules.")
            @RequestParam(value = "vp_token", required = false) String vpToken,
            @Parameter(description = "The state parameter from the request, which is required and must not be empty.")
            @RequestParam(value = "state", required = true) String state,
            @Parameter(description = "The error parameter from the request, which may be null or empty.")
            @RequestParam(value = "error", required = false) String error,
            @Parameter(description = "The error_description parameter from the request, which may be null or empty.")
            @RequestParam(value = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {

        // Log incoming request parameters
        log.debug("Received VP submission with state: {}, error: {}, error_description: {}", state, error,
                errorDescription);
        if (StringUtils.hasText(vpToken)) {
            log.debug("Received VP submission with vp_token length: {}", vpToken.length());
        }

        try {
            for (String key : request.getParameterMap().keySet()) {
                if (!ALLOWED_PARAMS.contains(key)) {
                    throw new VPRequestValidationException(ErrorCode.UNKNOWN_PARAMETER, "Invalid parameter: " + sanitizeParamName(key));
                }
            }

            Map<String, Object> response = verifiablePresentationSubmissionService.submitVerifiablePresentation(
                    vpToken, state, error, errorDescription);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (VPRequestValidationException e) {
            log.error("VP submission validation error: {}", e.getMessage());
            ErrorCode errorCode = e.getErrorCode();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto(errorCode.getErrorCode(), e.getMessage()));
        } catch (RedirectUriGenerationException e) {
            log.error("Failed to build redirect_uri: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDto(ErrorCode.REDIRECT_URI_NOT_FOUND));
        } catch (VPAlreadySubmittedException e) {
            log.debug("VP submission already exists for state {}: {}", state, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto(ErrorCode.VP_ALREADY_SUBMITTED));
        } catch (InvalidVpTokenException e) {
            log.error("Invalid VP token structure for state {}", state);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto("invalid_vp_token", "The vp_token structure is invalid: " + e.getMessage()));
        }
    }

    /**
     * Sanitizes an untrusted request parameter name before it is echoed back in an error
     * message: characters outside {@code A-Z}, {@code a-z}, {@code 0-9}, underscore, hyphen,
     * and period are replaced with underscores, and the result is truncated to 64 characters.
     */
    private static String sanitizeParamName(String key) {
        if (key == null) {
            return "";
        }
        String sanitized = key.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }
}
