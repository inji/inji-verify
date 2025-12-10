package io.inji.verify.services.impl;

import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.*;
import io.inji.verify.dto.result.VCResultDto;
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
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.json.JSONTokener;
import static io.inji.verify.utils.Utils.isSdJwt;

@Service
@Slf4j
public class VerifiablePresentationSubmissionServiceImpl implements VerifiablePresentationSubmissionService {

    final VPSubmissionRepository vpSubmissionRepository;
    final CredentialsVerifier credentialsVerifier;
    final PresentationVerifier presentationVerifier;
    final VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService;

    public VerifiablePresentationSubmissionServiceImpl(VPSubmissionRepository vpSubmissionRepository, CredentialsVerifier credentialsVerifier, PresentationVerifier presentationVerifier, VerifiablePresentationRequestServiceImpl verifiablePresentationRequestService) {
        this.vpSubmissionRepository = vpSubmissionRepository;
        this.credentialsVerifier = credentialsVerifier;
        this.presentationVerifier = presentationVerifier;
        this.verifiablePresentationRequestService = verifiablePresentationRequestService;
    }

    @Override
    public void submit(VPSubmissionDto vpSubmissionDto) {
        vpSubmissionRepository.save(new VPSubmission(vpSubmissionDto.getState(), vpSubmissionDto.getVpToken(), vpSubmissionDto.getPresentationSubmission(), vpSubmissionDto.getError(), vpSubmissionDto.getErrorDescription()));
        verifiablePresentationRequestService.invokeVpRequestStatusListener(vpSubmissionDto.getState());
    }

    private VPTokenResultDto processSubmission(VPSubmission vpSubmission, String transactionId) throws VPSubmissionWalletError, CredentialStatusCheckException {
        log.info("Processing VP submission");

        List<VCResultDto> verificationResults = new ArrayList<>();
        List<VPVerificationStatus> vpVerificationStatuses = new ArrayList<>();

        try {
            Optional<String> error = Optional.ofNullable(vpSubmission.getError()).filter(e -> !e.isEmpty());
            if (error.isPresent()) {
                log.info("VP submission from wallet has error");
                throw new VPSubmissionWalletError(vpSubmission.getError(), vpSubmission.getErrorDescription());
            }

            log.info("Processing VP token matching");
            if (!isVPTokenMatching(vpSubmission, transactionId)) {
                throw new TokenMatchingFailedException();
            }

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
                    } else {
                        Object verifiableCredential = vpToken.opt("verifiableCredential");
                        if (verifiableCredential instanceof JSONArray array) {
                            for (Object vc : array) {
                                addVerificationResults(vc.toString(), verificationResults, CredentialFormat.LDP_VC);
                            }
                        } else {
                            throw new InvalidVpTokenException();
                        }
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
    public VPTokenResultDto getVPResult(List<String> requestIds, String transactionId) throws VPSubmissionNotFoundException, VPSubmissionWalletError, CredentialStatusCheckException {
        List<VPSubmission> vpSubmissions = vpSubmissionRepository.findAllById(requestIds);

        if (vpSubmissions.isEmpty()) {
            throw new VPSubmissionNotFoundException();
        }
        VPSubmission vpSubmission = vpSubmissions.getFirst();
        return processSubmission(vpSubmission, transactionId);
    }

    private boolean isVPTokenMatching(VPSubmission vpSubmission, String transactionId) {
        Object vpTokenRaw = new JSONTokener(vpSubmission.getVpToken()).nextValue();
        List<DescriptorMapDto> descriptorMap = vpSubmission.getPresentationSubmission().getDescriptorMap();
        AuthorizationRequestCreateResponse request = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);

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
}
