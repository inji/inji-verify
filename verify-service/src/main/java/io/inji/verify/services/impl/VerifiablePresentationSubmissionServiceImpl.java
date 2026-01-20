package io.inji.verify.services.impl;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.result.*;
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
import java.util.Base64;
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

    private VPTokenResultDto processSubmission(VPSubmission vpSubmission, String transactionId) {
        log.info("Processing VP submission");

        List<VCResultDto> verificationResults = new ArrayList<>();
        List<VPVerificationStatus> vpVerificationStatuses = new ArrayList<>();

        try {
            boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(vpSubmission, transactionId);

            VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());

            log.info("Processing VP verification");

            for (JSONObject vpToken : vpTokenDto.getJsonVpTokens()) {
                if (!isVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();
                boolean isSigned = isVerifiablePresentationSigned(vpToken);

                if (isSigned) {
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
            }

            for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
                addVerificationResults(sdJwtVpToken, verificationResults, CredentialFormat.VC_SD_JWT);
            }

            log.info("VP submission processing done");
            return new VPTokenResultDto(transactionId, getCombinedVerificationStatus(vpVerificationStatuses, verificationResults), verificationResults);

        } catch (Exception e) {
            log.error("Failed to verify VP submission", e);
            return new VPTokenResultDto(transactionId, VPResultStatus.FAILED, verificationResults);
        }
    }

    private boolean isAcceptVPWithoutHolderProof(VPSubmission vpSubmission, String transactionId) {
        AuthorizationRequestCreateResponse request = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);

        log.info("Processing VP token matching");
        if (!isVPTokenMatching(vpSubmission, request)) throw new TokenMatchingFailedException();

        return Optional.ofNullable(request.getAuthorizationDetails()).map(AuthorizationRequestResponseDto::isAcceptVPWithoutHolderProof).orElse(false);
    }

    private void addVerificationResults(String vc, List<VCResultDto> verificationResults, CredentialFormat  credentialFormat) {
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

    public VPTokenDto extractTokens(String vpTokenString) {
        if (vpTokenString == null) return null;
        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();

        Object vpTokenRaw = new JSONTokener(vpTokenString).nextValue();

        if (vpTokenRaw instanceof JSONArray array) {
            IntStream.range(0, array.length()).forEach(i -> processSingleToken(array.get(i), jsonVpTokens, sdJwtVpTokens));
        } else {
            processSingleToken(vpTokenRaw, jsonVpTokens, sdJwtVpTokens);
        }

        log.debug("Number of VP tokens to verify: {}", jsonVpTokens.size() + ":" + sdJwtVpTokens.size());
        if (jsonVpTokens.isEmpty() && sdJwtVpTokens.isEmpty()) throw new InvalidVpTokenException();

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

    @Override
    public VPTokenResultDto getVPResult(List<String> requestIds, String transactionId) {
        VPSubmission vpSubmission = getValidVpSubmission(requestIds);
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
    public VPVerificationResultDto getVPResultV2(@Valid VerificationRequestDto request, List<String> requestIds, String transactionId) {
        VPSubmission vpSubmission = getValidVpSubmission(requestIds);
        boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(vpSubmission, transactionId);

        VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());

        List<CredentialResultsDto> credentialResults = new ArrayList<>();

        for (JSONObject vpToken : vpTokenDto.getJsonVpTokens()) {
            credentialResults.addAll(verifyPresentations(request, vpToken, acceptVPWithoutHolderProof));
        }

        for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
            credentialResults.add(verifyCredential(request, sdJwtVpToken));
        }

        boolean allChecksSuccessful = credentialResults.stream().allMatch(CredentialResultsDto::isAllChecksSuccessful);
        return new VPVerificationResultDto(transactionId, allChecksSuccessful, credentialResults);
    }

    private List<CredentialResultsDto> verifyPresentations(VerificationRequestDto request, JSONObject vpToken, boolean acceptVPWithoutHolderProof) {
        if (!isVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();
        boolean isSigned = isVerifiablePresentationSigned(vpToken);

        if (isSigned) {
            return verifySignedPresentations(vpToken, request);
        } else if (acceptVPWithoutHolderProof) {
            return verifyUnsignedPresentations(request, vpToken);
        } else {
            throw new VPWithoutProofException();
        }
    }

    private List<CredentialResultsDto> verifyUnsignedPresentations(VerificationRequestDto request, JSONObject vpToken) {
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

    private List<CredentialResultsDto> verifySignedPresentations(JSONObject vpToken, @Valid VerificationRequestDto request) {
        List<VerificationSummary> list;
        if (request.isSkipStatusChecks()) {
            PresentationVerificationResult result = presentationVerifier.verify(vpToken.toString());
            list = result.getVcResults().stream()
                    .map(vcRes -> new VerificationSummary(
                            result.getProofVerificationStatus(),
                            vcRes.getVc(),
                            vcRes.getStatus(),
                            null))
                    .toList();
        } else {
            List<String> filters = request.getStatusCheckFilters();
            PresentationResultWithCredentialStatus result = presentationVerifier.verifyAndGetCredentialStatus(vpToken.toString(), filters);
            list = result.getVcResults().stream()
                    .map(vcRes -> new VerificationSummary(
                            result.getProofVerificationStatus(),
                            vcRes.getVc(),
                            vcRes.getStatus(),
                            vcRes.getCredentialStatus()))
                    .toList();
        }

        List<CredentialResultsDto> results = new ArrayList<>();

        for (VerificationSummary verificationSummary : list) {
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

    private VPSubmission getValidVpSubmission(List<String> requestIds) {
        VPSubmission submission = vpSubmissionRepository.findAllById(requestIds)
                .stream()
                .findFirst()
                .orElseThrow(VPSubmissionNotFoundException::new);

        validateVpSubmission(submission);
        return submission;
    }

    private void validateVpSubmission(VPSubmission vpSubmission) {
        if (vpSubmission.getError() != null && !vpSubmission.getError().isEmpty()) {
            throw new VPSubmissionWalletError(
                    vpSubmission.getError(),
                    vpSubmission.getErrorDescription()
            );
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
