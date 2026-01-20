package io.inji.verify.services.impl;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.*;
import io.inji.verify.dto.submission.DescriptorMapDto;
import io.inji.verify.dto.submission.VPSubmissionDto;
import io.inji.verify.dto.submission.VPTokenResultDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.VCVerificationRequestDto;
import io.inji.verify.enums.KBJwtErrorCodes;
import io.inji.verify.enums.VPResultStatus;
import io.inji.verify.exception.*;
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
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.IntStream;
import static io.inji.verify.utils.Utils.isSdJwt;
import static io.inji.verify.utils.Utils.createStatusCheckDtoList;
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

    private VPTokenResultDto processSubmission(VPSubmission vpSubmission, String transactionId) throws VPSubmissionWalletError,  InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException {
        log.info("Processing VP submission");

        List<VCResultDto> verificationResults = new ArrayList<>();
        List<VPVerificationStatus> vpVerificationStatuses = new ArrayList<>();

        try {
            AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);

            log.info("Processing VP token matching");
            if (isVPTokenNotMatching(vpSubmission, authRequest)) throw new TokenMatchingFailedException();

            VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());

            log.info("Processing VP verification");
            boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(authRequest);
            for (JSONObject vpToken : vpTokenDto.getJsonVpTokens()) {
                if (isInvalidVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();
                boolean isSigned = isVerifiablePresentationSigned(vpToken);

                if (isSigned) {
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

    private VPVerificationResultDto processSubmissionV2(VerificationRequestDto request, String transactionId, VPSubmission vpSubmission) {
        log.info("Processing VP submission V2");

        List<CredentialResultsDto> credentialResults = new ArrayList<>();

        AuthorizationRequestCreateResponse authRequest = verifiablePresentationRequestService.getLatestAuthorizationRequestFor(transactionId);

        log.info("Processing VP token matching V2");
        if (isVPTokenNotMatching(vpSubmission, authRequest)) throw new TokenMatchingFailedException();

        VPTokenDto vpTokenDto = extractTokens(vpSubmission.getVpToken());

        log.info("Processing VP verification V2");
        boolean acceptVPWithoutHolderProof = isAcceptVPWithoutHolderProof(authRequest);
        for (JSONObject vpToken : vpTokenDto.getJsonVpTokens()) {
            if (isInvalidVerifiablePresentation(vpToken)) throw new InvalidVpTokenException();
            boolean isSigned = isVerifiablePresentationSigned(vpToken);

            if (isSigned) {
                if (request.isSkipStatusChecks()) {
                    verifyPresentation(vpToken, credentialResults);
                } else {
                    verifyPresentationWithCredentialStatusChecks(request, vpToken, credentialResults);
                }
            } else if (acceptVPWithoutHolderProof) {
                // for a VPToken without proof, do verification for all credentials
                Object verifiableCredential = vpToken.opt("verifiableCredential");
                if (verifiableCredential instanceof JSONArray array) {
                    if (array.isEmpty()) throw new InvalidVpTokenException();
                    for (Object vc : array) {
                        credentialResults.add(verifySingleCredential(request, vc, false));
                    }
                } else {
                    throw new InvalidVpTokenException();
                }
            } else {
                throw new VPWithoutProofException();
            }
        }

        for (String sdJwtVpToken : vpTokenDto.getSdJwtVpTokens()) {
            credentialResults.add(verifySingleCredential(request, sdJwtVpToken, true));
        }

        boolean allChecksSuccessful = credentialResults.stream().allMatch(CredentialResultsDto::isAllChecksSuccessful);

        log.info("VP submission processing done V2");
        return new VPVerificationResultDto(transactionId, allChecksSuccessful, credentialResults);
    }

    private void verifyPresentationWithCredentialStatusChecks(VerificationRequestDto request, JSONObject vpToken, List<CredentialResultsDto> credentialResults) {
        List<String> filters = request.getStatusCheckFilters();
        PresentationResultWithCredentialStatus result = presentationVerifier.verifyAndGetCredentialStatus(vpToken.toString(), filters);
        List<VCResultWithCredentialStatus> vcResults = result.getVcResults();
        if (vcResults.isEmpty()) throw new InvalidVpTokenException();
        for (VCResultWithCredentialStatus vcResWithStatus : vcResults) {
            CredentialResultsDto credentialResultsDto = new CredentialResultsDto();
            credentialResultsDto.setVerifiableCredential(vcResWithStatus.getVc());
            credentialResultsDto.setHolderProofCheck(createHolderProofDto(result.getProofVerificationStatus()));
            credentialResultsDto.setSchemaAndSignatureCheck(createSchemaAndSignatureCheckDto(vcResWithStatus.getStatus()));
            credentialResultsDto.setExpiryCheck(createExpiryCheckDto(vcResWithStatus.getStatus(), credentialResultsDto));
            credentialResultsDto.setStatusCheck(createStatusCheckDtoList(vcResWithStatus.getCredentialStatus()));
            boolean allChecksSuccessful = populateAllChecksSuccessful(credentialResultsDto.getSchemaAndSignatureCheck(), credentialResultsDto.getExpiryCheck(), credentialResultsDto.getStatusCheck(), credentialResultsDto.getHolderProofCheck());
            credentialResultsDto.setAllChecksSuccessful(allChecksSuccessful);
            credentialResults.add(credentialResultsDto);
        }
    }

    private void verifyPresentation(JSONObject vpToken, List<CredentialResultsDto> credentialResults) {
        PresentationVerificationResult result = presentationVerifier.verify(vpToken.toString());
        List<VCResult> vcResults = result.getVcResults();
        if (vcResults.isEmpty()) throw new InvalidVpTokenException();
        for (VCResult vcRes : vcResults) {
            CredentialResultsDto credentialResultsDto = new CredentialResultsDto();
            credentialResultsDto.setVerifiableCredential(vcRes.getVc());
            credentialResultsDto.setHolderProofCheck(createHolderProofDto(result.getProofVerificationStatus()));
            credentialResultsDto.setSchemaAndSignatureCheck(createSchemaAndSignatureCheckDto(vcRes.getStatus()));
            credentialResultsDto.setExpiryCheck(createExpiryCheckDto(vcRes.getStatus(), credentialResultsDto));
            boolean allChecksSuccessful = populateAllChecksSuccessful(credentialResultsDto.getSchemaAndSignatureCheck(), credentialResultsDto.getExpiryCheck(), credentialResultsDto.getStatusCheck(), credentialResultsDto.getHolderProofCheck());
            credentialResultsDto.setAllChecksSuccessful(allChecksSuccessful);
            credentialResults.add(credentialResultsDto);
        }
    }

    private boolean isAcceptVPWithoutHolderProof(AuthorizationRequestCreateResponse request) {
        return Optional.ofNullable(request.getAuthorizationDetails()).map(AuthorizationRequestResponseDto::isAcceptVPWithoutHolderProof).orElse(false);
    }

    private void addVerificationResults(String vc, List<VCResultDto> verificationResults, CredentialFormat credentialFormat) {
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

    private boolean isInvalidVerifiablePresentation(JSONObject vpToken) {
        Object types = vpToken.opt("type");
        if (types == null) return true;

        return !switch (types) {
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
        if (vpTokenString == null) throw new InvalidVpTokenException();
        List<JSONObject> jsonVpTokens = new ArrayList<>();
        List<String> sdJwtVpTokens = new ArrayList<>();

        Object vpTokenRaw = new JSONTokener(vpTokenString).nextValue();

        if (vpTokenRaw instanceof JSONArray array) {
            IntStream.range(0, array.length()).forEach(i -> processSingleToken(array.get(i), jsonVpTokens, sdJwtVpTokens));
        } else {
            processSingleToken(vpTokenRaw, jsonVpTokens, sdJwtVpTokens);
        }

        log.debug("Number of VP tokens to verify: {}", jsonVpTokens.size() + ":" + sdJwtVpTokens.size());
        if (jsonVpTokens.isEmpty() && sdJwtVpTokens.isEmpty())
            throw new InvalidVpTokenException();

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
    public VPTokenResultDto getVPResult(List<String> requestIds, String transactionId) throws VPSubmissionWalletError,  InvalidVpTokenException, CredentialStatusCheckException, VPWithoutProofException, VPSubmissionNotFoundException {
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds);
        return processSubmission(vpSubmission, transactionId);
    }

    @Override
    public VPVerificationResultDto getVPResultV2(VerificationRequestDto request, List<String> requestIds, String transactionId) {
        VPSubmission vpSubmission = fetchVpSubmissionIfValid(requestIds);
        return processSubmissionV2(request, transactionId, vpSubmission);
    }

    private boolean isVPTokenNotMatching(VPSubmission vpSubmission, AuthorizationRequestCreateResponse request) {
        Object vpTokenRaw = new JSONTokener(vpSubmission.getVpToken()).nextValue();
        List<DescriptorMapDto> descriptorMap = vpSubmission.getPresentationSubmission().getDescriptorMap();

        if (vpTokenRaw == null || request == null || descriptorMap == null || descriptorMap.isEmpty()) {
            log.info("Unable to perform token matching");
            return true;
        }

        log.info("VP token matching done");
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

    private CredentialResultsDto verifySingleCredential(VerificationRequestDto request, Object vc, boolean isSdJwt) {
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

    private VPSubmission fetchVpSubmissionIfValid(List<String> requestIds) {
        VPSubmission submission = vpSubmissionRepository.findAllById(requestIds)
                .stream()
                .findFirst()
                .orElseThrow(VPSubmissionNotFoundException::new);

        if (submission.getError() != null && !submission.getError().isEmpty()) throw new VPSubmissionWalletError(submission.getError(), submission.getErrorDescription());

        return submission;
    }

    private SchemaAndSignatureCheckDto createSchemaAndSignatureCheckDto(VerificationStatus verificationStatus) {
        return verificationStatus.equals(VerificationStatus.INVALID)
                ? new SchemaAndSignatureCheckDto(false, null)
                : new SchemaAndSignatureCheckDto(true, null);
    }

    private ExpiryCheckDto createExpiryCheckDto(VerificationStatus verificationStatus, CredentialResultsDto credentialResultsDto) {
        if (!credentialResultsDto.getSchemaAndSignatureCheck().isValid()) return null;

        return (verificationStatus.equals(VerificationStatus.EXPIRED)) ? new ExpiryCheckDto(false) : new ExpiryCheckDto(true);
    }

    private static HolderProofCheckDto createHolderProofDto(VPVerificationStatus status) {
        return (status.equals(VPVerificationStatus.VALID)) ?
                new HolderProofCheckDto(true, null) :
                new HolderProofCheckDto(false, null);
    }
}
