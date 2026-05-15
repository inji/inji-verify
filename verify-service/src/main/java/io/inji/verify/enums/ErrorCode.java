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
    DCQL_QUERY_REQUIRED("dcql_query","dcql_query is required"),
    AMBIGUOUS_QUERY("ambiguous_query","Both dcql_query and presentationDefinition provided"),
    DCQL_VALIDATION_ERROR("dcql_validation_error","DCQL structure invalid"),
    DCQL_CREDENTIALS_REQUIRED("dcql_query.credentials","Each DCQL credential entry must not be null."),
    DCQL_CREDENTIALS_INVALID("dcql_query.credentials","dcql_query.credentials must be a non-empty array."),
    DCQL_CREDENTIAL_ID_REQUIRED("dcql_query.credentials","Each DCQL credential entry must contain id."),
    DCQL_CREDENTIAL_FORMAT_REQUIRED("dcql_query.credentials","Each DCQL credential entry must contain format."),
    DCQL_CREDENTIAL_FORMAT_UNSUPPORTED("dcql_query.credentials","Only dc+sd-jwt is supported for DCQL credential format."),
    PRESENTATION_DEFINITION_NOT_SUPPORTED("presentation_definition","presentation_definition is not supported"),
    INVALID_CLAIMS_STRUCTURE("dcql_validation_error","dcql_query.credentials[].claims must contain valid claim path definitions."),
    CLIENT_ID_REQUIRED("invalid_request","client_id is required"),
    DID_CREATION_FAILED("DID_CREATION_FAILED","Error while creating DID document."),
    VP_SUBMISSION_EXCEPTION("VP_SUBMISSION_EXCEPTION","Error while processing VP submission"),
    TOKEN_MATCHING_FAILED("TOKEN_MATCHING_FAILED", "Token matching failed."),
    INVALID_VP_TOKEN("INVALID_VP_TOKEN","Verifiable presentation failed due to invalid VP token"),
    VP_WITHOUT_PROOF("VP_WITHOUT_PROOF", "Invalid VP Submission since VP is without proof"),
    RESPONSE_CODE_NOT_MATCHING("RESPONSE_CODE_NOT_MATCHING", "Response code is not matching the VP Submission response"),
    RESPONSE_CODE_EXPIRED("RESPONSE_CODE_EXPIRED", "Response code has expired"),
    RESPONSE_CODE_USED("RESPONSE_CODE_USED", "Response code has been used"),
    RESPONSE_CODE_NOT_FOUND("RESPONSE_CODE_NOT_FOUND", "Response code is missing for this VP Submission"),
    REDIRECT_URI_NOT_FOUND("REDIRECT_URI_NOT_FOUND", "Redirect URI configuration is missing"),
    VP_SESSION_INVALID("VP_SESSION_INVALID", "Your VP verification session is invalid. Please restart the process and try again"),
    MALFORMED_COOKIE("MALFORMED_COOKIE", "Request cannot be processed due to malformed cookie" ),
    RESPONSE_CODE_NOT_USED("RESPONSE_CODE_NOT_USED", "Transaction was incomplete, response_code was not used" ),
    NONCE_VALIDATION_FAILED("invalid_request", "Nonce validation failed due to invalid nonce"),
    CLIENT_ID_VALIDATION_FAILED("invalid_request", "Client id validation failed due to invalid client id"),
    CLIENT_ID_NONCE_VALIDATION_FAILED("invalid_request", "Client id or nonce validation failed"),
    INTERNAL_SERVER_ERROR("internal_server_error", "Internal server error");

    private final String errorCode;
    private final String errorMessage;
}