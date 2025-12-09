package io.inji.verify.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.client.ClientMetadataDto;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.JWTCreationException;
import io.inji.verify.exception.PresentationDefinitionNotFoundException;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.repository.PresentationDefinitionRepository;
import io.inji.verify.repository.VPSubmissionRepository;
import io.inji.verify.services.KeyManagementService;
import io.inji.verify.shared.Constants;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.utils.SecurityUtils;
import io.inji.verify.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import static io.inji.verify.shared.Constants.VP_FORMATS;

@Service
@Slf4j
public class VerifiablePresentationRequestServiceImpl implements VerifiablePresentationRequestService {

    final PresentationDefinitionRepository presentationDefinitionRepository;
    final AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository;
    final VPSubmissionRepository vpSubmissionRepository;
    final KeyManagementService<OctetKeyPair> keyManagementService;

    @Value("${inji.vp-request.long-polling-timeout}")
    Long defaultTimeout;

    @Value("${inji.vp-submission.base-url}")
    String verifyServiceBaseUrl;

    @Value("${inji.did.verify.public.key.uri}")
    String verifyPublicKeyURI;

    HashMap<String, DeferredResult<VPRequestStatusDto>> vpRequestStatusListeners = new HashMap<>();

    public VerifiablePresentationRequestServiceImpl(PresentationDefinitionRepository presentationDefinitionRepository, AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository, VPSubmissionRepository vpSubmissionRepository, KeyManagementService<OctetKeyPair> keyManagementService) {
        this.presentationDefinitionRepository = presentationDefinitionRepository;
        this.authorizationRequestCreateResponseRepository = authorizationRequestCreateResponseRepository;
        this.vpSubmissionRepository = vpSubmissionRepository;
        this.keyManagementService = keyManagementService;
    }

    @Override
    public VPRequestResponseDto createAuthorizationRequest(VPRequestCreateDto vpRequestCreate) throws PresentationDefinitionNotFoundException {
        log.info("Creating authorization request");
        String transactionId = vpRequestCreate.getTransactionId() != null ? vpRequestCreate.getTransactionId() : Utils.generateID(Constants.TRANSACTION_ID_PREFIX);
        String requestId = Utils.generateID(Constants.REQUEST_ID_PREFIX);
        long expiresAt = Instant.now().plusSeconds(Constants.DEFAULT_EXPIRY).toEpochMilli();
        String nonce = vpRequestCreate.getNonce() != null ? vpRequestCreate.getNonce() : SecurityUtils.generateNonce();
        String responseUri = verifyServiceBaseUrl + Constants.RESPONSE_SUBMISSION_URI_ROOT + Constants.RESPONSE_SUBMISSION_URI;

        AuthorizationRequestResponseDto authorizationRequestResponseDto = Optional.ofNullable(vpRequestCreate.getPresentationDefinitionId())
                .map(presentationDefinitionId -> presentationDefinitionRepository.findById(presentationDefinitionId)
                .map(presentationDefinition -> {
                    VPDefinitionResponseDto vpDefinitionResponseDto = new VPDefinitionResponseDto(presentationDefinition.getId(), presentationDefinition.getInputDescriptors(), presentationDefinition.getName(), presentationDefinition.getPurpose(), presentationDefinition.getFormat(), presentationDefinition.getSubmissionRequirements());
                    return new AuthorizationRequestResponseDto(vpRequestCreate.getClientId(), presentationDefinition.getURL(), vpDefinitionResponseDto, nonce, responseUri);
                })
                .orElseThrow(PresentationDefinitionNotFoundException::new))
                .orElseGet(() -> new AuthorizationRequestResponseDto(vpRequestCreate.getClientId(), null, vpRequestCreate.getPresentationDefinition(), nonce, responseUri));

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(requestId, transactionId, authorizationRequestResponseDto, expiresAt);
        authorizationRequestCreateResponseRepository.save(authorizationRequestCreateResponse);
        log.info("Authorization request created");
        if (vpRequestCreate.getClientId().startsWith("did")) {
            String requestUri = verifyServiceBaseUrl + Constants.VP_REQUEST_URI;
            return new VPRequestResponseDto(authorizationRequestCreateResponse.getTransactionId(), authorizationRequestCreateResponse.getRequestId(), null, authorizationRequestCreateResponse.getExpiresAt(), "%s/%s".formatted(requestUri, authorizationRequestCreateResponse.getRequestId()));
        }
        return new VPRequestResponseDto(authorizationRequestCreateResponse.getTransactionId(), authorizationRequestCreateResponse.getRequestId(), authorizationRequestCreateResponse.getAuthorizationDetails(), authorizationRequestCreateResponse.getExpiresAt(), null);
    }

    @Override
    public VPRequestStatusDto getCurrentRequestStatus(String requestId) {
        VPSubmission vpSubmission = vpSubmissionRepository.findById(requestId).orElse(null);

        if (vpSubmission != null) {
            return new VPRequestStatusDto(VPRequestStatus.VP_SUBMITTED);
        }
        Long expiresAt = authorizationRequestCreateResponseRepository.findById(requestId).map(AuthorizationRequestCreateResponse::getExpiresAt).orElse(null);
        if (expiresAt == null) {
            return null;
        }
        if (Instant.now().toEpochMilli() > expiresAt) {
            return new VPRequestStatusDto(VPRequestStatus.EXPIRED);
        }
        return new VPRequestStatusDto(VPRequestStatus.ACTIVE);
    }

