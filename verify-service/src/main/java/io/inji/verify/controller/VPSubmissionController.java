package io.inji.verify.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.inji.verify.dto.result.DcqlVPTokenDto;
import io.inji.verify.exception.InvalidVpTokenException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class VPSubmissionController {

	// Services for handling VP requests and submissions
	final VerifiablePresentationRequestService verifiablePresentationRequestService;
	final VerifiablePresentationSubmissionService verifiablePresentationSubmissionService;

	// Constructor injection for services
	public VPSubmissionController(VerifiablePresentationRequestService verifiablePresentationRequestService,
			VerifiablePresentationSubmissionService verifiablePresentationSubmissionService) {
		this.verifiablePresentationRequestService = verifiablePresentationRequestService;
		this.verifiablePresentationSubmissionService = verifiablePresentationSubmissionService;
	}

	private static final Set<String> ALLOWED_PARAMS = Set.of("vp_token", "state", "error", "error_description");

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
	@PostMapping(path = Constants.VP_RESPONSE_SUBMISSION_URI, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<?> submitVP(@RequestParam(value = "vp_token", required = false) String vpToken,
			@RequestParam(value = "state", required = true) String state,
			@RequestParam(value = "error", required = false) String error,
			@RequestParam(value = "error_description", required = false) String errorDescription,
			HttpServletRequest request) {

		// Log incoming request parameters
		log.debug("Received VP submission with state: {}, error: {}, error_description: {}", state, error,
				errorDescription);
		if (StringUtils.hasText(vpToken)) {
			log.debug("Received VP submission with vp_token length: {}", vpToken.length());
		}

		// --- 1. Validate request parameters ---
		ResponseEntity<?> requestValidation = validateRequest(vpToken, error, errorDescription, request);
		if (requestValidation != null) {
			return requestValidation;
		}
		// --- 2. Validate state parameter and retrieve current VP request status ---
		ResponseEntity<?> stateValidation = validateState(state);
		if (stateValidation != null) {
			return stateValidation;
		}
		// --- 3. Validate vp_token structure if present ---
		if (StringUtils.hasText(vpToken)) {
			ResponseEntity<?> structureValidation = validateVPTokenStructure(vpToken);
			if (structureValidation != null) {
				return structureValidation;
			}
		}
		log.debug("Request parameters validated successfully for state: {}", state);
		// ---- 5. Validate against the Authorization Request

		AuthorizationRequestCreateResponse authRequestCreateResponse = verifiablePresentationSubmissionService
				.getAuthRequest(state);
		if (authRequestCreateResponse == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.NO_MATCHING_VP_REQUEST));
		}
		log.debug("authRequestCreateResponse is {}", authRequestCreateResponse);

		// ---- 5. Validate against the DCQL if vp_token is present

		// TODO

		// ---- 6. Extract DCQL VP tokens from the vp_token string
        DcqlVPTokenDto dcqlVPTokenDto = null;
        if (StringUtils.hasText(vpToken)) {
            try {
                dcqlVPTokenDto = verifiablePresentationSubmissionService.extractDcqlVpTokens(vpToken);
            } catch (InvalidVpTokenException ex) {
                log.error("Invalid VP token structure for state {}", state);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorDto("invalid_vp_token", "The vp_token structure is invalid: " + ex.getMessage()));
            }
        }
        // ---- 7. Validate client_id and none if vp_token is present
        if (dcqlVPTokenDto != null && dcqlVPTokenDto.getLdpVpTokens() != null && !dcqlVPTokenDto.getLdpVpTokens().isEmpty()) {
            ResponseEntity<?> clientIdNonceValidation = validateClientIdNonce(dcqlVPTokenDto.getLdpVpTokens(), authRequestCreateResponse);
            if (clientIdNonceValidation != null) {
                return clientIdNonceValidation;
            }
        } else {
            log.debug("Skipping client_id and nonce validation as no LdpVpTokens extracted for state {}", state);
        }
        // ---- 8. generate response_code and build redirect_uri as required
		Map<String, Object> response = new HashMap<>();
		String responseCode = verifiablePresentationSubmissionService
				.generateResponseCode(authRequestCreateResponse.getAuthorizationDetails());
		Timestamp responseCodeExpiryAt = null;
		if (responseCode != null) {
			log.debug("Generated response code {} for state {}", responseCode, state);
			responseCodeExpiryAt = verifiablePresentationSubmissionService.generateResponseCodeExpiry();
			String redirectUriWithResponseCode = verifiablePresentationSubmissionService.buildRedirectUri(responseCode);
			log.debug("Built redirect URI with response code for state {}: {}", state, redirectUriWithResponseCode);
			if (redirectUriWithResponseCode == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(new ErrorDto(ErrorCode.REDIRECT_URI_NOT_FOUND));
			}
			response.put("redirect_uri", redirectUriWithResponseCode);
		}
		// ---- 9. If all validations pass, proceed with VP submission processing
		try {
			verifiablePresentationSubmissionService.submitVpToken(authRequestCreateResponse.getAuthorizationDetails(),
					vpToken, state, error, errorDescription, responseCode, responseCodeExpiryAt);
		} catch (VPAlreadySubmittedException e) {
			log.debug("VP submission already exists for state {}: {}", state, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.VP_ALREADY_SUBMITTED));
		}
		// --- 10. Return success response with redirect URI if generated
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	private ResponseEntity<?> validateClientIdNonce(Map<String, JSONObject> ldpVpTokens, AuthorizationRequestCreateResponse authRequest) {
        boolean isClientIdValid = verifiablePresentationSubmissionService
                .isClientIdValid(authRequest.getAuthorizationDetails(), ldpVpTokens);
        if (!isClientIdValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDto(ErrorCode.CLIENT_ID_VALIDATION_FAILED));
        }
        boolean isNonceValid = verifiablePresentationSubmissionService
                .isNonceValid(authRequest.getAuthorizationDetails(), ldpVpTokens);
        if (!isNonceValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.NONCE_VALIDATION_FAILED));
        }
		return null; // Return null if both client_id and nonce are valid, indicating the request can
						// proceed to the next validation steps
	}

	/**
	 * Validates the incoming request parameters for VP submission according to the
	 * 
	 * @param vpToken          - The vp_token parameter from the request, which may
	 *                         be null or empty
	 * @param error            - The error parameter from the request, which may be
	 *                         null or empty
	 * @param errorDescription - The error_description parameter from the request,
	 *                         which may be null or empty
	 * @param request          - The HttpServletRequest object containing the
	 *                         request parameters to be validated
	 * @return ResponseEntity with appropriate error if validation fails, or null if
	 *         all validations pass and the request parameters are considered valid
	 */
	private ResponseEntity<?> validateRequest(String vpToken, String error, String errorDescription,
			HttpServletRequest request) {
		// Validate that only allowed parameters are present in the request
		Map<String, String[]> params = request.getParameterMap();
		for (String key : params.keySet()) {
			if (!ALLOWED_PARAMS.contains(key)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorDto("invalid_request", "Invalid parameter: " + key));
			}
		}
		// Validation: Either vp_token or error must be provided
		if (!StringUtils.hasText(vpToken) && !StringUtils.hasText(error) && !StringUtils.hasText(errorDescription)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.EITHER_VP_TOKEN_OR_ERROR_REQUIRED));
		}
		// Validation: Both vp_token and error cannot be provided together
		if (StringUtils.hasText(vpToken) && StringUtils.hasText(error)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.BOTH_VP_TOKEN_AND_ERROR_NOT_ALLOWED));
		}
		// Validation: If error_description is provided, vp_token must be null
		if (StringUtils.hasText(errorDescription) && StringUtils.hasText(vpToken)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.ERROR_DESCRIPTION_VP_TOKEN_CONFLICT));
		}
		// Validation: If error_description is provided, error must also be provided
		if (StringUtils.hasText(errorDescription) && !StringUtils.hasText(error)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.ERROR_DESCRIPTION_ERROR_REQUIRED));
		}
		return null; // Return null if all validations pass, indicating the request parameters are
		// valid
	}

	/**
	 * Validates the state parameter by checking the following:
	 * 
	 * @param state - The state parameter to be validated
	 * @return ResponseEntity with appropriate error if validation fails, or null if
	 *         validation
	 */
	private ResponseEntity<?> validateState(String state) {
		// Validation: State parameter must not be empty
		if (!StringUtils.hasText(state)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.INVALID_STATE_MISSING));
		}

		// Retrieve current VP request status by state
		VPRequestStatusDto currentVPRequestStatusDto = verifiablePresentationRequestService
				.getCurrentRequestStatus(state);
		if (currentVPRequestStatusDto == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.NO_MATCHING_VP_REQUEST));
		}
		log.debug("Current VP request status for state {}: {}", state, currentVPRequestStatusDto.getStatus());
		// Validation: VP request must be in a valid state to accept submissions (e.g., not expired, not already
		if (currentVPRequestStatusDto.getStatus().equals(VPRequestStatus.EXPIRED)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.VP_REQUEST_EXPIRED));
		}
		// Validation: VP request must not have already received a submission
		if (currentVPRequestStatusDto.getStatus().equals(VPRequestStatus.VP_SUBMITTED)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.VP_ALREADY_SUBMITTED));
		}
		return null; // Return null if state is valid, indicating the request can proceed to the next
		// validation steps
	}

	/**
	 * Validates the structure of the vp_token according to the following rules:
	 * 
	 * @param vpToken - The vp_token string to be validated
	 * @return - ResponseEntity with appropriate error if validation fails, or null
	 *         if validation passes
	 */
	private ResponseEntity<?> validateVPTokenStructure(String vpToken) {

		// Validation: vp_token must not be 'null' if present
		if (vpToken.trim().equalsIgnoreCase("null")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto(ErrorCode.VP_TOKEN_REQUIRED));
		}

		// Validation: vp_token must be a valid JSON object with specific structure if
		// present
		try {
			// Parse the vp_token as a JSON object
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode node = objectMapper.readTree(vpToken);

			// Validation: vp_token must be a JSON object if present
			if (!node.isObject()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorDto(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT));
			}
			// Validation: vp_token must contain at least one key-value pair if present
			if (node.size() < 1) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorDto(ErrorCode.VP_TOKEN_MUST_HAVE_KEY_VALUE_PAIR));
			}
			// Validation: All values in vp_token must be arrays if present
			for (JsonNode value : node) {
				if (!value.isArray()) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new ErrorDto(ErrorCode.VP_TOKEN_VALUES_MUST_BE_ARRAYS));
				}
			}
			// Validation: All arrays in vp_token must have at least one element, and all
			// elements must be same type
			for (JsonNode value : node) {
				if (!value.isArray() || value.size() < 1) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new ErrorDto(ErrorCode.VP_TOKEN_ARRAYS_MUST_HAVE_ELEMENTS));
				}
				JsonNode firstElement = value.get(0);
				boolean isFirstJson = firstElement.isObject() && firstElement.size() > 0;
				boolean isFirstSdJwt = firstElement.isTextual() && !firstElement.asText().isEmpty();
				if (!isFirstJson && !isFirstSdJwt) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(new ErrorDto(ErrorCode.VP_TOKEN_ARRAY_ELEMENTS_INVALID));
				}
				for (JsonNode element : value) {
					if (isFirstJson) {
						if (!element.isObject() || element.size() == 0) {
							return ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new ErrorDto(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_OBJECTS));
						}
					} else if (isFirstSdJwt) {
						if (!element.isTextual() || element.asText().isEmpty()) {
							return ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new ErrorDto(ErrorCode.VP_TOKEN_ALL_ELEMENTS_MUST_BE_SD_JWT));
						}
					}
				}
			}
			// check for duplicate query IDs at the outermost level
			boolean isValid = validateDuplicateQueryIds(vpToken);
			if (!isValid) {
				log.debug("Duplicate query ids found in vp_token: {}", vpToken);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ErrorDto(ErrorCode.DUPLICATE_QUERY_IDS_NOT_ALLOWED));
			}
			
		} catch (IllegalArgumentException | IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorDto(ErrorCode.VP_TOKEN_NOT_VALID_JSON_OBJECT));
		}
		return null; // Return null if all validations pass, indicating the structure is valid
	}

	/**
	 * Validates that there are no duplicate query ids at the outermost level of the
	 * 
	 * @param vpToken - The vp_token string to be validated for duplicate query ids
	 * @return true if no duplicate query ids are found, false if duplicate query
	 *         ids are found
	 * @throws IOException
	 * @throws JsonParseException
	 */
	private boolean validateDuplicateQueryIds(String vpToken) throws IOException, JsonParseException {
		// Use Jackson's streaming API to efficiently parse the JSON and check for
		// duplicate keys at the outermost level
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(vpToken);
		try {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				return false; // Not a JSON object
			}
			Set<String> seenKeys = new java.util.HashSet<>();
			while (parser.nextToken() == JsonToken.FIELD_NAME) {
				String fieldName = parser.currentName();
				if (!seenKeys.add(fieldName.trim().toLowerCase())) {
					return false; // Duplicate key found
				}
				parser.nextToken(); // Move to value
				parser.skipChildren();
			}
			return true; // No duplicates found
		} finally {
			parser.close();
		}
	}

}
