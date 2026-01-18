package io.inji.verify.services.impl;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.result.VPVerificationResultDto;
import io.inji.verify.dto.result.VerificationRequestDto;
import io.inji.verify.dto.result.HolderProofCheckDto;
import io.inji.verify.dto.result.CredentialResultsDto;
import io.inji.verify.dto.result.VerificationSummary;
import io.inji.verify.dto.result.VCResultDto;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.VPSubmissionWalletError;
import io.inji.verify.exception.VPWithoutProofException;
import io.inji.verify.exception.TokenMatchingFailedException;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.InvalidVpTokenException;
import io.inji.verify.exception.VPSubmissionNotFoundException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.repository.VPSubmissionRepository;
import io.inji.verify.services.VerifiablePresentationSubmissionService;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.PresentationVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static io.inji.verify.utils.Utils.isSdJwt;
import static io.inji.verify.utils.Utils.populateStatusCheck;
import static io.inji.verify.utils.Utils.populateAllChecksSuccessful;

@Service
@Slf4j
public class VerifiablePresentationSubmissionServiceImpl implements VerifiablePresentationSubmissionService {

    final VPSubmissionRepository vpSubmissionRepository;
    final CredentialsVerifier credentialsVerifier;
    final PresentationVerifier presentationVerifier;
    final VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService;
    final VCVerificationServiceImpl vcVerificationService;

    public VerifiablePresentationSubmissionServiceImpl(VPSubmissionRepository vpSubmissionRepository, CredentialsVerifier credentialsVerifier, PresentationVerifier presentationVerifier, VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService, VCVerificationServiceImpl vcVerificationService) {
        this.vpSubmissionRepository = vpSubmissionRepository;
        this.credentialsVerifier = credentialsVerifier;
        this.presentationVerifier = presentationVerifier;
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
        this.vcVerificationService = vcVerificationService;
    }

    @Override
    public void submit(VPSubmissionDto vpSubmissionDto) {
        vpSubmissionRepository.save(new VPSubmission(vpSubmissionDto.getState(), vpSubmissionDto.getVpToken(), vpSubmissionDto.getPresentationSubmission(), vpSubmissionDto.getError(), vpSubmissionDto.getErrorDescription()));
        verifiablePresentationRequestService.invokeVpRequestStatusListener(vpSubmissionDto.getState());
    }

