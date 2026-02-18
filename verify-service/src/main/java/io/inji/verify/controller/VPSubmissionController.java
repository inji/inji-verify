package io.inji.verify.controller;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.HashMap;
import com.nimbusds.jose.shaded.gson.JsonSyntaxException;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPSubmissionException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.nimbusds.jose.shaded.gson.Gson;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(path = Constants.RESPONSE_SUBMISSION_URI_ROOT)
@Slf4j
public class VPSubmissionController {

    @Value("${inji.verify.redirect-uri:#{null}}")
    String redirectUri;

    @Value("${inji.verify.responce-code-expiry-time:#{5}}")
    int responseCodeExpiryTime;

    @Value("${inji.verify.validate-response-code-with-time:#{true}}")
    boolean validateResponseCodeWithTime;

    final VerifiablePresentationRequestService verifiablePresentationRequestService;

    final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;

    final Gson gson;

    final AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository;

    public VPSubmissionController(VerifiablePresentationRequestService verifiablePresentationRequestService, VerifiablePresentationSubmissionService verifiablePresentationSubmissionService, Gson gson, AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository) {
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
        this.gson = gson;
        this.authorizationRequestCreateResponseRepository = authorizationRequestCreateResponseRepository;
    }

    @PostMapping(path = Constants.RESPONSE_SUBMISSION_URI, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> submitVP(
            @RequestParam(value = "vp_token", required = false) String vpToken,
            @RequestParam(value = "presentation_submission", required = false) String presentationSubmission,
            @NotNull @NotBlank @RequestParam(value = "state") String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        // --- 1. Initial response validation ---
        if (!isValidResponse(vpToken, error, presentationSubmission)) {
            String invalidResponseMessage = "Invalid response: either vp_token and presentation_submission must be provided, or error must be provided.";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(invalidResponseMessage);
        }

        // --- 2. Check if request present of not ---
        VPRequestStatusDto currentVPRequestStatusDto = verifiablePresentationRequestService.getCurrentRequestStatus(state);
        if (currentVPRequestStatusDto == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // --- 3. Check if error present ---
        if (error != null) {
            processVPSubmission(null, state, null, error, errorDescription, null, null);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            // --- 4. Presentation Submission Validation ---
            try {
                PresentationSubmissionDto presentationSubmissionDto = gson.fromJson(presentationSubmission, PresentationSubmissionDto.class);
                Set<ConstraintViolation<PresentationSubmissionDto>> violations = Validation.buildDefaultValidatorFactory().getValidator().validate(presentationSubmissionDto);
                if (!violations.isEmpty()) {
                    String violationMessage = violations.iterator().next().getMessage();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(violationMessage);
                }

                AuthorizationRequestCreateResponse authorizationRequestCreateResponse = authorizationRequestCreateResponseRepository.findById(state).orElse(null);
                Map<String, Object> response = new HashMap<>();
                if (authorizationRequestCreateResponse != null) {
                    String presentationFlow = authorizationRequestCreateResponse.getAuthorizationDetails().getPresentationFlow();
                    String responseCode = null;
                    Timestamp expiryAt = null;
                    if (StringUtils.hasText(redirectUri) && Objects.equals(presentationFlow, "same_device")) {
                        responseCode = UUID.randomUUID().toString();
                        expiryAt = Timestamp.from(Instant.now().plus(responseCodeExpiryTime, ChronoUnit.MINUTES));
                        String updatedRedirectUri = getUpdatedRedirectUri(responseCode);
                        response.put("redirect_uri", updatedRedirectUri);
                    }
                    processVPSubmission(vpToken, state, presentationSubmissionDto, null, null, responseCode, expiryAt);
                } else {
                    processVPSubmission(vpToken, state, presentationSubmissionDto, null, null, null, null);
                }
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } catch (JsonSyntaxException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_PRESENTATION_SUBMISSION");
            } catch (VPSubmissionException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.VP_SUBMISSION_EXCEPTION));
            }
        }
    }

    private String getUpdatedRedirectUri(String responseCode) {
        return UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("response_code", responseCode)
                .build()
                .toUriString();
    }

    private void processVPSubmission(String vpToken, String state, PresentationSubmissionDto presentationSubmissionDto, String error, String errorDescription, String responseCode, Timestamp responseCodeExpiryAt) {
        if (validateResponseCodeWithTime && responseCode != null && responseCodeExpiryAt == null) {
            throw new VPSubmissionException();
        }
        VPSubmissionDto vpSubmissionDto = new VPSubmissionDto(vpToken, presentationSubmissionDto, state, error, errorDescription, responseCode, responseCodeExpiryAt, false);
        verifiablePresentationSubmissionService.submit(vpSubmissionDto);
    }

    private boolean isValidResponse(String vpToken, String error, String presentationSubmission) {
        boolean hasVpToken = StringUtils.hasText(vpToken);
        boolean hasSubmission = StringUtils.hasText(presentationSubmission);
        boolean hasError = StringUtils.hasText(error);

        boolean hasValidVpBlock = hasVpToken && hasSubmission && !hasError;

        boolean hasValidErrorBlock = hasError && !hasVpToken && !hasSubmission;

        return hasValidVpBlock || hasValidErrorBlock;
    }
}