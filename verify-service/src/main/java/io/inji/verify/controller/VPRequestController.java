package io.inji.verify.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.utils.SubmissionOriginExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static io.inji.verify.shared.Constants.VP_REQUEST_URI;

@RestController
@Slf4j
public class VPRequestController {

    final VerifiablePresentationRequestService verifiablePresentationRequestService;
    @Value("${inji.verify.cookie-duration-in-minute:#{5}}")
    int cookieDurationInMinute;
    @Value("${inji.verify.cookie-secure-value:#{true}}")
    boolean cookieIsSecure;
    @Value("${inji.verify.cookie-path}")
    String cookiePath;
    @Value("${inji.verify.cookie-same-site}")
    String cookieSameSite;

    public VPRequestController(VerifiablePresentationRequestService verifiablePresentationRequestService) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
    }

    @Operation(summary = "Create a new VP request based on the provided parameters. Validates the DCQL query and returns the created request details.")
    @PostMapping(path = VP_REQUEST_URI, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "VP request created successfully. The response body contains the details of the created request, including the transaction ID and other relevant information.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VPRequestResponseDto.class)
                    )
            )
    })
    public ResponseEntity<Object> createVPRequest(
            @Parameter(description = "The parameters for creating a VP request, including the DCQL query and other relevant details.")
            @Valid @RequestBody VPRequestCreateDto vpRequestCreate,
            HttpServletRequest httpRequest) {
        return processCreateVPRequest(vpRequestCreate, httpRequest, false);
    }

    @Operation(summary = "Create a new VP session request with cookie management. Validates the DCQL query, creates the request, and sets a cookie for session tracking.")
    @PostMapping(path = "/v2/vp-session-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "VP session request created successfully. The response body contains the details of the created request, including the transaction ID and other relevant information. A cookie is set for session tracking.",
                    content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = VPRequestResponseDto.class)
                )
            )
    })
    public ResponseEntity<Object> createVPSessionRequest(
            @Parameter(description = "The parameters for creating a VP session request, including the DCQL query and other relevant details. A cookie will be set for session tracking.")
            @Valid @RequestBody VPRequestCreateDto vpRequestCreate,
            HttpServletRequest httpRequest) {
        return processCreateVPRequest(vpRequestCreate, httpRequest, true);
    }

    @Operation(summary = "Get the status of a VP request by its ID. Returns the current status of the request, including any relevant details.")
    @GetMapping(path = "/vp-request/{requestId}/status")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP request status retrieved successfully. The response body contains the current status of the VP request."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "VP request not found. No VP request exists with the provided request ID."
            )
    })
    public DeferredResult<VPRequestStatusDto> getStatus(
            @Parameter(description = "The unique identifier of the VP request for which to retrieve the status.")
            @PathVariable String requestId) {
        return verifiablePresentationRequestService.getStatus(requestId);
    }

    @Operation(summary = "Get a VP request by its ID. Returns the VP request details as a JWT.")
    @GetMapping(path = "/v2/vp-request/{requestId}", produces = "application/oauth-authz-req+jwt")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "VP request retrieved successfully. The response body contains the VP request details as a JWT."
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "VP request not found. No VP request exists with the provided request ID."
            )
    })
    public ResponseEntity<Object> getVPRequest(
            @Parameter(description = "The unique identifier of the VP request to retrieve.")
            @PathVariable String requestId) throws VPRequestNotFoundException {
        return ResponseEntity.status(HttpStatus.OK).body(verifiablePresentationRequestService.getVPRequestJwt(requestId));
    }

    @ExceptionHandler(VPRequestValidationException.class)
    public ResponseEntity<ErrorDto> handleVPRequestValidationException(VPRequestValidationException e) {
        log.error("VP request validation error: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDto(errorCode.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(VPRequestNotFoundException.class)
    public ResponseEntity<ErrorDto> handleVPRequestNotFoundException(VPRequestNotFoundException e) {
        log.error("VP request not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_AUTH_REQUEST));
    }

    private ResponseEntity<Object> processCreateVPRequest(
            VPRequestCreateDto vpRequestCreate, HttpServletRequest httpRequest, boolean createCookie) {
        VPRequestResponseDto authorizationRequestResponse =
                verifiablePresentationRequestService.createAuthorizationRequest(
                        vpRequestCreate, SubmissionOriginExtractor.from(httpRequest));

        if (createCookie) {
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
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(authorizationRequestResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleJsonErrors(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException upe) {
            return ResponseEntity.badRequest()
                    .body(new ErrorDto(
                            "UNKNOWN_FIELD",
                            "Unknown field: " + upe.getPropertyName()
                    ));
        }
        if (cause instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            return ResponseEntity.badRequest()
                    .body(new ErrorDto(
                            "INVALID_FIELD_VALUE",
                            String.format(
                                    "Invalid value '%s' for field '%s'",
                                    ife.getValue(),
                                    field
                            )
                    ));
        }
        if (cause instanceof MismatchedInputException mie) {
            String field = mie.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            if (field.contains("type_values")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorDto(
                                ErrorCode.DCQL_META_INVALID.getErrorCode(),
                                "type_values must be an array of arrays."
                        ));
            }
            if (field.contains("vct_values")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorDto(
                                ErrorCode.DCQL_META_INVALID.getErrorCode(),
                                "vct_values must be an array of strings."
                        ));
            }
            return ResponseEntity.badRequest()
                    .body(new ErrorDto(
                            "MALFORMED_REQUEST",
                            mie.getOriginalMessage()
                    ));
        }
        if (cause instanceof JsonParseException jpe) {
            return ResponseEntity.badRequest()
                    .body(new ErrorDto(
                            "INVALID_JSON",
                            jpe.getOriginalMessage()
                    ));
        }
        return ResponseEntity.badRequest()
                .body(new ErrorDto(
                        "INVALID_REQUEST",
                        cause.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleInvalidRequestBody(MethodArgumentNotValidException ex) {
        return handleVPRequestValidationException(VPRequestValidationException.from(ex));
    }
}