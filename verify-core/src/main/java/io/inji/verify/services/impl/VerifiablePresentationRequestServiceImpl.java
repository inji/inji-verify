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
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.client.ClientMetadataDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.exception.JWTCreationException;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.models.VPSubmission;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.repository.VPSubmissionRepository;
import io.inji.verify.services.KeyManagementService;
import io.inji.verify.shared.Constants;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.inji.verify.utils.SecurityUtils;
import io.inji.verify.utils.Utils;
import io.inji.verify.validator.DcqlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import static io.inji.verify.shared.Constants.VP_FORMATS_SUPPORTED;

@Service
@Slf4j
public class VerifiablePresentationRequestServiceImpl implements VerifiablePresentationRequestService {

    final AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository;
    final VPSubmissionRepository vpSubmissionRepository;
    final KeyManagementService<OctetKeyPair> keyManagementService;
    final ObjectMapper objectMapper;
    final DcqlValidator dcqlValidator;

    @Value("${inji.vp-request.long-polling-timeout}")
    Long defaultTimeout;

    @Value("${inji.vp-submission.base-url}")
    String verifyServiceBaseUrl;

    @Value("${inji.did.verify.public.key.uri}")
    String verifyPublicKeyURI;

    /**
     * The DNS name a x509_san_dns client_id is expected to claim for this deployment. Declared
     * independently of inji.vp-submission.base-url (which can churn in dev, e.g. behind a tunnel)
     * — every deployment sets this once to match its own certificate's SAN, the same way
     * inji.did.verify.uri is independently declared for kid mode. Default matches the SAN baked
     * into the bundled sample keystore (test.p12).
     */
    @Value("${inji.verify.x509-san-dns.host:test.example.com}")
    String x509SanDnsHost = "test.example.com";

    ConcurrentHashMap<String, DeferredResult<VPRequestStatusDto>> vpRequestStatusListeners = new ConcurrentHashMap<>();

