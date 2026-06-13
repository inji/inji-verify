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
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
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

    public VerifiablePresentationSubmissionServiceImpl(VPSubmissionRepository vpSubmissionRepository, CredentialsVerifier credentialsVerifier, PresentationVerifier presentationVerifier, VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService, VCVerificationServiceImpl vcVerificationService, PixelPass pixelPass, AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository, Gson gson, Validator validator, ObjectMapper objectMapper) {
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
    }

    /**
     * This method retrieves the authorization request details from the database using the provided state parameter.
     * @param state
     * @return
     */
    public AuthorizationRequestCreateResponse getAuthRequest(String state) {
		return authorizationRequestCreateResponseRepository.findById(state).orElse(null);
	}

    /**
     * This method extracts the VP tokens from the input VP token string.
     * It distinguishes between LDP VP tokens and SD-JWT tokens based on their structure.
     * @param vpTokenString
     * @return
     */
    public DcqlTokensDto extractDcqlTokens(String vpTokenString, AuthorizationRequestResponseDto authRequest) throws InvalidVpTokenException {
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
                .map(cq -> cq.isRequire_cryptographic_holder_binding())
                .orElseGet(() -> {
                    log.warn("No DCQL credential entry found for queryId '{}' in stored VP token; defaulting require_cryptographic_holder_binding to true", queryId);
                    return true;
                });
    }

    /**
     * This method validates the client_id from the VP token against the client_id in the authorization request.
     * @param authRequest
     * @param ldpVpTokens
     * @return
     */
    public boolean isClientIdValid(AuthorizationRequestResponseDto authRequest, Map<String, List<JSONObject>> ldpVpTokens) {
        log.info("Validating client_id from VP token");
        String clientId = authRequest.getClientId();
        if (!StringUtils.hasText(clientId)) {
            log.error("clientId is missing");
            return false;
        }
        // client_id validation is done only for LDP VP tokens since SD-JWT tokens
        // are self-contained and do not have a proof with a domain claim.
        for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
            String queryId = entry.getKey();
            for (JSONObject jsonVPToken : entry.getValue()) {
                log.debug("Processing query ID: {}", queryId);
                JSONObject proof = jsonVPToken.optJSONObject("proof");
                String domain = proof != null ? proof.optString("domain", null) : null;
                log.debug("domain: {}, expected clientId: {}", domain, clientId);
                if (!Objects.equals(clientId, domain)) {
                    log.error(
                            "clientId validation failed for query ID: {}, expected: {}, actual: {}",
                            queryId,
                            clientId,
                            domain
                    );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     *  This method validates the nonce from the VP token against the nonce in the authorization request.
     *  If the authorization request allows accepting VP without holder proof, it skips nonce validation and returns true.
     *  If nonce validation is required, it checks if the nonce in the proof's challenge claim of each LDP VP token matches the nonce in the authorization request.
     *  If any of the tokens fail this validation, it returns false.
     *  For SD-JWT tokens, since they are self-contained and do not have a proof with a challenge claim, nonce validation is not performed on them.
     *
     * @param authRequest
     * @param ldpVpTokens
     * @return
     */
    public boolean isNonceValid(AuthorizationRequestResponseDto authRequest, Map<String, List<JSONObject>> ldpVpTokens) {
        String nonce = authRequest.getNonce();
        if (!StringUtils.hasText(nonce)) {
            log.error("nonce is missing");
            return false;
        }
        // nonce validation is done only for LDP VP tokens since SD-JWT tokens
        // are self-contained and do not have a proof with challenge claim.
        for (Map.Entry<String, List<JSONObject>> entry : ldpVpTokens.entrySet()) {
            String queryId = entry.getKey();
            log.info("Performing nonce validation for query ID {} since require_cryptographic_holder_binding is true", queryId);
            for (JSONObject jsonVPToken : entry.getValue()) {
                log.debug("Processing query ID: {}", queryId);
                JSONObject proof = jsonVPToken.optJSONObject("proof");
                String challenge = proof != null ? proof.optString("challenge", null) : null;
                log.debug("challenge: {}, expected nonce: {}", challenge, nonce);
                if (!Objects.equals(nonce, challenge)) {
                    log.error(
                            "nonce validation failed for query ID: {}, expected: {}, actual: {}",
                            queryId,
                            nonce,
                            challenge
                    );
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method generates a unique response code using UUID if the authorization request requires response code validation. If response code validation is not required, it returns null.
     * @param authRequest
     * @return
     */
    public String generateResponseCode(AuthorizationRequestResponseDto authRequest) {
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
	public Timestamp generateResponseCodeExpiry() {
		log.debug("Generating response code expiry time since response code validation is required");
		Timestamp responseCodeExpiryAt = Timestamp.from(Instant.now().plus(responseCodeExpiryTimeInMins, ChronoUnit.MINUTES));
		return responseCodeExpiryAt;
	}

    /**
     * This method builds the redirect URI by appending the response code as a fragment to the base redirect URI.
     * @param responseCode
     * @return
     */
    public  String buildRedirectUri(String responseCode) {
        if (redirectUri == null || redirectUri.isBlank() || responseCode == null) return null;
        String redirectUriWithResponseCode = UriComponentsBuilder
                    .fromUriString(redirectUri)
                    .fragment("response_code=" + responseCode)
                    .build()
                    .toUriString();
        return redirectUriWithResponseCode;
    }
  
    /**
     * This method is used to persist the VP submission details along with the response code and 
     * its expiry time. 
     * It also invokes the listener to update the status of VP request.
     */
	@Transactional
	public void submitVpToken(AuthorizationRequestResponseDto authRequest, String vpToken, String state, String error,
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
        //log.debug(vpSubmissionRepository.getById(state).getVpToken());
        /// invoke listener to update the status of VP request
		verifiablePresentationRequestService.invokeVpRequestStatusListener(state);

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
                            verifyAndGetCredentialStatus(credential.toString(), verificationResults, CredentialFormat.LDP_VC);
                        }
                    } else {
                        throw new VPWithoutProofException();
                    }
                }
                for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
                    verifyAndGetCredentialStatus(sdJwtVpToken, verificationResults, CredentialFormat.VC_SD_JWT);
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
                        verifyAndGetCredentialStatus(ldpVcString, verificationResults, CredentialFormat.LDP_VC);
                    }
                }
                for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing SD-JWT tokens for query ID: {}", queryId);
                    for (String sdJwtToken : entry.getValue()) {
                        verifyAndGetCredentialStatus(sdJwtToken, verificationResults, CredentialFormat.DC_SD_JWT);
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
                            credentialResults.add(verifyAndGetCredentialStatusV2(request, credential, false));
                        }
                    } else {
                        throw new VPWithoutProofException();
                    }
                }
                for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
                    credentialResults.add(verifyAndGetCredentialStatusV2(request, sdJwtVpToken, true));
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
                        credentialResults.add(verifyAndGetCredentialStatusV2(request, ldpVcString, false));
                    }
                }
                for (Map.Entry<String, List<String>> entry : sdJwtTokens.entrySet()) {
                    String queryId = entry.getKey();
                    log.info("Processing SD-JWT tokens for query ID: {}", queryId);
                    for (String sdJwtToken : entry.getValue()) {
                        credentialResults.add(verifyAndGetCredentialStatusV2(request, sdJwtToken, true));
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
            throw new VPVerificationException();
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

    private void verifyAndGetCredentialStatus(String vc, List<VCResultDto> verificationResults, CredentialFormat credentialFormat) {
        List<String> statusPurposeList = new ArrayList<>();
        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
        CredentialVerificationSummary credentialVerificationSummary = credentialsVerifier.verifyAndGetCredentialStatus(vc, credentialFormat, statusPurposeList);
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
        log.info("authRequest: {}", authRequest);
        if (authRequest != null && authRequest.getAuthorizationDetails() != null && authRequest.getAuthorizationDetails().getPresentationDefinition() !=null
                && authRequest.getAuthorizationDetails().getDcqlQuery() == null) {
            isAuthRequestWithPresentationExchange = true;
            log.info("Authorization request is for presentation exchange");
        }
        return isAuthRequestWithPresentationExchange;
    }

    @Override
    public VPTokenResultDto getVPResult(List<String> requestIds, String transactionId) throws VPSubmissionWalletError,  InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException {
        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
            VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds, null, authRequest, false);
        return processSubmission(vpSubmission, transactionId, authRequest);
    }

    @Override
    public VPVerificationResultDto getVPResultV2(VerificationRequestDto request, List<String> requestIds, String transactionId) {
        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds, null, authRequest, false);
        return processSubmissionV2(request, transactionId, vpSubmission, authRequest);
    }

    @Override
    @Transactional
    public VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, List<String> requestIds, String transactionId) {
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

    private CredentialResultsDto verifyAndGetCredentialStatusV2(VerificationRequestDto request, Object vc, boolean isSdJwt) {
        VCVerificationRequestDto vcVerificationRequestDto = new VCVerificationRequestDto(vc.toString());
        vcVerificationRequestDto.setSkipStatusChecks(request.isSkipStatusChecks());
        vcVerificationRequestDto.setStatusCheckFilters(request.getStatusCheckFilters());
        vcVerificationRequestDto.setIncludeClaims(request.isIncludeClaims());

        VCVerificationResultDto resultDto = vcVerificationService.verifyV2(vcVerificationRequestDto);

        CredentialResultsDto credentialResults = new CredentialResultsDto();
        credentialResults.setVerifiableCredential(vc.toString());
        credentialResults.setAllChecksSuccessful(resultDto.isAllChecksSuccessful());
        credentialResults.setSchemaAndSignatureCheck(resultDto.getSchemaAndSignatureCheck());
        credentialResults.setExpiryCheck(resultDto.getExpiryCheck());
        credentialResults.setStatusCheck(resultDto.getStatusCheck());
        credentialResults.setClaims(resultDto.getClaims());
        if (isSdJwt) {
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
        } else {
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
