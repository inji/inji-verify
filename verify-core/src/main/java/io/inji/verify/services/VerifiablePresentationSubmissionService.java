package io.inji.verify.services;

import java.util.Map;
import java.util.Optional;

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
     * Submits a Verifiable Presentation (VP) in response to a VP request. Validates the submission
     * and processes it according to the stored authorization request ({@code response_mode}).
     * Uses the same pipeline for {@code direct_post} and {@code dc_api}.
     *
     * @param vpToken          The vp_token parameter from the request, which may be null or empty
     * @param state            The state / requestId parameter from the request, which is required and must not be empty
     * @param error            The error parameter from the request, which may be null or empty
     * @param errorDescription The error_description parameter from the request, which may be null or empty
     * @param submissionOrigin Raw Origin/Referer from the web layer (required for DC API sessions)
     * @return the response body: empty if no response code was required, or containing a
     * {@code redirect_uri} entry if one was generated. For {@code dc_api}, typically empty
     * (HTTP 200 with no body).
     */
    Map<String, Object> submitVerifiablePresentation(
            String vpToken,
            String state,
            String error,
            String errorDescription,
            Optional<String> submissionOrigin)
            throws VPRequestValidationException, RedirectUriGenerationException, VPAlreadySubmittedException, InvalidVpTokenException;

    /**
     * Resolves a transactionId to its VP submission result. Looks up the request IDs for the
     * transactionId internally; if none are found, falls back to checking for a VC submission
     * result before giving up.
     */
    Object getVPResult(String transactionId) throws InvalidTransactionIdException, VPSubmissionWalletError, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException, ResponseCodeException;

    /**
     * Resolves a transactionId to its VP submission result. Looks up the request IDs for the
     * transactionId internally.
     */
    VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException;

    /**
     * Resolves a transactionId (from the session cookie) to its VP submission result.
     */
    @Transactional
    VPVerificationResultDto getVPSessionResults(VerificationSessionRequestDto request, String transactionId) throws InvalidTransactionIdException, VPSubmissionNotFoundException, ResponseCodeException, VPSubmissionWalletError, TokenMatchingFailedException, InvalidVpTokenException, VPWithoutProofException, VPVerificationException;
}
