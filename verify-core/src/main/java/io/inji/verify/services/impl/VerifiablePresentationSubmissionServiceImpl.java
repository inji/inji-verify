package io.inji.verify.services.impl;

import static io.inji.verify.utils.Utils.extractClaims;
import static io.inji.verify.utils.Utils.isSdJwt;
import static io.inji.verify.utils.Utils.populateAllChecksSuccessful;
import static io.inji.verify.utils.Utils.populateExpiryCheck;
import static io.inji.verify.utils.Utils.populateSchemaAndSignature;
import static io.inji.verify.utils.Utils.populateStatusCheckDtoList;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.result.*;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.exception.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.shaded.gson.Gson;

import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.submission.VCSubmissionVerificationStatusDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.KBJwtErrorCodes;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.repository.VPSubmissionRepository;
import io.inji.verify.services.VCSubmissionService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.OriginAudienceResolver;
import io.inji.verify.utils.Utils;
import io.inji.verify.validator.DcqlValidator;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.enums.VPRequestStatus;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.PresentationResultWithCredentialStatus;
import io.mosip.vercred.vcverifier.data.PresentationResultWithCredentialStatusV2;
import io.mosip.vercred.vcverifier.data.PresentationVerificationResultV2;
import io.mosip.vercred.vcverifier.data.VCResultV2;
import io.mosip.vercred.vcverifier.data.VCResultWithCredentialStatus;
import io.mosip.vercred.vcverifier.data.VCResultWithCredentialStatusV2;
import io.mosip.vercred.vcverifier.data.VPVerificationStatus;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VerifiablePresentationSubmissionServiceImpl implements VerifiablePresentationSubmissionService {

    @Value("${inji.verify.claims-with-meta-data}")
    List<String> claimsWithMetaData;

    @Value("${inji.verify.response-code-expiry-time-in-mins:#{5}}")
    int responseCodeExpiryTimeInMins;

    @Value("${inji.verify.redirect-uri}")
    String redirectUri;

    @Value("${inji.verify.kb-jwt-max-age-seconds:#{600}}")
    long kbJwtMaxAgeSeconds;

    final AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository;
    final VPSubmissionRepository vpSubmissionRepository;
    final CredentialsVerifier credentialsVerifier;
    final PresentationVerifier presentationVerifier;
    final VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService;
    final VCVerificationServiceImpl vcVerificationService;
    final PixelPass pixelPass;
    final Gson gson;
    final Validator validator;
    final ObjectMapper objectMapper;
    final DcqlValidator dcqlValidator;
    final VCSubmissionService vcSubmissionService;

    public VerifiablePresentationSubmissionServiceImpl(VPSubmissionRepository vpSubmissionRepository, CredentialsVerifier credentialsVerifier, PresentationVerifier presentationVerifier, VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService, VCVerificationServiceImpl vcVerificationService, PixelPass pixelPass, AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository, Gson gson, Validator validator, ObjectMapper objectMapper, DcqlValidator dcqlValidator, VCSubmissionService vcSubmissionService) {
        this.vpSubmissionRepository = vpSubmissionRepository;
        this.credentialsVerifier = credentialsVerifier;
        this.presentationVerifier = presentationVerifier;
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.vcVerificationService = vcVerificationService;
        this.pixelPass = pixelPass;
        this.authorizationRequestCreateResponseRepository = authorizationRequestCreateResponseRepository;
        this.gson = gson;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.dcqlValidator = dcqlValidator;
        this.vcSubmissionService = vcSubmissionService;
    }

    /**
     * This method retrieves the authorization request details from the database using the provided state parameter.
     * @param state
     * @return
     */
    AuthorizationRequestCreateResponse getAuthRequest(String state) {
		return authorizationRequestCreateResponseRepository.findById(state).orElse(null);
	}

    /**
     * Runs the full VP submission flow. This is the single entry point the controller (or a
     * consumer embedding this service directly) needs to call.
     * <p>
     * Behavior is driven by the stored authorization request {@code response_mode}, not by which
     * HTTP endpoint was used. Only DCQL / audience / nonce rejections are persisted (the wallet's
     * actual answer was evaluated). Earlier failures (params, state, structure, origin, malformed
     * token) throw without persisting so they do not burn the one-shot submission slot.
     * <p>
     * Not annotated {@code @Transactional}: each {@code save} commits on its own so the
     * status listener can observe the DB row.
     */
    @Override
    public Map<String, Object> submitVerifiablePresentation(
            String vpToken,
            String state,
            String error,
            String errorDescription,
            Optional<String> submissionOrigin)
            throws VPRequestValidationException, RedirectUriGenerationException, VPAlreadySubmittedException, InvalidVpTokenException {
        // --- 1. Validate request parameters ---
        validateSubmissionRequest(vpToken, error, errorDescription);

        // --- 2. Validate state parameter and retrieve current VP request status ---
        validateState(state);

        // --- 3. Validate vp_token structure if present ---
        if (StringUtils.hasText(vpToken)) {
            validateVpTokenStructure(vpToken);
        }
        log.debug("Request parameters validated successfully for state: {}", state);

        // ---- 4. Validate against the Authorization Request
        AuthorizationRequestCreateResponse authRequestCreateResponse = getAuthRequest(state);
        if (authRequestCreateResponse == null) {
            throw new VPRequestValidationException(ErrorCode.NO_MATCHING_VP_REQUEST);
        }
        log.debug("Authorization request resolved for state: {}", state);

        AuthorizationRequestResponseDto authDetails = authRequestCreateResponse.getAuthorizationDetails();
        boolean isResponseModeDcApi = Constants.RESPONSE_MODE_DC_API.equals(authDetails.getResponseMode());
        String originAudience = null;
        // DC API: submission Origin must be in expected_origins (including error-only responses).
        if (isResponseModeDcApi) {
            OriginAudienceResolver.ResolveResult origin = OriginAudienceResolver.resolveOriginAudience(
                    authDetails.getExpectedOrigins(), submissionOrigin);
            if (!origin.isOk()) {
                throw new VPRequestValidationException(origin.error());
            }
            originAudience = origin.expectedAudience();
        }

        // ---- 5. Validate against the DCQL if vp_token is present (persist on rejection)
        if (StringUtils.hasText(vpToken)) {
            try {
                validateVpTokenAgainstDcql(vpToken, authRequestCreateResponse);
            } catch (VPRequestValidationException ex) {
                saveRejectedSubmission(state, vpToken, ex);
                verifiablePresentationRequestService.invokeVpRequestStatusListener(state);
                throw ex;
            }
        }

        // ---- 6. Extract DCQL VP tokens from the vp_token string (malformed → unpersisted)
        DcqlTokensDto dcqlTokensDto = null;
        if (StringUtils.hasText(vpToken)) {
            dcqlTokensDto = extractDcqlTokens(vpToken, authDetails);
        }

        // ---- 7. Validate audience + nonce (persist on rejection)
        if (dcqlTokensDto != null) {
            try {
                validateAudienceAndNonce(dcqlTokensDto, authDetails, isResponseModeDcApi, originAudience);
            } catch (VPRequestValidationException ex) {
                saveRejectedSubmission(state, vpToken, ex);
                verifiablePresentationRequestService.invokeVpRequestStatusListener(state);
                throw ex;
            }
        }

        // ---- 8. generate response_code and build redirect_uri when required
        // (generateResponseCode is a no-op unless responseCodeValidationRequired is set)
        Map<String, Object> response = new HashMap<>();
        String responseCode = null;
        Timestamp responseCodeExpiryAt = null;
        responseCode = generateResponseCode(authDetails);
        if (responseCode != null) {
            log.debug("Generated response code for state {}", state);
            responseCodeExpiryAt = generateResponseCodeExpiry();
            String redirectUriWithResponseCode = resolveRedirectUri(responseCode);
            log.debug("Built redirect URI with response code for state {}", state);
            response.put("redirect_uri", redirectUriWithResponseCode);
        }

        // ---- 9. If all validations pass, proceed with VP submission processing
        submitVpToken(authDetails, vpToken, state, error, errorDescription, responseCode, responseCodeExpiryAt);

        // ---- 10. Notify status listeners after transaction commits so the DB record is visible
        verifiablePresentationRequestService.invokeVpRequestStatusListener(state);

        // --- 11. Return success response with redirect URI if generated
        return response;
    }

    /**
     * Validates that vp_token/error/error_description are mutually consistent, per the
     * OpenID4VP direct_post response rules.
     */
    private void validateSubmissionRequest(String vpToken, String error, String errorDescription) {
        if (!StringUtils.hasText(vpToken) && !StringUtils.hasText(error) && !StringUtils.hasText(errorDescription)) {
            throw new VPRequestValidationException(ErrorCode.EITHER_VP_TOKEN_OR_ERROR_REQUIRED);
        }
        if (StringUtils.hasText(vpToken) && StringUtils.hasText(error)) {
            throw new VPRequestValidationException(ErrorCode.BOTH_VP_TOKEN_AND_ERROR_NOT_ALLOWED);
        }
        if (StringUtils.hasText(errorDescription) && StringUtils.hasText(vpToken)) {
            throw new VPRequestValidationException(ErrorCode.ERROR_DESCRIPTION_VP_TOKEN_CONFLICT);
        }
        if (StringUtils.hasText(errorDescription) && !StringUtils.hasText(error)) {
            throw new VPRequestValidationException(ErrorCode.ERROR_DESCRIPTION_ERROR_REQUIRED);
        }
    }

    /**
     * Validates that the given state refers to an active, non-expired VP request that has not
     * already received a submission.
     */
    private void validateState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new VPRequestValidationException(ErrorCode.INVALID_STATE_MISSING);
        }
        VPRequestStatusDto currentVPRequestStatusDto = verifiablePresentationRequestService.getCurrentRequestStatus(state);
        if (currentVPRequestStatusDto == null) {
            throw new VPRequestValidationException(ErrorCode.NO_MATCHING_VP_REQUEST);
        }
        log.debug("Current VP request status for state {}: {}", state, currentVPRequestStatusDto.getStatus());
        if (currentVPRequestStatusDto.getStatus().equals(VPRequestStatus.EXPIRED)) {
            throw new VPRequestValidationException(ErrorCode.VP_REQUEST_EXPIRED);
        }
        if (currentVPRequestStatusDto.getStatus().equals(VPRequestStatus.VP_SUBMITTED)) {
            throw new VPRequestValidationException(ErrorCode.VP_ALREADY_SUBMITTED);
        }
    }

    /**
     * Validates the structural shape of a vp_token JSON object: must not be the literal string
     * "null", must be a non-empty JSON object whose values are non-empty arrays of a single
     * consistent element type (JSON object or SD-JWT string), and must not contain duplicate
     * query ids.
     */
    private void validateVpTokenStructure(String vpToken) {
        if (vpToken.trim().equalsIgnoreCase("null")) {
            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_REQUIRED);
        }
        try {
            JsonNode node = objectMapper.readTree(vpToken);

            if (!node.isObject()) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT);
            }
            if (node.size() < 1) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_MUST_HAVE_KEY_VALUE_PAIR);
            }
            for (JsonNode value : node) {
                if (!value.isArray()) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_VALUES_MUST_BE_ARRAYS);
                }
            }
            for (JsonNode value : node) {
                if (!value.isArray() || value.size() < 1) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_ARRAYS_MUST_HAVE_ELEMENTS);
                }
                JsonNode firstElement = value.get(0);
                boolean isFirstJson = firstElement.isObject() && firstElement.size() > 0;
                boolean isFirstSdJwt = firstElement.isTextual() && !firstElement.asText().isEmpty();
                if (!isFirstJson && !isFirstSdJwt) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_ARRAY_ELEMENTS_INVALID);
                }
                for (JsonNode element : value) {
                    if (isFirstJson) {
                        if (!element.isObject() || element.size() == 0) {
                            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_OBJECTS);
                        }
                    } else {
                        if (!element.isTextual() || element.asText().isEmpty()) {
                            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_SD_JWT);
                        }
                    }
                }
            }
            boolean isValid = validateDuplicateQueryIds(vpToken);
            if (!isValid) {
                log.debug("Duplicate query ids found in vp_token");
                throw new VPRequestValidationException(ErrorCode.DUPLICATE_QUERY_IDS_NOT_ALLOWED);
            }
        } catch (IllegalArgumentException | IOException e) {
            log.debug("Failed to parse vp_token as JSON: {}", e.getMessage());
            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT);
        }
    }

    private boolean validateDuplicateQueryIds(String vpToken) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(vpToken);
        try {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return false;
            }
            Set<String> seenKeys = new HashSet<>();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                if (!seenKeys.add(fieldName.trim())) {
                    return false;
                }
                parser.nextToken();
                parser.skipChildren();
            }
            return true;
        } finally {
            parser.close();
        }
    }

    /**
     * Validates a vp_token against the DCQL query on the authorization request, if one is present.
     */
    private void validateVpTokenAgainstDcql(String vpToken, AuthorizationRequestCreateResponse authRequestCreateResponse) {
        DCQLQueryDto dcqlQuery = authRequestCreateResponse.getAuthorizationDetails().getDcqlQuery();
        if (dcqlQuery == null) {
            return;
        }
        try {
            dcqlValidator.validateVpTokenAgainstDcql(dcqlQuery, objectMapper.readTree(vpToken));
        } catch (IOException e) {
            log.debug("Failed to parse vp_token as JSON while validating against DCQL: {}", e.getMessage());
            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT);
        }
    }

    /**
     * Validates audience then nonce (and SD-JWT KB-JWT iat) for bindable tokens.
     * Audience check is mode-specific; nonce / iat checks are shared.
     *
     * @param originAudience {@code origin:…} audience already resolved at step 4b for DC API; ignored for direct_post
     */
    private void validateAudienceAndNonce(
            DcqlTokensDto dcqlTokensDto,
            AuthorizationRequestResponseDto authDetails,
            boolean isResponseModeDcApi,
            String originAudience) {
        Map<String, List<JSONObject>> ldpVpTokens = dcqlTokensDto.getLdpVpTokens() != null
                ? dcqlTokensDto.getLdpVpTokens() : Collections.emptyMap();
        Map<String, List<String>> sdJwtTokens = dcqlTokensDto.getSdJwtTokens() != null
                ? dcqlTokensDto.getSdJwtTokens() : Collections.emptyMap();

        if (!ldpVpTokens.isEmpty()) {
            ErrorCode audienceError = isResponseModeDcApi
                    ? processLdpVpOrigin(ldpVpTokens, originAudience)
                    : processLdpVpClientId(authDetails, ldpVpTokens);
            if (audienceError != null) {
                throw new VPRequestValidationException(audienceError);
            }
            ErrorCode nonceError = validateLdpNonce(authDetails, ldpVpTokens);
            if (nonceError != null) {
                throw new VPRequestValidationException(nonceError);
            }
        }
        if (!sdJwtTokens.isEmpty()) {
            ErrorCode audienceError = isResponseModeDcApi
                    ? processSdJwtOrigin(authDetails, sdJwtTokens, originAudience)
                    : processSdJwtClientId(authDetails, sdJwtTokens);
            if (audienceError != null) {
                throw new VPRequestValidationException(audienceError);
            }
            ErrorCode nonceError = validateSdJwtNonce(authDetails, sdJwtTokens);
            if (nonceError != null) {
                throw new VPRequestValidationException(nonceError);
            }
            ErrorCode iatError = processSdJwtKbJwtIat(authDetails, sdJwtTokens);
            if (iatError != null) {
                throw new VPRequestValidationException(iatError);
            }
        }
        if (ldpVpTokens.isEmpty() && sdJwtTokens.isEmpty()) {
            log.debug("Skipping audience/nonce validation as no bindable tokens extracted.");
        }
    }

    /**
     * This method extracts the VP tokens from the input VP token string.
     * It distinguishes between LDP VP tokens and SD-JWT tokens based on their structure.
     * @param vpTokenString
     * @return
     */
    DcqlTokensDto extractDcqlTokens(String vpTokenString, AuthorizationRequestResponseDto authRequest) throws InvalidVpTokenException {
        Map<String, List<JSONObject>> ldpVpTokens = new HashMap<String, List<JSONObject>>();
        Map<String, List<JSONObject>> ldpVcTokens = new HashMap<String, List<JSONObject>>();
        Map<String, List<String>> sdJwtTokens = new HashMap<String, List<String>>();
        log.debug("Extracting VP tokens from input string");
        try {
            JsonNode root = objectMapper.readTree(vpTokenString);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String queryId = entry.getKey();
                boolean requireCryptographicHolderBinding = isCryptographicHolderBindingRequired(authRequest, queryId);
                log.info("require_cryptographic_holder_binding for query ID {}: {}", queryId, requireCryptographicHolderBinding);

                JsonNode value = entry.getValue();
                log.debug("Processing query ID: {}", queryId);
                if (!value.isArray()) {
                    log.warn("Unexpected JSON node type for query ID {}: {}", queryId, value.getNodeType());
                    throw new InvalidVpTokenException();
                }
                for (JsonNode item : value) {
                    if (item.isTextual()) {
                        if (isSdJwt(item.asText())) {
                            log.debug("Identified SD-JWT token");
                            // When require_cryptographic_holder_binding=true, the SD-JWT must have a cnf claim and a KB-JWT.
                            if (requireCryptographicHolderBinding && !Utils.hasSdJwtCnfClaim(item.asText())) {
                                String errorMessage = String.format(
                                        "SD-JWT token is missing cnf claim for query ID %s",
                                        queryId);
                                log.warn(errorMessage);
                                throw new InvalidVpTokenException(errorMessage);
                            }
                            if (requireCryptographicHolderBinding && !Utils.hasSdJwtKeyBinding(item.asText())) {
                                String errorMessage = String.format(
                                        "SD-JWT token is missing required Key Binding JWT for query ID %s",
                                        queryId);
                                log.warn(errorMessage);
                                throw new InvalidVpTokenException(errorMessage);
                            }
                            sdJwtTokens.computeIfAbsent(queryId, k -> new ArrayList<>())
                                    .add(item.asText());
                        } else {
                            String errorMessage = String.format(
                                    "Text node is not a valid SD-JWT token for query ID %s: %s",
                                    queryId,
                                    item.asText());
                            log.warn(errorMessage);
                            throw new InvalidVpTokenException(errorMessage);
                        }
                    } else if (item.isObject()) {
                        log.debug("Identified JSON LD Proof token");
                        boolean isLdpVpFormat = Utils.isLdpFormat(item, Constants.LDP_TYPE_VERIFIABLE_PRESENTATION);
                        boolean isLdpVcFormat = Utils.isLdpFormat(item, Constants.LDP_TYPE_VERIFIABLE_CREDENTIAL);
                        // Safety net for result-processing paths that bypass DcqlValidator (Validation E).
                        if (requireCryptographicHolderBinding && !isLdpVpFormat) {
                            String errorMessage = String.format(
                                    "JSON node does not match expected 'VerifiablePresentation' format for query ID %s: %s",
                                    queryId,
                                    item);
                            log.warn(errorMessage);
                            throw new InvalidVpTokenException(errorMessage);
                        }
                        if (!requireCryptographicHolderBinding && !isLdpVcFormat) {
                            String errorMessage = String.format(
                                    "JSON node does not match expected 'VerifiableCredential' format for query ID %s: %s",
                                    queryId,
                                    item);
                            log.warn(errorMessage);
                            throw new InvalidVpTokenException(errorMessage);
                        }
                        if (requireCryptographicHolderBinding) {
                            ldpVpTokens.computeIfAbsent(queryId, k -> new ArrayList<>())
                                    .add(new JSONObject(item.toString()));
                        } else {
                            ldpVcTokens.computeIfAbsent(queryId, k -> new ArrayList<>())
                                    .add(new JSONObject(item.toString()));
                        }

                    }
                }
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            String errorMessage = String.format("Failed to parse VP token: %s", e.getMessage());
            log.warn(errorMessage);
            throw new InvalidVpTokenException(errorMessage);
        }
        return new DcqlTokensDto(ldpVpTokens, ldpVcTokens, sdJwtTokens);
    }

    private boolean isCryptographicHolderBindingRequired(AuthorizationRequestResponseDto authRequest, String queryId) {
        return authRequest
                .getDcqlQuery()
                .getCredentials()
                .stream()
                .filter(cq -> cq.getId().equals(queryId))
                .findFirst()
                .map(cq -> !Boolean.FALSE.equals(cq.getRequire_cryptographic_holder_binding()))
                .orElseGet(() -> {
                    log.warn("No DCQL credential entry found for queryId '{}' in stored VP token; defaulting require_cryptographic_holder_binding to true", queryId);
                    return true;
                });
    }

    /**
     * direct_post: compare LDP {@code proof.domain} against the auth request {@code client_id}.
     */
    ErrorCode processLdpVpClientId(AuthorizationRequestResponseDto authRequest, Map<String, List<JSONObject>> ldpVpTokens) {
        String clientId = authRequest.getClientId();
        if (!StringUtils.hasText(clientId)) {
            log.error("clientId is missing in auth request");
            return ErrorCode.CLIENT_ID_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
            String queryId = entry.getKey();
            for (JSONObject jsonVPToken : entry.getValue()) {
                log.debug("Processing LDP VP client_id for query ID: {}", queryId);
                JSONObject proof = jsonVPToken.optJSONObject("proof");
                String domain = proof != null ? proof.optString("domain", null) : null;
                if (!OriginAudienceResolver.audienceMatches(clientId, domain)) {
                    log.error("clientId validation failed for query ID: {}, expected: {}, actual: {}", queryId, clientId, domain);
                    return ErrorCode.CLIENT_ID_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * dc_api: compare LDP {@code proof.domain} against the resolved {@code origin:…} audience.
     */
    ErrorCode processLdpVpOrigin(Map<String, List<JSONObject>> ldpVpTokens, String originAudience) {
        if (!StringUtils.hasText(originAudience)) {
            log.error("originAudience is missing for LDP VP origin validation");
            return ErrorCode.ORIGIN_AUDIENCE_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
            String queryId = entry.getKey();
            for (JSONObject jsonVPToken : entry.getValue()) {
                log.debug("Processing LDP VP origin audience for query ID: {}", queryId);
                JSONObject proof = jsonVPToken.optJSONObject("proof");
                String domain = proof != null ? proof.optString("domain", null) : null;
                if (!OriginAudienceResolver.audienceMatches(originAudience, domain)) {
                    log.error("origin audience validation failed for query ID: {}, expected: {}, actual: {}", queryId, originAudience, domain);
                    return ErrorCode.ORIGIN_AUDIENCE_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * Mode-independent LDP nonce check: compare {@code proof.challenge} to the auth request nonce.
     */
    ErrorCode validateLdpNonce(AuthorizationRequestResponseDto authRequest, Map<String, List<JSONObject>> ldpVpTokens) {
        String nonce = authRequest.getNonce();
        if (!StringUtils.hasText(nonce)) {
            log.error("nonce is missing in auth request");
            return ErrorCode.NONCE_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
            String queryId = entry.getKey();
            for (JSONObject jsonVPToken : entry.getValue()) {
                JSONObject proof = jsonVPToken.optJSONObject("proof");
                String challenge = proof != null ? proof.optString("challenge", null) : null;
                if (!Objects.equals(nonce, challenge)) {
                    log.error("nonce validation failed for query ID: {}, expected: {}, actual: {}", queryId, nonce, challenge);
                    return ErrorCode.NONCE_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * direct_post: compare SD-JWT KB-JWT {@code aud} against the auth request {@code client_id}.
     */
    ErrorCode processSdJwtClientId(AuthorizationRequestResponseDto authRequest, Map<String, List<String>> sdJwtTokens) {
        String clientId = authRequest.getClientId();
        if (!StringUtils.hasText(clientId)) {
            log.error("clientId is missing in auth request");
            return ErrorCode.CLIENT_ID_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
            String queryId = entry.getKey();
            if (!isCryptographicHolderBindingRequired(authRequest, queryId)) {
                log.debug("Skipping KB-JWT client_id validation for query ID {} since require_cryptographic_holder_binding is false", queryId);
                continue;
            }
            for (String sdJwt : entry.getValue()) {
                JSONObject kbPayload = Utils.extractKbJwtPayload(sdJwt);
                if (kbPayload == null) {
                    log.error("KB-JWT payload could not be decoded for query ID: {}", queryId);
                    return ErrorCode.CLIENT_ID_VALIDATION_FAILED;
                }
                String audClaim = kbPayload.optString("aud", null);
                if (!OriginAudienceResolver.audienceMatches(clientId, audClaim)) {
                    log.error("KB-JWT aud mismatch for query ID: {}, expected: {}, actual: {}", queryId, clientId, audClaim);
                    return ErrorCode.CLIENT_ID_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * dc_api: compare SD-JWT KB-JWT {@code aud} against the resolved {@code origin:…} audience.
     */
    ErrorCode processSdJwtOrigin(AuthorizationRequestResponseDto authRequest, Map<String, List<String>> sdJwtTokens, String originAudience) {
        if (!StringUtils.hasText(originAudience)) {
            log.error("originAudience is missing for SD-JWT origin validation");
            return ErrorCode.ORIGIN_AUDIENCE_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
            String queryId = entry.getKey();
            if (!isCryptographicHolderBindingRequired(authRequest, queryId)) {
                log.debug("Skipping KB-JWT origin validation for query ID {} since require_cryptographic_holder_binding is false", queryId);
                continue;
            }
            for (String sdJwt : entry.getValue()) {
                JSONObject kbPayload = Utils.extractKbJwtPayload(sdJwt);
                if (kbPayload == null) {
                    log.error("KB-JWT payload could not be decoded for query ID: {}", queryId);
                    return ErrorCode.ORIGIN_AUDIENCE_VALIDATION_FAILED;
                }
                String audClaim = kbPayload.optString("aud", null);
                if (!OriginAudienceResolver.audienceMatches(originAudience, audClaim)) {
                    log.error("KB-JWT origin aud mismatch for query ID: {}, expected: {}, actual: {}", queryId, originAudience, audClaim);
                    return ErrorCode.ORIGIN_AUDIENCE_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * Mode-independent SD-JWT nonce check: compare KB-JWT {@code nonce} to the auth request nonce.
     */
    ErrorCode validateSdJwtNonce(AuthorizationRequestResponseDto authRequest, Map<String, List<String>> sdJwtTokens) {
        String nonce = authRequest.getNonce();
        if (!StringUtils.hasText(nonce)) {
            log.error("nonce is missing in auth request");
            return ErrorCode.NONCE_VALIDATION_FAILED;
        }
        for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
            String queryId = entry.getKey();
            if (!isCryptographicHolderBindingRequired(authRequest, queryId)) {
                log.debug("Skipping KB-JWT nonce validation for query ID {} since require_cryptographic_holder_binding is false", queryId);
                continue;
            }
            for (String sdJwt : entry.getValue()) {
                JSONObject kbPayload = Utils.extractKbJwtPayload(sdJwt);
                if (kbPayload == null) {
                    log.error("KB-JWT payload could not be decoded for nonce check, query ID: {}", queryId);
                    return ErrorCode.NONCE_VALIDATION_FAILED;
                }
                String kbNonce = kbPayload.optString("nonce", null);
                if (!Objects.equals(nonce, kbNonce)) {
                    log.error("KB-JWT nonce mismatch for query ID: {}, expected: {}, actual: {}", queryId, nonce, kbNonce);
                    return ErrorCode.NONCE_VALIDATION_FAILED;
                }
            }
        }
        return null;
    }

    /**
     * Validates the KB-JWT {@code iat} claim for all SD-JWT tokens in a single pass.
     * Checks that {@code iat} is:
     * <ul>
     *   <li>present and a positive value</li>
     *   <li>not in the future beyond {@code clockSkewSeconds} (60 s)</li>
     *   <li>not older than {@code kbJwtMaxAgeSeconds} (default: 600 s / 10 minutes)</li>
     * </ul>
     * The {@code nonce} is the primary replay-protection mechanism per SD-JWT RFC 9901;
     * {@code kbJwtMaxAgeSeconds} guards against very stale KB-JWTs (e.g. replayed presentations).
     * Configurable via {@code inji.verify.kb-jwt-max-age-seconds} (default: 600 s / 10 minutes).
     * Only applies to query IDs where {@code require_cryptographic_holder_binding=true}.
     * Returns the first {@link ErrorCode} encountered, or null if all tokens pass.
     */
    ErrorCode processSdJwtKbJwtIat(AuthorizationRequestResponseDto authRequest, Map<String, List<String>> sdJwtTokens) {
        final long clockSkewSeconds = 60L;
        for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
            String queryId = entry.getKey();
            if (!isCryptographicHolderBindingRequired(authRequest, queryId)) {
                log.debug("Skipping KB-JWT iat validation for query ID {} since require_cryptographic_holder_binding is false", queryId);
                continue;
            }
            for (String sdJwt : entry.getValue()) {
                JSONObject kbPayload = Utils.extractKbJwtPayload(sdJwt);
                if (kbPayload == null) {
                    log.error("KB-JWT payload could not be decoded for iat check, query ID: {}", queryId);
                    return ErrorCode.KB_JWT_IAT_MISSING_OR_INVALID;
                }
                long iat = kbPayload.optLong("iat", -1);
                if (iat <= 0) {
                    log.error("KB-JWT iat is missing or invalid for query ID: {}", queryId);
                    return ErrorCode.KB_JWT_IAT_MISSING_OR_INVALID;
                }
                long nowSeconds = System.currentTimeMillis() / 1000;
                if (iat > nowSeconds + clockSkewSeconds) {
                    log.error("KB-JWT iat is in the future for query ID: {}, iat={}, now={}", queryId, iat, nowSeconds);
                    return ErrorCode.KB_JWT_IAT_IN_FUTURE;
                }
                if (iat < nowSeconds - kbJwtMaxAgeSeconds) {
                    log.error("KB-JWT iat is too old for query ID: {}, iat={}, now={}, maxAge={}s", queryId, iat, nowSeconds, kbJwtMaxAgeSeconds);
                    return ErrorCode.KB_JWT_IAT_TOO_OLD;
                }
            }
        }
        return null;
    }

    /**
     * This method generates a unique response code using UUID if the authorization request requires response code validation. If response code validation is not required, it returns null.
     * @param authRequest
     * @return
     */
    String generateResponseCode(AuthorizationRequestResponseDto authRequest) {
    	String responseCode = null;
    	boolean responseCodeValidationRequired = false;
        responseCodeValidationRequired = authRequest.isResponseCodeValidationRequired();
        if (responseCodeValidationRequired) {
        	log.debug("Generating response code since response code validation is required");
            responseCode = UUID.randomUUID().toString();
        }    
        return responseCode;
    }

    /**
     * This method generates the response code expiry time by adding the configured expiry duration (in minutes) to the current time.
     * @return
     */
	Timestamp generateResponseCodeExpiry() {
		log.debug("Generating response code expiry time since response code validation is required");
		Timestamp responseCodeExpiryAt = Timestamp.from(Instant.now().plus(responseCodeExpiryTimeInMins, ChronoUnit.MINUTES));
		return responseCodeExpiryAt;
	}

    /**
     * This method builds the redirect URI by appending the response code as a fragment to the base redirect URI.
     * @param responseCode
     * @return
     */
    String buildRedirectUri(String responseCode) {
        if (redirectUri == null || redirectUri.isBlank() || responseCode == null) return null;
        String redirectUriWithResponseCode = UriComponentsBuilder
                    .fromUriString(redirectUri)
                    .fragment("response_code=" + responseCode)
                    .build()
                    .toUriString();
        return redirectUriWithResponseCode;
    }

    /**
     * Same as {@link #buildRedirectUri}, but throws {@link RedirectUriGenerationException}
     * instead of returning null when the redirect URI cannot be built.
     */
    String resolveRedirectUri(String responseCode) {
        String redirectUriWithResponseCode = buildRedirectUri(responseCode);
        if (redirectUriWithResponseCode == null) {
            throw new RedirectUriGenerationException();
        }
        return redirectUriWithResponseCode;
    }

    /**
     * This method is used to persist the VP submission details along with the response code and
     * its expiry time.
     */
	void submitVpToken(AuthorizationRequestResponseDto authRequest, String vpToken, String state, String error,
			String errorDescription, String responseCode, Timestamp responseCodeExpiryAt)
			throws VPAlreadySubmittedException {

		/// --- persist VP submission with response code and other details ---
		VPSubmission vpSubmission = new VPSubmission(state, vpToken, null, error, errorDescription, responseCode,
				responseCodeExpiryAt, false);

		try {
			vpSubmissionRepository.save(vpSubmission);
		} catch (DataIntegrityViolationException e) {
			throw new VPAlreadySubmittedException("VP already submitted for request_id: " + state, e);
		}
        log.debug("VP submission saved successfully for state: {}", state);

	}

    /**
     * Persists a rejected VP submission (DCQL / audience / nonce) so the verifier session can close.
     * Best-effort: a concurrent successful submit should not replace the original error returned to the wallet.
     */
    private void saveRejectedSubmission(String state, String vpToken, RuntimeException ex) {
        String errorCode = ex instanceof VPRequestValidationException vpEx
                ? vpEx.getErrorCode().name()
                : ErrorCode.INVALID_VP_TOKEN.name();
        try {
            submitVpToken(null, vpToken, state, errorCode, ex.getMessage(), null, null);
        } catch (VPAlreadySubmittedException e) {
            log.warn("Could not save failed submission for state {}: {}", state, e.getMessage());
        }
    }

    private VPTokenResultDto processSubmission(VPSubmission vpSubmission, String transactionId, AuthorizationRequestCreateResponse authRequest) throws VPSubmissionWalletError,  InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException {
        log.info("Processing VP submission");
        List<VCResultDto> verificationResults = new ArrayList<>();
        List<VPVerificationStatus> vpVerificationStatuses = new ArrayList<>();
        try {
            //Legacy processing for backward compatibility to support older transactions with Presentation Exchange.
            boolean isPresentationExchange = isAuthRequestWithPresentationExchange(authRequest);
            log.info("Is authorization request with presentation exchange: {}", isPresentationExchange);
            if (isPresentationExchange) {
               log.info("Authorization request contains presentation exchange, validating VP token against authorization request");
                if (isVPTokenNotMatching(vpSubmission, authRequest)) throw new TokenMatchingFailedException();
                VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());
                boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(authRequest);
                log.info("Is authorization request accept VP without holder proof: {}", acceptVPWithoutHolderProof);
                for (JSONObject vpToken : vpTokenDto.getJsonVpTokens()) {
                    if (!isValidVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();
                    boolean isSigned = isVerifiablePresentationSigned(vpToken);
                    if (isSigned) {
                        verifyPresentationWithCredentialStatusChecks(vpToken, vpVerificationStatuses, verificationResults);
                    } else if (acceptVPWithoutHolderProof) {
                        Object verifiableCredential = vpToken.opt("verifiableCredential");
                        List<Object> listOfVerifiableCredentials = getListOfVerifiableCredentials(verifiableCredential);
                        for (Object credential : listOfVerifiableCredentials) {
                            verifyAndGetCredentialStatus(credential.toString(), verificationResults, CredentialFormat.LDP_VC, false);
                        }
                    } else {
                        throw new VPWithoutProofException();
                    }
                }
                for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
                    verifyAndGetCredentialStatus(sdJwtVpToken, verificationResults, CredentialFormat.DC_SD_JWT, !acceptVPWithoutHolderProof);
                }
            } else {
                log.info("Authorization request does not contain presentation exchange, processing VP token as DCQL query response");
                DcqlTokensDto dcqlTokensDto = extractDcqlTokens(vpSubmission.getVpToken(), authRequest.getAuthorizationDetails());
                Map<String, List<JSONObject>> ldpVpTokens = dcqlTokensDto.getLdpVpTokens();
                Map<String, List<JSONObject>> ldpVcTokens = dcqlTokensDto.getLdpVcTokens();
                Map<String, List<String>> sdJwtTokens = dcqlTokensDto.getSdJwtTokens();
                for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing LDP VP tokens for query ID: {}", queryId);
                    for (JSONObject ldpVpToken : entry.getValue()) {
                        verifyPresentationWithCredentialStatusChecks(ldpVpToken, vpVerificationStatuses, verificationResults);
                    }
                }
                for (Map.Entry<String, List<JSONObject>> entry : ldpVcTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing LDP VC tokens for query ID: {}", queryId);
                    for (JSONObject ldpVcToken : entry.getValue()) {
                        String ldpVcString = ldpVcToken.toString();
                        verifyAndGetCredentialStatus(ldpVcString, verificationResults, CredentialFormat.LDP_VC, false);
                    }
                }
                for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing SD-JWT tokens for query ID: {}", queryId);
                    boolean requireCryptographicHolderBinding = isCryptographicHolderBindingRequired(authRequest.getAuthorizationDetails(), queryId);
                    for (String sdJwtToken : entry.getValue()) {
                        verifyAndGetCredentialStatus(sdJwtToken, verificationResults, CredentialFormat.DC_SD_JWT, requireCryptographicHolderBinding);
                    }
                }
            }
            log.info("VP submission processing done");
            return new VPTokenResultDto(transactionId, getCombinedVerificationStatus(vpVerificationStatuses, verificationResults), verificationResults);
        } catch (VPSubmissionWalletError e) {
            log.error("Received wallet error: {} - {}", e.getErrorCode(), e.getErrorDescription());
            throw e;
        } catch (CredentialStatusCheckException e) {
            log.error("Received Credential status check exception: {} - {}", e.getErrorCode(), e.getErrorDescription());
            throw e;
        } catch (VPWithoutProofException e) {
            log.error("Received Invalid VP: ", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify VP submission", e);
            return new VPTokenResultDto(transactionId, VPResultStatus.FAILED, verificationResults);
        }
    }

    private VPVerificationResultDto processSubmissionV2(VerificationRequestDto request, String transactionId, VPSubmission vpSubmission, AuthorizationRequestCreateResponse authRequest) {
        try {
            log.info("Processing VP submission V2");
            List<CredentialResultsDto> credentialResults = new ArrayList<>();
            boolean isPresentationExchange = isAuthRequestWithPresentationExchange(authRequest);
            log.info("Is authorization request with presentation exchange: {}", isPresentationExchange);
            //Legacy processing for backward compatibility to support older transactions with Presentation Exchange.
            if (isPresentationExchange) {
                log.info("Authorization request contains presentation exchange, validating VP token against authorization request");
                boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(authRequest);
                log.info("Is authorization request accept VP without holder proof: {}", acceptVPWithoutHolderProof);
                if (isVPTokenNotMatching(vpSubmission, authRequest)) throw new TokenMatchingFailedException();
                VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());
                for (JSONObject ldpVpToken : vpTokenDto.getJsonVpTokens()) {
                    if (!isValidVerifiablePresentation(ldpVpToken)) throw new InvalidVpTokenException();
                    boolean isSigned = isVerifiablePresentationSigned(ldpVpToken);
                    if (isSigned) {
                        if (request.isSkipStatusChecks()) {
                            verifyPresentationV2(request, ldpVpToken, credentialResults);
                        } else {
                            verifyPresentationWithCredentialStatusChecksV2(request, ldpVpToken, credentialResults);

                        }
                    } else if (acceptVPWithoutHolderProof) {
                        // for a VPToken without proof, do verification for all credentials
                        Object verifiableCredential = ldpVpToken.opt("verifiableCredential");
                        List<Object> listOfVerifiableCredentials = getListOfVerifiableCredentials(verifiableCredential);
                        for (Object credential : listOfVerifiableCredentials) {
                            credentialResults.add(verifyAndGetCredentialStatusV2(request, credential, false, false));
                        }
                    } else {
                        throw new VPWithoutProofException();
                    }
                }
                for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
                    credentialResults.add(verifyAndGetCredentialStatusV2(request, sdJwtVpToken, true, !acceptVPWithoutHolderProof));
                }
            } else {
                log.info("Authorization request does not contain presentation exchange, processing VP token as DCQL query response");
                DcqlTokensDto dcqlTokensDto = extractDcqlTokens(vpSubmission.getVpToken(), authRequest.getAuthorizationDetails());
                Map<String, List<JSONObject>> ldpVpTokens = dcqlTokensDto.getLdpVpTokens();
                Map<String, List<JSONObject>> ldpVcTokens = dcqlTokensDto.getLdpVcTokens();
                Map<String, List<String>> sdJwtTokens = dcqlTokensDto.getSdJwtTokens();
                for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing LDP VP tokens for query ID: {}", queryId);
                    for (JSONObject ldpVpToken : entry.getValue()) {
                        if (request.isSkipStatusChecks()) {
                            verifyPresentationV2(request, ldpVpToken, credentialResults);
                        } else {
                            verifyPresentationWithCredentialStatusChecksV2(request, ldpVpToken, credentialResults);
                        }
                    }
                }
                for (Map.Entry<String, List<JSONObject>> entry : ldpVcTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing LDP VC tokens for query ID: {}", queryId);
                    for (JSONObject ldpVcToken : entry.getValue()) {
                        String ldpVcString = ldpVcToken.toString();
                        credentialResults.add(verifyAndGetCredentialStatusV2(request, ldpVcString, false, false));
                    }
                }
                for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing SD-JWT tokens for query ID: {}", queryId);
                    boolean requireCryptographicHolderBinding = isCryptographicHolderBindingRequired(authRequest.getAuthorizationDetails(), queryId);
                    for (String sdJwtToken : entry.getValue()) {
                        credentialResults.add(verifyAndGetCredentialStatusV2(request, sdJwtToken, true, requireCryptographicHolderBinding));
                    }
                }
            }
            boolean allChecksSuccessful = credentialResults.stream().allMatch(CredentialResultsDto::isAllChecksSuccessful);
            log.info("VP submission processing done V2");
            return new VPVerificationResultDto(transactionId, allChecksSuccessful, credentialResults);
        }  catch (TokenMatchingFailedException | InvalidVpTokenException | VPWithoutProofException ex) {
            log.error("Failed to process VP submission V2", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to process VP submission V2", ex);
            throw new VPVerificationException(ex);
        }
    }

    private List<Object> getListOfVerifiableCredentials(Object verifiableCredential) {
        if (verifiableCredential instanceof JSONArray array) {
            if (array.isEmpty()) throw new InvalidVpTokenException();
            List<Object> verifiableCredentialsList = new ArrayList<>();
            for (Object credential : array)
                verifiableCredentialsList.add(credential);
            return verifiableCredentialsList;
        }
        if (verifiableCredential instanceof JSONObject || verifiableCredential instanceof String) {
            return List.of(verifiableCredential);
        }
        throw new InvalidVpTokenException();
    }

    private void verifyPresentationWithCredentialStatusChecks(JSONObject vpToken, List<VPVerificationStatus> vpVerificationStatuses, List<VCResultDto> verificationResults) {
        List<String> statusPurposeList = new ArrayList<>();
        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
        PresentationResultWithCredentialStatus presentationResultWithCredentialStatus = presentationVerifier.verifyAndGetCredentialStatus(vpToken.toString(), statusPurposeList);
        VPVerificationStatus proofVerificationStatus = presentationResultWithCredentialStatus.getProofVerificationStatus();
        vpVerificationStatuses.add(proofVerificationStatus);

        List<VCResultWithCredentialStatus> vcResultsWithStatus = presentationResultWithCredentialStatus.getVcResults();
        if (vcResultsWithStatus.isEmpty()) throw new InvalidVpTokenException();
        List<VCResultDto> vcResults = new ArrayList<>();
        for (var vcResult : vcResultsWithStatus) {
            VerificationStatus vcStatus = Utils.applyRevocationStatus(vcResult.getStatus(), vcResult.getCredentialStatus());
            vcResults.add(new VCResultDto(vcResult.getVc(), vcStatus));
        }
        verificationResults.addAll(vcResults);
    }

    private void verifyPresentationWithCredentialStatusChecksV2(VerificationRequestDto request, JSONObject vpToken, List<CredentialResultsDto> credentialResults) {
        List<String> filters = request.getStatusCheckFilters();
        PresentationResultWithCredentialStatusV2 result = presentationVerifier.verifyAndGetCredentialStatusV2(vpToken.toString(), filters);
        List<VCResultWithCredentialStatusV2> vcResults = result.getVcResults();
        if (vcResults.isEmpty()) throw new InvalidVpTokenException();
        for (VCResultWithCredentialStatusV2 vcResWithStatus : vcResults) {
            CredentialResultsDto credentialResultsDto = new CredentialResultsDto();
            credentialResultsDto.setVerifiableCredential(vcResWithStatus.getVc());
            credentialResultsDto.setHolderProofCheck(populateHolderProofDto(result.getProofVerificationResult()));
            credentialResultsDto.setSchemaAndSignatureCheck(populateSchemaAndSignature(vcResWithStatus.getVerificationResult()));
            ExpiryCheckDto expiryCheckDto =
                    (credentialResultsDto.getSchemaAndSignatureCheck().isValid()) ? populateExpiryCheck(vcResWithStatus.getVerificationResult()) : null;
            Map<String, Object> claims =
                    (credentialResultsDto.getSchemaAndSignatureCheck().isValid() && request.isIncludeClaims()) ? extractClaims(vcResWithStatus.getVc(), CredentialFormat.LDP_VC, claimsWithMetaData, pixelPass) : Map.of();
            credentialResultsDto.setExpiryCheck(expiryCheckDto);
            credentialResultsDto.setClaims(claims);
            credentialResultsDto.setStatusCheck(populateStatusCheckDtoList(vcResWithStatus.getCredentialStatus()));
            boolean allChecksSuccessful = populateAllChecksSuccessful(credentialResultsDto.getSchemaAndSignatureCheck(), credentialResultsDto.getExpiryCheck(), credentialResultsDto.getStatusCheck(), credentialResultsDto.getHolderProofCheck());
            credentialResultsDto.setAllChecksSuccessful(allChecksSuccessful);
            credentialResults.add(credentialResultsDto);
        }
    }

    private void verifyPresentationV2(VerificationRequestDto request, JSONObject vpToken, List<CredentialResultsDto> credentialResults) {
        PresentationVerificationResultV2 result = presentationVerifier.verifyV2(vpToken.toString());
        List<VCResultV2> vcResults = result.getVcResults();
        if (vcResults.isEmpty()) throw new InvalidVpTokenException();
        for (VCResultV2 vcRes : vcResults) {
            CredentialResultsDto credentialResultsDto = new CredentialResultsDto();
            credentialResultsDto.setVerifiableCredential(vcRes.getVc());
            credentialResultsDto.setHolderProofCheck(populateHolderProofDto(result.getProofVerificationResult()));
            credentialResultsDto.setSchemaAndSignatureCheck(populateSchemaAndSignature(vcRes.getVerificationResult()));
            ExpiryCheckDto expiryCheckDto =
                    (credentialResultsDto.getSchemaAndSignatureCheck().isValid()) ? populateExpiryCheck(vcRes.getVerificationResult()) : null;
            Map<String, Object> claims =
                    (credentialResultsDto.getSchemaAndSignatureCheck().isValid() && request.isIncludeClaims()) ? extractClaims(vcRes.getVc(), CredentialFormat.LDP_VC, claimsWithMetaData, pixelPass) : Map.of();
            credentialResultsDto.setExpiryCheck(expiryCheckDto);
            credentialResultsDto.setClaims(claims);
            boolean allChecksSuccessful = populateAllChecksSuccessful(credentialResultsDto.getSchemaAndSignatureCheck(), credentialResultsDto.getExpiryCheck(), credentialResultsDto.getStatusCheck(), credentialResultsDto.getHolderProofCheck());
            credentialResultsDto.setAllChecksSuccessful(allChecksSuccessful);
            credentialResults.add(credentialResultsDto);
        }
    }

    private boolean isAcceptVPWithoutHolderProof(AuthorizationRequestCreateResponse request) {
        return Optional.ofNullable(request.getAuthorizationDetails()).map(AuthorizationRequestResponseDto::isAcceptVPWithoutHolderProof).orElse(false);
    }

    private void verifyAndGetCredentialStatus(String vc, List<VCResultDto> verificationResults, CredentialFormat credentialFormat, boolean requireCryptographicHolderBinding) {
        List<String> statusPurposeList = new ArrayList<>();
        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
        boolean validateKbJwt = false;
        if (requireCryptographicHolderBinding && CredentialFormat.DC_SD_JWT.equals(credentialFormat)) {
            validateKbJwt = true;
        }
        CredentialVerificationSummary credentialVerificationSummary = credentialsVerifier.verifyAndGetCredentialStatus(vc, credentialFormat, statusPurposeList, validateKbJwt);
        VerificationResult verificationResult = credentialVerificationSummary.getVerificationResult();
        if (!verificationResult.getVerificationStatus()) {
            log.error("VC Verification Failed");
            log.error("VC verification result errors : {} {}", verificationResult.getVerificationErrorCode(), verificationResult.getVerificationMessage());
        }
        VerificationStatus status = Utils.getVcVerificationStatus(credentialVerificationSummary);
        verificationResults.add(new VCResultDto(vc, status));
    }

    private boolean isValidVerifiablePresentation(JSONObject jsonObject) {
        try {
            return Utils.isLdpFormat(objectMapper.readTree(jsonObject.toString()), Constants.LDP_TYPE_VERIFIABLE_PRESENTATION);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private boolean isVerifiablePresentationSigned(JSONObject vpToken) {
        Object proof = vpToken.opt("proof");
        return proof != null;
    }

    public VPTokenDto extractTokens(String vpTokenString) {
        if (vpTokenString == null || vpTokenString.isEmpty()) throw new InvalidVpTokenException();
        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();

        try {
            Object vpTokenRaw = new JSONTokener(vpTokenString).nextValue();

            if (vpTokenRaw instanceof JSONArray array) {
                IntStream.range(0, array.length()).forEach(i -> processSingleToken(array.get(i), jsonVpTokens, sdJwtVpTokens));
            } else {
                processSingleToken(vpTokenRaw, jsonVpTokens, sdJwtVpTokens);
            }
        } catch (JSONException e) {
            log.error("Failed to parse VP Token JSON", e);
            throw new InvalidVpTokenException();
        }

        log.debug("Number of VP tokens to verify: {}", jsonVpTokens.size() + ":" + sdJwtVpTokens.size());
        if (jsonVpTokens.isEmpty() && sdJwtVpTokens.isEmpty()) {
            throw new InvalidVpTokenException();
        }
        return new VPTokenDto(jsonVpTokens, sdJwtVpTokens);
    }

    private void processSingleToken(Object item, List<JSONObject> jsonVpTokens, List<String> sdJwtVpTokens) {
        switch (item) {
            case String itemString -> {
                if (isSdJwt(itemString)) {
                    sdJwtVpTokens.add(itemString);
                } else {
                    try {
                        String decodedJson = new String(Base64.getUrlDecoder().decode(itemString));
                        Object decodedRaw = new JSONTokener(decodedJson).nextValue();

                        if (decodedRaw instanceof JSONObject decodedObject) {
                            jsonVpTokens.add(decodedObject);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to decode or parse token string: {}", e.getMessage());
                    }
                }
            }
            case JSONObject jsonObject -> jsonVpTokens.add(jsonObject);
            case null, default -> {
            }
        }

    }

    private boolean isAuthRequestWithPresentationExchange(AuthorizationRequestCreateResponse authRequest) {
        boolean isAuthRequestWithPresentationExchange = false;
        log.info("Checking if authorization request is for presentation exchange");
        if (authRequest != null && authRequest.getAuthorizationDetails() != null && authRequest.getAuthorizationDetails().getPresentationDefinition() !=null
                && authRequest.getAuthorizationDetails().getDcqlQuery() == null) {
            isAuthRequestWithPresentationExchange = true;
            log.info("Authorization request is for presentation exchange");
        }
        return isAuthRequestWithPresentationExchange;
    }

    /**
     * Resolves a transactionId to its VP submission result, falling back to a VC submission
     * result if no VP request matches. This is the single entry point for the v1 result-lookup
     * endpoint — any caller (this service's own controller, or a consumer embedding this service
     * directly) gets identical guarantees.
     */
    @Override
    public Object getVPResult(String transactionId) throws InvalidTransactionIdException, VPSubmissionWalletError, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        if (requestIds.isEmpty()) {
            VCSubmissionVerificationStatusDto vcResult = vcSubmissionService.getVcWithVerification(transactionId);
            if (vcResult != null) {
                return vcResult;
            }
            throw new InvalidTransactionIdException();
        }
        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds, null, authRequest, false);
        return processSubmission(vpSubmission, transactionId, authRequest);
    }

    /**
     * Resolves a transactionId to its VP submission result. Single entry point for the v2
     * result-lookup endpoint.
     */
    @Override
    public VPVerificationResultDto getVPResultV2(VerificationRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        if (requestIds.isEmpty()) {
            throw new InvalidTransactionIdException();
        }
        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds, null, authRequest, false);
        log.debug("Processing VP submission, token length: {}", vpSubmission.getVpToken() != null ? vpSubmission.getVpToken().toString().length() : 0);
        return processSubmissionV2(request, transactionId, vpSubmission, authRequest);
    }

    /**
     * Resolves a transactionId (from the session cookie) to its VP submission result. Single
     * entry point for the session result-lookup endpoint.
     */
    @Override
    @Transactional
    public VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException {
        List<String> requestIds = verifiablePresentationRequestService.getLatestRequestIdFor(transactionId);
        if (requestIds.isEmpty()) {
            throw new InvalidTransactionIdException();
        }
        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds, request.getResponseCode(), authRequest, true);
        return processSubmissionV2(request, transactionId, vpSubmission, authRequest);
    }

    private boolean isVPTokenNotMatching(VPSubmission vpSubmission, AuthorizationRequestCreateResponse request) {
        Object vpTokenRaw = new JSONTokener(vpSubmission.getVpToken()).nextValue();
        List < DescriptorMapDto > descriptorMap = vpSubmission.getPresentationSubmission() != null ? vpSubmission.getPresentationSubmission().getDescriptorMap() : null;
        if(vpTokenRaw == null || request == null || descriptorMap == null || descriptorMap.isEmpty()) {
            log.info("VP token matching failed due to missing VP token, authorization request or descriptor map");
            return true;
        }
        log.info("VP token matching done based on presentation submission.");
        return false;
    }


    private VPResultStatus getCombinedVerificationStatus(List<VPVerificationStatus> vpVerificationStatuses, List<VCResultDto> verificationResults) {
        boolean combinedVerificationStatus = true;
        for (VPVerificationStatus vpVerificationStatus : vpVerificationStatuses) {
            combinedVerificationStatus = combinedVerificationStatus && (vpVerificationStatus == VPVerificationStatus.VALID);
        }
        for (VCResultDto verificationResult : verificationResults) {
            combinedVerificationStatus = combinedVerificationStatus && (verificationResult.getVerificationStatus() == VerificationStatus.SUCCESS);
        }
        return combinedVerificationStatus ? VPResultStatus.SUCCESS : VPResultStatus.FAILED;
    }

    private CredentialResultsDto verifyAndGetCredentialStatusV2(VerificationRequestDto request, Object vc, boolean isSdJwt, boolean requireCryptographicHolderBinding) {
        VCVerificationRequestDto vcVerificationRequestDto = new VCVerificationRequestDto(vc.toString());
        vcVerificationRequestDto.setSkipStatusChecks(request.isSkipStatusChecks());
        vcVerificationRequestDto.setStatusCheckFilters(request.getStatusCheckFilters());
        vcVerificationRequestDto.setIncludeClaims(request.isIncludeClaims());

        boolean validateKbJwt = false;
        if (requireCryptographicHolderBinding && isSdJwt) {
            validateKbJwt = true;
        }
        VCVerificationResultDto resultDto = vcVerificationService.verifyV2(vcVerificationRequestDto, validateKbJwt);

        CredentialResultsDto credentialResults = new CredentialResultsDto();
        credentialResults.setVerifiableCredential(vc.toString());
        credentialResults.setAllChecksSuccessful(resultDto.isAllChecksSuccessful());
        credentialResults.setSchemaAndSignatureCheck(resultDto.getSchemaAndSignatureCheck());
        credentialResults.setExpiryCheck(resultDto.getExpiryCheck());
        credentialResults.setStatusCheck(resultDto.getStatusCheck());
        credentialResults.setClaims(resultDto.getClaims());
        if (isSdJwt && validateKbJwt) {
            SchemaAndSignatureCheckDto schemaAndSignatureCheck = resultDto.getSchemaAndSignatureCheck();
            if (schemaAndSignatureCheck.isValid()) {
                credentialResults.setHolderProofCheck(new HolderProofCheckDto(true, null));
            } else {
                ErrorDto errorDto = schemaAndSignatureCheck.getError();
                if (errorDto != null) {
                    for (KBJwtErrorCodes errorCode : KBJwtErrorCodes.values()) {
                        if (errorCode.name().equals(errorDto.getErrorCode())) {
                            credentialResults.setHolderProofCheck(new HolderProofCheckDto(false, errorDto));
                        }
                    }
                }
            }
        }  else {
            credentialResults.setHolderProofCheck(null);
        }

        return credentialResults;
    }

    private VPSubmission fetchVpSubmissionIfValid(List<String> requestIds, String responseCode, AuthorizationRequestCreateResponse authRequest, boolean isResponseCodeMandatory) {
        VPSubmission submission = vpSubmissionRepository.findAllById(requestIds)
                .stream()
                .findFirst()
                .orElseThrow(VPSubmissionNotFoundException::new);

        boolean responseCodeValidationRequired = false;
		if (authRequest != null && authRequest.getAuthorizationDetails() != null) {
			responseCodeValidationRequired = authRequest.getAuthorizationDetails().isResponseCodeValidationRequired();
		}
        if (responseCodeValidationRequired) validateResponseCode(responseCode, submission, isResponseCodeMandatory);

        if (submission.getError() != null && !submission.getError().isEmpty())
            throw new VPSubmissionWalletError(submission.getError(), submission.getErrorDescription());

        return submission;
    }

    private void validateResponseCode(String responseCode, VPSubmission submission, boolean isResponseCodeMandatory) {
        if (isResponseCodeMandatory) {
            if (submission.getResponseCode() == null || responseCode == null || submission.getResponseCode().isEmpty())
                throw new ResponseCodeException(ErrorCode.RESPONSE_CODE_NOT_FOUND);

            if (!responseCode.equals(submission.getResponseCode()))
                throw new ResponseCodeException(ErrorCode.RESPONSE_CODE_NOT_MATCHING);

            if (submission.getResponseCodeExpiryAt() != null
                    && Instant.now().isAfter(submission.getResponseCodeExpiryAt().toInstant())) {
                throw new ResponseCodeException(ErrorCode.RESPONSE_CODE_EXPIRED);
            }

            if (vpSubmissionRepository.markResponseCodeAsUsed(submission.getRequestId()) == 0) {
                throw new ResponseCodeException(ErrorCode.RESPONSE_CODE_USED);
            }
        } else {
            // This is to support Relying Parties to retrieve VP results after response_code has been used.
            //  The Relying Parties may maintain a list of past transactions and may want to show results for those. In that case since response_code was consumed once, we can safely show the past results.
            if (!submission.isResponseCodeUsed()) {
                throw new ResponseCodeException(ErrorCode.RESPONSE_CODE_NOT_USED);
            }
        }
    }

    private HolderProofCheckDto populateHolderProofDto(VerificationResult verificationResult) {
        boolean isValid = verificationResult.getVerificationStatus();
        ErrorDto error = isValid ? null : new ErrorDto(verificationResult.getVerificationErrorCode(), verificationResult.getVerificationMessage());

        return new HolderProofCheckDto(isValid, error);
    }
}
