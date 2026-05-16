
package io.inji.verify.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_TRANSACTION_ID("INVALID_TRANSACTION_ID","Invalid transaction ID, No requests found for given transaction ID."),
    NO_VP_SUBMISSION("NO_VP_SUBMISSION","No VP submission found for given transaction ID."),
    NO_AUTH_REQUEST("NO_AUTH_REQUEST","No Authorization request found for given request ID."),
    BOTH_ID_AND_PD_CANNOT_BE_NULL("BOTH_ID_AND_PD_CANNOT_BE_NULL","Both Presentation Definition and Presentation Definition ID cannot be empty."),
    NO_PRESENTATION_DEFINITION("NO_PRESENTATION_DEFINITION","No Presentation Definition found for given Presentation Definition ID."),
    DID_CREATION_FAILED("DID_CREATION_FAILED","Error while creating DID document."),
    VP_SUBMISSION_EXCEPTION("VP_SUBMISSION_EXCEPTION","Error while processing VP submission"),
    TOKEN_MATCHING_FAILED("TOKEN_MATCHING_FAILED", "Token matching failed."),
    INVALID_VP_TOKEN("INVALID_VP_TOKEN","Verifiable Presentation Submission failed, due to invalid VP token"),
    VP_WITHOUT_PROOF("VP_WITHOUT_PROOF", "Invalid VP Submission since VP is without proof"),
    RESPONSE_CODE_NOT_MATCHING("RESPONSE_CODE_NOT_MATCHING", "Response code is not matching the VP Submission response"),
    RESPONSE_CODE_EXPIRED("RESPONSE_CODE_EXPIRED", "Response code has expired"),
    RESPONSE_CODE_USED("RESPONSE_CODE_USED", "Response code has been used"),
    RESPONSE_CODE_NOT_FOUND("RESPONSE_CODE_NOT_FOUND", "Response code is missing for this VP Submission"),
    REDIRECT_URI_NOT_FOUND("REDIRECT_URI_NOT_FOUND", "Redirect URI configuration is missing"),
    VP_SESSION_INVALID("VP_SESSION_INVALID", "Your VP verification session is invalid. Please restart the process and try again"),
    MALFORMED_COOKIE("MALFORMED_COOKIE", "Request cannot be processed due to malformed cookie" ),
    RESPONSE_CODE_NOT_USED("RESPONSE_CODE_NOT_USED", "Transaction was incomplete, response_code was not used" ),
    NONCE_VALIDATION_FAILED("invalid_request", "Nonce validation failed due to invalid nonce/challenge."),
    CLIENT_ID_VALIDATION_FAILED("invalid_request", "Client id validation failed due to invalid client_id/domain."),
    CLIENT_ID_NONCE_VALIDATION_FAILED("invalid_request", "Client id or nonce validation failed"),
    INVALID_STATE_MISSING("invalid_request", "State parameter is required and cannot be empty."),
    NO_MATCHING_VP_REQUEST("invalid_request", "Invalid state parameter: No matching VP request found."),
    VP_REQUEST_EXPIRED("invalid_request", "Invalid state parameter: VP request has expired."),
    VP_ALREADY_SUBMITTED("invalid_request", "VP response has already been submitted."),
    EITHER_VP_TOKEN_OR_ERROR_REQUIRED("invalid_request", "Either vp_token or error must be provided."),
    BOTH_VP_TOKEN_AND_ERROR_NOT_ALLOWED("invalid_request", "Both vp_token and error cannot be provided together."),
    ERROR_DESCRIPTION_VP_TOKEN_CONFLICT("invalid_request", "If error_description is provided, vp_token must be null."),
    ERROR_DESCRIPTION_ERROR_REQUIRED("invalid_request", "If error_description is provided, error must also be provided."),
    VP_TOKEN_REQUIRED("invalid_request", "invalid_vp_token: vp_token is required and cannot be empty or 'null'"),
    DUPLICATE_QUERY_IDS_NOT_ALLOWED("invalid_request", "invalid_vp_token: duplicate query ids are not allowed"),
    VP_TOKEN_MUST_HAVE_KEY_VALUE_PAIR("invalid_request", "invalid_vp_token: must contain at least one key-value pair"),
    VP_TOKEN_VALUES_MUST_BE_ARRAYS("invalid_request", "invalid_vp_token: all values must be arrays"),
    VP_TOKEN_ARRAYS_MUST_HAVE_ELEMENTS("invalid_request", "invalid_vp_token: all arrays must have at least one element"),
    VP_TOKEN_ARRAY_ELEMENTS_INVALID("invalid_request", "invalid_vp_token: array elements must be non-empty JSON objects or non-empty SD-JWT strings"),
    VP_TOKEN_ALL_ELEMENTS_MUST_BE_OBJECTS("invalid_request", "invalid_vp_token: all elements must be non-empty JSON objects"),
    VP_TOKEN_ALL_ELEMENTS_MUST_BE_SD_JWT("invalid_request", "invalid_vp_token: all elements must be non-empty SD-JWT strings"),
    VP_TOKEN_NOT_VALID_JSON_OBJECT("invalid_request", "invalid_vp_token, not a valid JSON object");

    private final String errorCode;
    private final String errorMessage;
}