package io.inji.verify.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.DcqlQueryMissingException;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.services.VerifiablePresentationRequestService;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static io.inji.verify.shared.Constants.VP_REQUEST_URI;

@RestController
@Validated
@Slf4j
public class VPRequestController {

    @Value("${inji.verify.cookie-duration-in-minute:#{5}}")
    int cookieDurationInMinute;

    @Value("${inji.verify.cookie-secure-value:#{true}}")
    boolean cookieIsSecure;

    @Value("${inji.verify.cookie-path}")
    String cookiePath;

    @Value("${inji.verify.cookie-same-site}")
    String cookieSameSite;

    final VerifiablePresentationRequestService verifiablePresentationRequestService;
    private final ObjectMapper objectMapper;

    public VPRequestController(VerifiablePresentationRequestService verifiablePresentationRequestService, ObjectMapper objectMapper) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = VP_REQUEST_URI, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createVPRequest(@RequestBody JsonNode requestBody) {
        VPRequestCreateDto vpRequestCreate;
        try {
            vpRequestCreate = objectMapper.treeToValue(requestBody, VPRequestCreateDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid request body format for VP request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto(ErrorCode.INVALID_REQUEST_FORMAT));
        }
        return processCreateVPRequest(requestBody, vpRequestCreate, false);
    }

    @PostMapping(path = "/v2/vp-session-request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createVPSessionRequest(@RequestBody JsonNode requestBody) {
        VPRequestCreateDto vpRequestCreate;
        try {
            vpRequestCreate = objectMapper.treeToValue(requestBody, VPRequestCreateDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid request body format for VP session request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto(ErrorCode.INVALID_REQUEST_FORMAT));
        }
        return processCreateVPRequest(requestBody, vpRequestCreate, true);
    }

    @NotNull
    private ResponseEntity<Object> processCreateVPRequest(JsonNode requestBody, VPRequestCreateDto vpRequestCreate, boolean createCookie) {
        ErrorCode validationError = validateVpRequest(requestBody, vpRequestCreate);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(validationError));
        }
        try {
            VPRequestResponseDto authorizationRequestResponse = verifiablePresentationRequestService.createAuthorizationRequest(vpRequestCreate);

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
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDto(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping(path = "/vp-request/{requestId}/status")
    public DeferredResult<VPRequestStatusDto> getStatus(@PathVariable String requestId) {
        return verifiablePresentationRequestService.getStatus(requestId);
    }

    @GetMapping(path = "/v2/vp-request/{requestId}" , produces = "application/oauth-authz-req+jwt")
    public ResponseEntity<Object> getVPRequest(@PathVariable String requestId) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(verifiablePresentationRequestService.getVPRequestJwt(requestId));
        } catch (VPRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_AUTH_REQUEST));
        } catch (DcqlQueryMissingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.DCQL_QUERY_REQUIRED));
        }
    }

    private ErrorCode validateVpRequest(JsonNode requestBody, VPRequestCreateDto vpRequestCreate) {
        if (vpRequestCreate.getClientId() == null || vpRequestCreate.getClientId().isBlank()) {
            return ErrorCode.CLIENT_ID_REQUIRED;
        }

        boolean hasLegacyKeys = hasLegacyPresentationDefinitionKeys(requestBody);
        boolean hasDcqlQueryInBody = requestBody != null && requestBody.hasNonNull("dcqlQuery");
        boolean hasDcqlInDto = vpRequestCreate.getDcqlQuery() != null && !vpRequestCreate.getDcqlQuery().isNull();
        boolean hasDcqlQuery = hasDcqlQueryInBody || hasDcqlInDto;

        if (hasDcqlQuery && hasLegacyKeys) {
            return ErrorCode.AMBIGUOUS_QUERY;
        }

        if (hasLegacyKeys) {
            return ErrorCode.PRESENTATION_DEFINITION_NOT_SUPPORTED;
        }

        if (!hasDcqlQuery) {
            return ErrorCode.DCQL_QUERY_REQUIRED;
        }

        return validateDcqlQuery(vpRequestCreate.getDcqlQuery());
    }

    private boolean hasLegacyPresentationDefinitionKeys(JsonNode requestBody) {
        if (requestBody == null || !requestBody.isObject()) {
            return false;
        }

        return hasNonNullOrTextual(requestBody, "presentationDefinition")
                || hasNonNullOrTextual(requestBody, "presentationDefinitionId")
                || hasNonNullOrTextual(requestBody, "presentation_definition")
                || hasNonNullOrTextual(requestBody, "presentation_definition_uri")
                || hasNonNullOrTextual(requestBody, "presentation_definition_id");
    }

    /** True if the property exists and is not JSON null (string/object/array all count as legacy payload). */
    private static boolean hasNonNullOrTextual(JsonNode requestBody, String fieldName) {
        if (!requestBody.has(fieldName) || requestBody.get(fieldName).isNull()) {
            return false;
        }
        JsonNode n = requestBody.get(fieldName);
        return !n.isMissingNode();
    }

    private ErrorCode validateDcqlQuery(JsonNode dcqlQuery) {
        if (dcqlQuery == null || dcqlQuery.isNull()) {
            return ErrorCode.DCQL_QUERY_REQUIRED;
        }
        if (!dcqlQuery.isObject()) {
            return ErrorCode.DCQL_VALIDATION_ERROR;
        }

        JsonNode credentials = dcqlQuery.get("credentials");
        if (credentials == null) {
            return ErrorCode.DCQL_CREDENTIALS_REQUIRED;
        }
        if (!credentials.isArray() || credentials.isEmpty()) {
            return ErrorCode.DCQL_CREDENTIALS_INVALID;
        }

        for (JsonNode credential : credentials) {
            JsonNode id = credential.get("id");
            if (id == null || !id.isTextual() || id.asText().isBlank()) {
                return ErrorCode.DCQL_CREDENTIAL_ID_REQUIRED;
            }

            JsonNode format = credential.get("format");
            if (format == null || !format.isTextual() || format.asText().isBlank()) {
                return ErrorCode.DCQL_CREDENTIAL_FORMAT_REQUIRED;
            }
            if (!"dc+sd-jwt".equalsIgnoreCase(format.asText().trim())) {
                return ErrorCode.DCQL_CREDENTIAL_FORMAT_UNSUPPORTED;
            }

            JsonNode claims = credential.get("claims");
            if (claims != null) {
                if (!claims.isArray()) {
                    return ErrorCode.INVALID_CLAIMS_STRUCTURE;
                }
                for (JsonNode claim : claims) {
                    JsonNode path = claim.get("path");
                    if (path == null || !path.isArray() || path.isEmpty()) {
                        return ErrorCode.INVALID_CLAIMS_STRUCTURE;
                    }
                    for (JsonNode pathElement : path) {
                        if (!pathElement.isTextual() || pathElement.asText().isBlank()) {
                            return ErrorCode.INVALID_CLAIMS_STRUCTURE;
                        }
                    }
                }
            }
        }

        return null;
    }
}