    @Override
    public List<String> getLatestRequestIdFor(String transactionId) {
        return authorizationRequestCreateResponseRepository.findAllByTransactionIdOrderByExpiresAtDesc(transactionId).stream().map(AuthorizationRequestCreateResponse::getRequestId).toList();
    }

    @Override
    public AuthorizationRequestCreateResponse getLatestAuthorizationRequestFor(String transactionId) {
        try {
            String requestId = getLatestRequestIdFor(transactionId).getFirst();
            return authorizationRequestCreateResponseRepository.findById(requestId).orElse(null);
        }catch (NoSuchElementException e){
            return null;
        }
    }

    private void registerVpRequestStatusListener(String requestId, DeferredResult<VPRequestStatusDto> result) {
        vpRequestStatusListeners.put(requestId, result);
    }

    @Override
    public void invokeVpRequestStatusListener(String requestId) {
        Optional.ofNullable(vpRequestStatusListeners.get(requestId)).ifPresent(vpRequestStatusDtoDeferredResult -> {
            vpRequestStatusDtoDeferredResult.setResult(new VPRequestStatusDto(VPRequestStatus.VP_SUBMITTED));
            vpRequestStatusListeners.remove(requestId);
        });
    }

    @Override
    public DeferredResult<VPRequestStatusDto> getStatus(String requestId) {
        return authorizationRequestCreateResponseRepository
                .findById(requestId)
                .map(authorizationRequestCreateResponse -> {
                    long expiresAt = authorizationRequestCreateResponse.getExpiresAt();
                    long timeToExpiry = expiresAt - Instant.now().toEpochMilli();
                    Long timeOut = timeToExpiry > defaultTimeout ? defaultTimeout : timeToExpiry;
                    DeferredResult<VPRequestStatusDto> result = new DeferredResult<>(timeOut);
                    VPRequestStatusDto currentRequestStatus = getCurrentRequestStatus(requestId);

                    if (currentRequestStatus.getStatus() == VPRequestStatus.EXPIRED) {
                        result.setResult(new VPRequestStatusDto(VPRequestStatus.EXPIRED));
                        return result;
                    }

                    if (currentRequestStatus.getStatus() == VPRequestStatus.VP_SUBMITTED) {
                        result.setResult(new VPRequestStatusDto(VPRequestStatus.VP_SUBMITTED));
                        return result;
                    }

                    result.onTimeout(() -> result.setResult(getCurrentRequestStatus(requestId)));
                    registerVpRequestStatusListener(requestId, result);
                    return result;
                })
                .orElseGet(() -> {
                    DeferredResult<VPRequestStatusDto> result = new DeferredResult<>();
                    result.setErrorResult(ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ErrorCode.NO_AUTH_REQUEST)));
                    return result;
                });
    }

    @Override
    public String getVPRequestJwt(String requestId) throws VPRequestNotFoundException {
        return authorizationRequestCreateResponseRepository
                .findById(requestId)
                .map(authorizationRequestCreateResponse -> {
                    String verifierDid = authorizationRequestCreateResponse.getAuthorizationDetails().getClientId();
                    String state = authorizationRequestCreateResponse.getRequestId();
                    return createAndSignAuthorizationRequestJwt(verifierDid, authorizationRequestCreateResponse.getAuthorizationDetails(), state);
                })
                .orElseThrow(VPRequestNotFoundException::new);
    }
    private String createAndSignAuthorizationRequestJwt(String verifierDid, AuthorizationRequestResponseDto authorizationRequest, String state) {

        try {

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(verifierDid)
                    .issueTime(Date.from(Instant.now()))
                    .claim("client_id", verifierDid)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("response_type", authorizationRequest.getResponseType())
                    .claim("response_mode", Constants.RESPONSE_MODE)
                    .claim("nonce", authorizationRequest.getNonce())
                    .claim("state", state)
                    .claim("response_uri", authorizationRequest.getResponseUri())
                    .claim("client_metadata", new ClientMetadataDto(verifierDid,VP_FORMATS))
                    .build();
            if (authorizationRequest.getPresentationDefinitionUri() != null) {
                claimsSet = new JWTClaimsSet.Builder(claimsSet)
                        .claim("presentation_definition_uri", authorizationRequest.getPresentationDefinitionUri())
                        .build();
            } else if (authorizationRequest.getPresentationDefinition() != null) {
                String presentationDefinitionJson = new ObjectMapper().writeValueAsString(authorizationRequest.getPresentationDefinition());
                claimsSet = new JWTClaimsSet.Builder(claimsSet)
                        .claim("presentation_definition", JSONObjectUtils.parse(presentationDefinitionJson))
                        .build();
            }

            JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                    .type(new JOSEObjectType("oauth-authz-req+jwt"))
                    .keyID(verifyPublicKeyURI)
                    .build();
            SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
            JWSSigner signer = new Ed25519Signer(keyManagementService.getKeyPair());

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (ParseException | JOSEException | JsonProcessingException e) {
            log.error("Error generating JWT: {}", e.getMessage());
            throw new JWTCreationException();
        }
    }
}