    private static final Pattern NONCE_PATTERN = Pattern.compile("^[A-Za-z0-9\\-._~]{16,}$");
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "0.0.0.0");
    // RFC 1123 hostname syntax: dot-separated labels, each 1-63 chars, alphanumeric plus internal
    // hyphens only (no leading/trailing hyphen, no empty labels — so "example..com" and a trailing
    // ":port" both fail), overall length capped at 253. OpenID4VP 5.9.3 requires the x509_san_dns
    // client_id value to actually be a DNS name, not just any string that happens to match config.
    private static final Pattern DNS_NAME_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$");

    public VerifiablePresentationRequestServiceImpl(
            AuthorizationRequestCreateResponseRepository authorizationRequestCreateResponseRepository,
            VPSubmissionRepository vpSubmissionRepository,
            KeyManagementService<OctetKeyPair> keyManagementService,
            ObjectMapper objectMapper,
            DcqlValidator dcqlValidator) {
        this.authorizationRequestCreateResponseRepository = authorizationRequestCreateResponseRepository;
        this.vpSubmissionRepository = vpSubmissionRepository;
        this.keyManagementService = keyManagementService;
        this.objectMapper = objectMapper;
        this.dcqlValidator = dcqlValidator;
    }

    @Override
    public VPRequestResponseDto createAuthorizationRequest(VPRequestCreateDto vpRequestCreate) {
        log.info("Creating authorization request");
        dcqlValidator.validate(vpRequestCreate.getDcqlQuery());
        validateX509SanDnsHost(vpRequestCreate.getClientId());
        validateHttpsForX509SanDns(vpRequestCreate.getClientId());
        validateCertificateChainConfigured(vpRequestCreate.getClientId());
        String transactionId = vpRequestCreate.getTransactionId() != null ? vpRequestCreate.getTransactionId() : Utils.generateID(Constants.TRANSACTION_ID_PREFIX);
        String requestId = Utils.generateID(Constants.REQUEST_ID_PREFIX);
        long expiresAt = Instant.now().plusSeconds(Constants.DEFAULT_EXPIRY).toEpochMilli();
        String nonce;
        if (StringUtils.hasText(vpRequestCreate.getNonce())) {
            if (!NONCE_PATTERN.matcher(vpRequestCreate.getNonce()).matches()) {
                throw new VPRequestValidationException(ErrorCode.NONCE_INVALID);
            }
            nonce = vpRequestCreate.getNonce();
        } else {
            nonce = SecurityUtils.generateNonce();
        }
        String responseUri = verifyServiceBaseUrl + Constants.VP_RESPONSE_SUBMISSION_URI;

        boolean responseCodeValidationRequired = vpRequestCreate.isResponseCodeValidationRequired();
        AuthorizationRequestResponseDto authorizationRequestResponseDto = new AuthorizationRequestResponseDto(
                vpRequestCreate.getClientId(),
                vpRequestCreate.getDcqlQuery(),
                null, // presentationDefinition is deprecated and should not be used, set to null for backward compatibility
                nonce,
                responseUri,
                false, //acceptVPWithoutHolderProof is deprecated and should not be used, set to false for backward compatibility
                responseCodeValidationRequired
        );

        AuthorizationRequestCreateResponse authorizationRequestCreateResponse = new AuthorizationRequestCreateResponse(requestId, transactionId, authorizationRequestResponseDto, expiresAt);
        authorizationRequestCreateResponseRepository.save(authorizationRequestCreateResponse);
        log.info("Authorization request created");
        if (isSignedRequestScheme(vpRequestCreate.getClientId())) {
            String requestUri = verifyServiceBaseUrl + Constants.VP_REQUEST_URI;
            return new VPRequestResponseDto(authorizationRequestCreateResponse.getTransactionId(), authorizationRequestCreateResponse.getRequestId(), null, authorizationRequestCreateResponse.getExpiresAt(), "%s/%s".formatted(requestUri, authorizationRequestCreateResponse.getRequestId()));
        }
        return new VPRequestResponseDto(authorizationRequestCreateResponse.getTransactionId(), authorizationRequestCreateResponse.getRequestId(), authorizationRequestCreateResponse.getAuthorizationDetails(), authorizationRequestCreateResponse.getExpiresAt(), null);
    }

    private static boolean isSignedRequestScheme(String clientId) {
        return clientId.startsWith(Constants.CLIENT_ID_PREFIX_DECENTRALIZED_IDENTIFIER + ":")
                || clientId.startsWith(Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + ":");
    }

    /**
     * Per OpenID4VP 1.0 5.9.3: unless the wallet already trusts this client_id via some
     * allowlist, the DNS name in an x509_san_dns client_id must match this deployment's declared
     * identity. Compared against the independently-configured inji.verify.x509-san-dns.host
     * (not derived from inji.vp-submission.base-url, which can churn in dev — e.g. behind a
     * tunnel — independently of what identity this deployment's certificate actually asserts).
     * Checked at request-creation time (cheap, string-only) rather than at JWT-sign time.
     */
    private void validateX509SanDnsHost(String clientId) {
        String prefix = Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + ":";
        if (clientId == null || !clientId.startsWith(prefix)) {
            return;
        }
        String claimedDns = clientId.substring(prefix.length());
        if (!DNS_NAME_PATTERN.matcher(claimedDns).matches()) {
            throw new VPRequestValidationException(ErrorCode.CLIENT_ID_DNS_NAME_INVALID,
                    "client_id value '" + claimedDns + "' after the x509_san_dns: prefix is not a "
                            + "syntactically valid DNS name.");
        }
        if (x509SanDnsHost == null || !x509SanDnsHost.equalsIgnoreCase(claimedDns)) {
            throw new VPRequestValidationException(ErrorCode.CLIENT_ID_HOST_MISMATCH,
                    "client_id DNS name '" + claimedDns + "' does not match this deployment's configured "
                            + "inji.verify.x509-san-dns.host ('" + x509SanDnsHost + "').");
        }
    }

    /**
     * request_uri delivers a signed JWT the wallet trusts implicitly (its own certificate chain
     * is embedded right there in the response) — plaintext HTTP would let a network attacker
     * substitute or tamper with it in transit. Enforced for x509_san_dns specifically (not
     * decentralized_identifier, to avoid changing behavior for existing deployments of that
     * scheme); skipped for loopback hosts so local dev keeps working over plain HTTP.
     */
    private void validateHttpsForX509SanDns(String clientId) {
        String prefix = Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + ":";
        if (clientId == null || !clientId.startsWith(prefix) || verifyServiceBaseUrl == null) {
            return;
        }
        URI baseUri;
        try {
            baseUri = URI.create(verifyServiceBaseUrl);
        } catch (IllegalArgumentException e) {
            // Fail closed, not open: this check exists specifically to keep an insecure/broken
            // request_uri endpoint from being handed to a wallet for x509_san_dns requests.
            throw new VPRequestValidationException(ErrorCode.REQUEST_URI_INSECURE,
                    "inji.vp-submission.base-url ('" + verifyServiceBaseUrl + "') is not a valid URI.");
        }
        String host = baseUri.getHost();
        if (host == null || host.isEmpty()) {
            throw new VPRequestValidationException(ErrorCode.REQUEST_URI_INSECURE,
                    "inji.vp-submission.base-url ('" + verifyServiceBaseUrl + "') must be an absolute URL "
                            + "with a host for the x509_san_dns client_id scheme.");
        }
        boolean isLocal = LOCAL_HOSTS.contains(host.toLowerCase());
        if (!isLocal && !"https".equalsIgnoreCase(baseUri.getScheme())) {
            throw new VPRequestValidationException(ErrorCode.REQUEST_URI_INSECURE,
                    "inji.vp-submission.base-url ('" + verifyServiceBaseUrl + "') must use https for the "
                            + "x509_san_dns client_id scheme outside local/dev environments.");
        }
    }

    /**
     * Fails fast at request-creation time if this deployment's keystore has no certificate chain
     * configured for x509_san_dns, rather than only surfacing that at JWT-signing time (when the
     * wallet fetches request_uri). The signing-time check in createAndSignAuthorizationRequestJwt
     * stays in place too, since the keystore could theoretically change between request creation
     * and the wallet's fetch.
     */
    private void validateCertificateChainConfigured(String clientId) {
        String prefix = Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + ":";
        if (clientId == null || !clientId.startsWith(prefix)) {
            return;
        }
        X509Certificate[] certChain;
        try {
            certChain = keyManagementService.getCertificateChain();
        } catch (RuntimeException e) {
            log.error("Could not read a signing certificate chain for x509_san_dns client_id: {}", e.getMessage());
            throw new VPRequestValidationException(ErrorCode.CLIENT_ID_CERTIFICATE_CHAIN_MISSING);
        }
        if (certChain == null || certChain.length == 0) {
            log.error("Keystore returned an empty certificate chain for x509_san_dns client_id");
            throw new VPRequestValidationException(ErrorCode.CLIENT_ID_CERTIFICATE_CHAIN_MISSING);
        }
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
                    // cleanup on timeout
                    result.onTimeout(() -> {
                        vpRequestStatusListeners.remove(requestId);
                        result.setResult(getCurrentRequestStatus(requestId));
                    });

                    // cleanup on completion
                    result.onCompletion(() ->
                            vpRequestStatusListeners.remove(requestId)
                    );
                    
                    registerVpRequestStatusListener(requestId, result);
                    return result;
                })
                .orElseGet(() -> {
                    DeferredResult<VPRequestStatusDto> result = new DeferredResult<>();
                    result.setErrorResult(new VPRequestNotFoundException());
                    return result;
                });
    }

    @Override
    public String getVPRequestJwt(String requestId) throws VPRequestNotFoundException {
        return authorizationRequestCreateResponseRepository
                .findById(requestId)
                .map(authorizationRequestCreateResponse -> {
                    AuthorizationRequestResponseDto details = authorizationRequestCreateResponse.getAuthorizationDetails();
                    String verifierDid = details.getClientId();
                    String state = authorizationRequestCreateResponse.getRequestId();
                    return createAndSignAuthorizationRequestJwt(verifierDid, authorizationRequestCreateResponse.getAuthorizationDetails(), state);
                })
                .orElseThrow(VPRequestNotFoundException::new);
    }

    private String createAndSignAuthorizationRequestJwt(String verifierDid, AuthorizationRequestResponseDto authorizationRequest, String state) {

        try {

            String issuer = verifierDid != null
                    ? verifierDid.replaceFirst("^(" + Constants.CLIENT_ID_PREFIX_DECENTRALIZED_IDENTIFIER
                            + "|" + Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + "):", "")
                    : verifierDid;
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(Constants.AUD_SELF_ISSUED)
                    .issueTime(Date.from(Instant.now()))
                    .claim("client_id", verifierDid)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("response_type", authorizationRequest.getResponseType())
                    .claim("response_mode", Constants.RESPONSE_MODE)
                    .claim("nonce", authorizationRequest.getNonce())
                    .claim("state", state)
                    .claim("response_uri", authorizationRequest.getResponseUri());

            if (verifierDid != null && isSignedRequestScheme(verifierDid)) {
                claimsBuilder.claim(
                        "client_metadata",
                        new ClientMetadataDto(VP_FORMATS_SUPPORTED)
                );
            }

            JWTClaimsSet claimsSet = claimsBuilder.build();

            // DCQL-only: never emit presentation_definition / presentation_definition_uri claims (spec).
            if (authorizationRequest.getDcqlQuery() != null) {
                String dcqlQueryJson = objectMapper.writeValueAsString(authorizationRequest.getDcqlQuery());
                claimsSet = new JWTClaimsSet.Builder(claimsSet)
                        .claim("dcql_query", JSONObjectUtils.parse(dcqlQueryJson))
                        .build();
            }

            JWSHeader.Builder jwsHeaderBuilder = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                    .type(new JOSEObjectType("oauth-authz-req+jwt"));

            // Which header this JWT gets is derived entirely from the client_id scheme itself,
            // so the header always matches what's actually claimed. A deployment can serve both
            // schemes side by side — decentralized_identifier:... requests get kid, x509_san_dns:...
            // requests get x5c — since both simply need the same underlying Ed25519 key dressed
            // differently in the header.
            if (verifierDid != null && verifierDid.startsWith(Constants.CLIENT_ID_PREFIX_X509_SAN_DNS + ":")) {
                X509Certificate[] certChain;
                try {
                    certChain = keyManagementService.getCertificateChain();
                } catch (RuntimeException e) {
                    // e.g. no cert configured in the keystore for this deployment — fail clearly
                    // rather than letting a raw exception escape or serving a broken JWT.
                    log.error("Could not read a signing certificate chain for x509_san_dns client_id: {}", e.getMessage());
                    throw new JWTCreationException();
                }
                if (certChain == null || certChain.length == 0) {
                    log.error("Keystore returned an empty certificate chain for x509_san_dns client_id");
                    throw new JWTCreationException();
                }
                for (X509Certificate cert : certChain) {
                    validateCertNotExpired(cert);
                }
                validateSanMatchesIssuer(issuer, certChain[0]);
                jwsHeaderBuilder.x509CertChain(toBase64CertChain(certChain));
            } else {
                jwsHeaderBuilder.keyID(verifyPublicKeyURI);
            }

            JWSHeader jwsHeader = jwsHeaderBuilder.build();
            SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
            JWSSigner signer = new Ed25519Signer(keyManagementService.getKeyPair());

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (ParseException | JOSEException | JsonProcessingException | CertificateEncodingException
                 | CertificateParsingException e) {
            log.error("Error generating JWT: {}", e.getMessage());
            throw new JWTCreationException();
        }
    }

    /**
     * Base64-DER-encodes a leaf-first cert chain for the {@code x5c} header, used instead of
     * {@code kid} when the client_id uses the {@code x509_san_dns} scheme.
     */
    private List<Base64> toBase64CertChain(X509Certificate[] certChain) throws CertificateEncodingException {
        List<Base64> x5c = new ArrayList<>(certChain.length);
        for (X509Certificate cert : certChain) {
            x5c.add(Base64.encode(cert.getEncoded()));
        }
        return x5c;
    }

    /**
     * A signing cert past its notAfter (or not yet at its notBefore) must never be embedded in an
     * x5c header — any spec-compliant wallet validates the cert chain itself and would reject the
     * request anyway, so failing fast here surfaces the real cause (an unrotated/misconfigured
     * keystore) instead of a downstream wallet-side rejection that's hard to diagnose.
     */
    private void validateCertNotExpired(X509Certificate leafCert) {
        try {
            leafCert.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            log.error("Signing certificate is not currently valid: {}", e.getMessage());
            throw new JWTCreationException();
        }
    }

    /**
     * The {@code x509_san_dns} scheme requires the client_id's DNS name (the {@code iss}/issuer
     * value here, prefix already stripped) to actually be present in the signing certificate's
     * Subject Alternative Names — otherwise a wallet validating the cert against the claimed
     * identity would reject it anyway. Catches keystore/client_id drift at sign time rather than
     * producing a JWT no compliant wallet would trust.
     */
    private void validateSanMatchesIssuer(String issuer, X509Certificate leafCert) throws CertificateParsingException {
        Collection<List<?>> sanEntries = leafCert.getSubjectAlternativeNames();
        boolean matches = sanEntries != null && sanEntries.stream()
                .filter(entry -> entry.size() >= 2 && Integer.valueOf(2).equals(entry.get(0))) // type 2 = dNSName
                .anyMatch(entry -> issuer.equalsIgnoreCase(String.valueOf(entry.get(1))));
        if (!matches) {
            log.error("client_id DNS name '{}' not found in signing certificate's Subject Alternative Names", issuer);
            throw new JWTCreationException();
        }
    }
}