    private VPTokenResultDto processSubmission(VPSubmission vpSubmission, String transactionId) throws VPSubmissionWalletError, CredentialStatusCheckException, VPWithoutProofException {
        log.info("Processing VP submission");

        List<VCResultDto> verificationResults = new ArrayList<>();
        List<VPVerificationStatus> vpVerificationStatuses = new ArrayList<>();

        try {
            Optional<String> error = Optional.ofNullable(vpSubmission.getError()).filter(e -> !e.isEmpty());
            if (error.isPresent()) {
                log.info("VP submission from wallet has error");
                throw new VPSubmissionWalletError(vpSubmission.getError(), vpSubmission.getErrorDescription());
            }
            AuthorizationRequestCreateResponse request = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);

            log.info("Processing VP token matching");
            checkVPTokenMatching(vpSubmission, request);

            List<JSONObject> jsonVpTokens = new ArrayList<>();
            List<String> sdJwtVpTokens = new ArrayList<>();

            extractTokens(vpSubmission.getVpToken(), jsonVpTokens, sdJwtVpTokens);

            log.info("Processing VP verification");
            log.debug("Number of VP tokens to verify: {}", jsonVpTokens.size() + ":" + sdJwtVpTokens.size());

            if (jsonVpTokens.isEmpty() && sdJwtVpTokens.isEmpty()) {
                throw new InvalidVpTokenException();
            }

            for (JSONObject vpToken : jsonVpTokens) {
                boolean isVerifiablePresentation = isVerifiablePresentation(vpToken);
                boolean isVerifiablePresentationSigned =  isVerifiablePresentationSigned(vpToken);
                boolean acceptVPWithoutHolderProof = Optional.ofNullable(request.getAuthorizationDetails()).map(AuthorizationRequestResponseDto::isAcceptVPWithoutHolderProof).orElse(false);

                if (isVerifiablePresentation) {
                    if (isVerifiablePresentationSigned) {
                        List<String> statusPurposeList = new ArrayList<>();
                        statusPurposeList.add(Constants.STATUS_PURPOSE_REVOKED);
                        PresentationResultWithCredentialStatus presentationResultWithCredentialStatus = presentationVerifier.verifyAndGetCredentialStatus(vpToken.toString(), statusPurposeList);
                        VPVerificationStatus proofVerificationStatus = presentationResultWithCredentialStatus.getProofVerificationStatus();
                        vpVerificationStatuses.add(proofVerificationStatus);

                        List<VCResultDto> vcResults = new ArrayList<>();
                        for (var vcResult : presentationResultWithCredentialStatus.getVcResults()) {
                            VerificationStatus vcStatus = Utils.applyRevocationStatus(vcResult.getStatus(), vcResult.getCredentialStatus());
                            vcResults.add(new VCResultDto(vcResult.getVc(), vcStatus));
                        }
                        verificationResults.addAll(vcResults);
                    } else if (acceptVPWithoutHolderProof) {
                        Object verifiableCredential = vpToken.opt("verifiableCredential");
                        if (verifiableCredential instanceof JSONArray array) {
                            for (Object vc : array) {
                                addVerificationResults(vc.toString(), verificationResults, CredentialFormat.LDP_VC);
                            }
                        } else {
                            throw new InvalidVpTokenException();
                        }
                    } else {
                        throw new VPWithoutProofException();
                    }
                } else {
                    throw new InvalidVpTokenException();
                }
            }

            for (String sdJwtVpToken : sdJwtVpTokens) {
                addVerificationResults(sdJwtVpToken, verificationResults, CredentialFormat.VC_SD_JWT);
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

    private void addVerificationResults(String vc, List<VCResultDto> verificationResults, CredentialFormat  credentialFormat) throws CredentialStatusCheckException{
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

    private boolean isVerifiablePresentation(JSONObject vpToken) {
        Object types = vpToken.opt("type");
        if (types == null) return false;

        return switch (types) {
            case JSONArray jsonTypes -> jsonTypes.toList().stream()
                    .anyMatch(type -> "VerifiablePresentation".equalsIgnoreCase(type.toString()));
            case String typeString ->
                    "VerifiablePresentation".equalsIgnoreCase(typeString);
            default -> false;
        };
    }

    private boolean isVerifiablePresentationSigned(JSONObject vpToken) {
        Object proof = vpToken.opt("proof");
        return proof != null;
    }

    void extractTokens(String vpTokenString, List<JSONObject> jsonVpTokens, List<String> sdJwtVpTokens) {
        if (vpTokenString == null) return;

        Object vpTokenRaw = new JSONTokener(vpTokenString).nextValue();

        if (vpTokenRaw instanceof JSONArray array) {
            IntStream.range(0, array.length()).forEach(i -> processSingleToken(array.get(i), jsonVpTokens, sdJwtVpTokens));
        } else {
            processSingleToken(vpTokenRaw, jsonVpTokens, sdJwtVpTokens);
        }
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

    @Override
    public VPTokenResultDto getVPResult(List<String> requestIds, String transactionId) throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException, VPWithoutProofException {
        List<VPSubmission> vpSubmissions = vpSubmissionRepository.findAllById(requestIds);

        if (vpSubmissions.isEmpty()) {
            throw new VPSubmissionNotFoundException();
        }
        VPSubmission vpSubmission = vpSubmissions.getFirst();
        return processSubmission(vpSubmission, transactionId);
    }

    private boolean isVPTokenMatching(VPSubmission vpSubmission, AuthorizationRequestCreateResponse request) {
        Object vpTokenRaw = new JSONTokener(vpSubmission.getVpToken()).nextValue();
        List<DescriptorMapDto> descriptorMap = vpSubmission.getPresentationSubmission().getDescriptorMap();

        if (vpTokenRaw == null || request == null || descriptorMap == null || descriptorMap.isEmpty()) {
            log.info("Unable to perform token matching");
            return false;
        }

        log.info("VP token matching done");
        return true;
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

    @Override
    public VPVerificationResultDto getDetailVPResult(@Valid VerificationRequestDto request, List<String> requestIds, String transactionId) throws VPSubmissionWalletError, TokenMatchingFailedException, VPWithoutProofException, VPSubmissionNotFoundException {
        VPSubmission vpSubmission = getVpSubmission(requestIds);
        AuthorizationRequestCreateResponse authorizationRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);
        checkVPTokenMatching(vpSubmission, authorizationRequest);

        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();
        extractTokens(vpSubmission.getVpToken(), jsonVpTokens, sdJwtVpTokens);

        if (jsonVpTokens.isEmpty() && sdJwtVpTokens.isEmpty()) throw new InvalidVpTokenException();

        List<CredentialResultsDto> credentialResults = new ArrayList<>();

        for (JSONObject vpToken : jsonVpTokens) {
            credentialResults.addAll(verifyJsonVPToken(request, vpToken, authorizationRequest));
        }

        for (String sdJwtVpToken : sdJwtVpTokens) {
            credentialResults.add(verifyCredential(request, sdJwtVpToken));
        }

        boolean allChecksSuccessful = credentialResults.stream().allMatch(CredentialResultsDto::isAllChecksSuccessful);
        return new VPVerificationResultDto(transactionId, allChecksSuccessful, credentialResults);
    }

    private List<CredentialResultsDto> verifyJsonVPToken(VerificationRequestDto request, JSONObject vpToken, AuthorizationRequestCreateResponse authorizationRequest) throws VPWithoutProofException {
        if (!isVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();

        boolean isSigned = isVerifiablePresentationSigned(vpToken);
        boolean acceptVPWithoutHolderProof = Optional.ofNullable(authorizationRequest.getAuthorizationDetails()).map(AuthorizationRequestResponseDto::isAcceptVPWithoutHolderProof).orElse(false);

        if (isSigned) {
            return processSignedVPToken(vpToken, request);
        } else if (acceptVPWithoutHolderProof) {
            return processUnsignedVPToken(request, vpToken);
        } else {
            throw new VPWithoutProofException();
        }
    }

    private List<CredentialResultsDto> processUnsignedVPToken(VerificationRequestDto request, JSONObject vpToken) {
        List<CredentialResultsDto> results = new ArrayList<>();
        Object verifiableCredential = vpToken.opt("verifiableCredential");

        if (verifiableCredential instanceof JSONArray array) {
            for (Object vc : array) {
                results.add(verifyCredential(request, vc));
            }
        } else {
            throw new InvalidVpTokenException();
        }
        return results;
    }

    private List<CredentialResultsDto> processSignedVPToken(JSONObject vpToken, @Valid VerificationRequestDto request) {
        List<VerificationSummary> verificationSummaryList = verifySignedPresentation(vpToken, request);
        List<CredentialResultsDto> results = new ArrayList<>();

        for (VerificationSummary verificationSummary : verificationSummaryList) {
        List<StatusCheckDto> statusCheck = populateStatusCheck(verificationSummary.getCredentialStatus());
        HolderProofCheckDto holderProofCheck = populateHolderProof(verificationSummary.getProofVerificationStatus());
        SchemaAndSignatureCheckDto schemaAndSignatureCheck = populateSchemaAndSignatureCheckDto(verificationSummary.getVerificationStatus());
        ExpiryCheckDto expiryCheck = schemaAndSignatureCheck.isValid() ? populateExpiryCheck(verificationSummary.getVerificationStatus()) : null;

        boolean allChecksSuccessful = populateAllChecksSuccessful(schemaAndSignatureCheck, expiryCheck, statusCheck, holderProofCheck);
        results.add(populateCredentialResultDto(verificationSummary.getCredential(), holderProofCheck, allChecksSuccessful, schemaAndSignatureCheck, expiryCheck, statusCheck, null));
        }

        return results;
    }

    private SchemaAndSignatureCheckDto populateSchemaAndSignatureCheckDto(VerificationStatus verificationStatus) {
        return verificationStatus.equals(VerificationStatus.INVALID)
                ? new SchemaAndSignatureCheckDto(false, null)
                : new SchemaAndSignatureCheckDto(true, null);
    }

    private List<VerificationSummary> verifySignedPresentation(JSONObject vpToken, VerificationRequestDto request) {

        if (request.isSkipStatusChecks()) {
            PresentationVerificationResult result = presentationVerifier.verify(vpToken.toString());
            return result.getVcResults().stream()
                    .map(vcRes -> new VerificationSummary(
                            result.getProofVerificationStatus(),
                            vcRes.getVc(),
                            vcRes.getStatus(),
                            null))
                    .collect(Collectors.toList());
        } else {
            List<String> filters = request.getStatusCheckFilters();
            PresentationResultWithCredentialStatus result = presentationVerifier.verifyAndGetCredentialStatus(vpToken.toString(), filters);
            return result.getVcResults().stream()
                    .map(vcRes -> new VerificationSummary(
                            result.getProofVerificationStatus(),
                            vcRes.getVc(),
                            vcRes.getStatus(),
                            vcRes.getCredentialStatus()))
                    .collect(Collectors.toList());
        }
    }

    private CredentialResultsDto verifyCredential(VerificationRequestDto request, Object vc) {
        VCVerificationRequestDto vcVerificationRequestDto = getVcVerificationRequestDto(request, vc);
        VCVerificationResultDto resultDto = vcVerificationService.verifyV2(vcVerificationRequestDto);

        List<StatusCheckDto> statusCheck = resultDto.getStatusCheck();
        SchemaAndSignatureCheckDto schemaAndSignatureCheck = resultDto.getSchemaAndSignatureCheck();
        ExpiryCheckDto expiryCheck = resultDto.getExpiryCheck();
        JSONObject claims = resultDto.getClaims();
        boolean allChecksSuccessful = resultDto.isAllChecksSuccessful();

        return populateCredentialResultDto(vc.toString(), null, allChecksSuccessful, schemaAndSignatureCheck, expiryCheck, statusCheck, claims);
    }

    private VPSubmission getVpSubmission(List<String> requestIds) throws VPSubmissionNotFoundException, VPSubmissionWalletError {
        VPSubmission submission = vpSubmissionRepository.findAllById(requestIds)
                .stream()
                .findFirst()
                .orElseThrow(VPSubmissionNotFoundException::new);

        validateVpSubmission(submission);
        return submission;
    }

    private void validateVpSubmission(VPSubmission vpSubmission) throws VPSubmissionWalletError {
        if (vpSubmission.getError() != null && !vpSubmission.getError().isEmpty()) {
            throw new VPSubmissionWalletError(
                    vpSubmission.getError(),
                    vpSubmission.getErrorDescription()
            );
        }
    }

    private void checkVPTokenMatching(VPSubmission vpSubmission, AuthorizationRequestCreateResponse authorizationRequest) throws TokenMatchingFailedException {
        if (!isVPTokenMatching(vpSubmission, authorizationRequest)) {
            throw new TokenMatchingFailedException();
        }
    }

    private CredentialResultsDto populateCredentialResultDto(String credential, HolderProofCheckDto holderProofCheck, boolean allChecksSuccessful, SchemaAndSignatureCheckDto schemaAndSignatureCheck, ExpiryCheckDto expiryCheck, List<StatusCheckDto> statusCheck, JSONObject claims) {
        CredentialResultsDto credentialResults = new CredentialResultsDto();
        credentialResults.setVerifiableCredential(credential);
        credentialResults.setHolderProofCheck(holderProofCheck);
        credentialResults.setAllChecksSuccessful(allChecksSuccessful);
        credentialResults.setSchemaAndSignatureCheck(schemaAndSignatureCheck);
        credentialResults.setExpiryCheck(expiryCheck);
        credentialResults.setStatusCheck(statusCheck);
        credentialResults.setClaims(claims);

        return credentialResults;
    }

    private ExpiryCheckDto populateExpiryCheck(VerificationStatus verificationStatus) {
        return (verificationStatus.equals(VerificationStatus.EXPIRED)) ? new ExpiryCheckDto(false) : new ExpiryCheckDto(true);
    }

    private static HolderProofCheckDto populateHolderProof(VPVerificationStatus status) {
        return (status.equals(VPVerificationStatus.VALID)) ?
                new HolderProofCheckDto(true, null) :
                new HolderProofCheckDto(false, null);
    }

    private VCVerificationRequestDto getVcVerificationRequestDto(VerificationRequestDto request, Object vc) {
        VCVerificationRequestDto vcVerificationRequestDto = new VCVerificationRequestDto(vc.toString());
        vcVerificationRequestDto.setSkipStatusChecks(request.isSkipStatusChecks());
        vcVerificationRequestDto.setStatusCheckFilters(request.getStatusCheckFilters());
        vcVerificationRequestDto.setIncludeClaims(request.isIncludeClaims());

        return vcVerificationRequestDto;
    }
}
