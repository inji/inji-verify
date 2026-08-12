package io.inji.verify.services;

import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import io.inji.verify.dto.VerificationSessionRequestDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.InvalidTransactionIdException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.RedirectUriGenerationException;
import io.inji.verify.exception.ResponseCodeException;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.exception.VPAlreadySubmittedException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPVerificationException;
import io.inji.verify.exception.VPWithoutProofException;
import jakarta.validation.Valid;

public interface VerifiablePresentationSubmissionService {

    /**
     * Submits a Verifiable Presentation (VP) in response to a VP request. Validates the submission and processes it according to the VP request details.
     *
     * @param vpToken          The vp_token parameter from the request, which may be null or empty
     * @param state            The state parameter from the request, which is required and must not be empty
     * @param error            The error parameter from the request, which may be null or empty
     * @param errorDescription The error_description parameter from the request, which may be null or empty
     * @return the response body: empty if no response code was required, or containing a
     * {@code redirect_uri} entry if one was generated.
     * @throws VPRequestValidationException   if any validation step fails (mapped to HTTP 400)
     * @throws RedirectUriGenerationException if a response code was generated but no redirect_uri
     *                                        could be built (mapped to HTTP 500)
     * @throws VPAlreadySubmittedException    if a submission already exists for this state (mapped to HTTP 400)
     * @throws InvalidVpTokenException        if the vp_token cannot be decomposed into DCQL tokens (mapped to HTTP 400)
     */
    Map<String, Object> submitVerifiablePresentation(String vpToken, String state, String error, String errorDescription)
            throws VPRequestValidationException, RedirectUriGenerationException, VPAlreadySubmittedException, InvalidVpTokenException;

    /**
     * Resolves a transactionId to its VP submission result. Looks up the request IDs for the
     * transactionId internally; if none are found, falls back to checking for a VC submission
     * result before giving up.
     *
     * @param transactionId The transaction ID associated with the VP submission
     * @return a {@link io.inji.verify.dto.submission.VPTokenResultDto} if a VP submission was
     * found, or a {@link io.inji.verify.dto.submission.VCSubmissionVerificationStatusDto} if a VC
     * submission was found instead (no common supertype between the two, hence {@code Object})
     * @throws InvalidTransactionIdException  Thrown if the transactionId resolves to neither a VP
     *                                        request nor a VC submission
     * @throws VPSubmissionWalletError        Thrown if there is an error in the wallet during VP submission
     * @throws CredentialStatusCheckException Thown if there is an error checking the status of the credential
     * @throws VPWithoutProofException        Thown if the VP submission does not contain the required proof
     * @throws VPSubmissionNotFoundException  Thown if the VP submission is not found for the given request IDs and transaction ID
     * @throws ResponseCodeException          Thrown if there is an error in generating or processing the response code
     */
    Object getVPResult(String transactionId) throws InvalidTransactionIdException, VPSubmissionWalletError, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException;

    /**
     * Resolves a transactionId to its VP submission result. Looks up the request IDs for the
     * transactionId internally.
     *
     * @param request       The VerificationRequestDto containing the details of the VP submission request
     * @param transactionId The transaction ID associated with the VP submission
     * @return The VPVerificationResultDto containing the result of the VP submission
     * @throws InvalidTransactionIdException Thrown if the transactionId resolves to no VP request
     * @throws VPSubmissionNotFoundException Thrown if no VP submission exists for the resolved request IDs
     * @throws ResponseCodeException         Thrown if there is an error validating the response code
     * @throws VPSubmissionWalletError       Thrown if the submission recorded a wallet error
     * @throws TokenMatchingFailedException  Thrown if the vp_token does not match the authorization request (Presentation Exchange flow)
     * @throws InvalidVpTokenException       Thrown if the vp_token is structurally invalid
     * @throws VPWithoutProofException       Thrown if the VP submission does not contain the required holder proof
     * @throws VPVerificationException       Thrown if verification fails for any other unexpected reason
     */
    VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException;

    /**
     * Resolves a transactionId (from the session cookie) to its VP submission result. Looks up
     * the request IDs for the transactionId internally.
     *
     * @param request       The VerificationSessionRequestDto containing the details of the VP submission request
     * @param transactionId The transaction ID associated with the VP submission
     * @return The VPVerificationResultDto containing the result of the VP submission
     * @throws InvalidTransactionIdException Thrown if the transactionId resolves to no VP request
     * @throws VPSubmissionNotFoundException Thrown if no VP submission exists for the resolved request IDs
     * @throws ResponseCodeException         Thrown if there is an error validating the response code
     * @throws VPSubmissionWalletError       Thrown if the submission recorded a wallet error
     * @throws TokenMatchingFailedException  Thrown if the vp_token does not match the authorization request (Presentation Exchange flow)
     * @throws InvalidVpTokenException       Thrown if the vp_token is structurally invalid
     * @throws VPWithoutProofException       Thrown if the VP submission does not contain the required holder proof
     * @throws VPVerificationException       Thrown if verification fails for any other unexpected reason
     */
    @Transactional
    VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException;

}
