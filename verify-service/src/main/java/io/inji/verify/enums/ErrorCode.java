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
    CLIENT_ID_NONCE_VALIDATION_FAILED("invalid_request", "Client id or nonce validation failed");

    private final String errorCode;
    private final String errorMessage;
}