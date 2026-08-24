package io.inji.verify.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestStatusDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPRequestNotFoundException;
import io.inji.verify.exception.VPRequestValidationException;
import com.nimbusds.jwt.SignedJWT;
import io.inji.verify.testsupport.DcqlTestFixtures;
import io.inji.verify.testsupport.TestCertUtil;
import io.inji.verify.enums.VPRequestStatus;
import io.inji.verify.models.AuthorizationRequestCreateResponse;
import io.inji.verify.repository.AuthorizationRequestCreateResponseRepository;
import io.inji.verify.repository.VPSubmissionRepository;
import io.inji.verify.services.KeyManagementService;
import io.inji.verify.shared.Constants;
import io.inji.verify.validator.DcqlValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.async.DeferredResult;

import java.text.ParseException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class VerifiablePresentationRequestServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static VerifiablePresentationRequestServiceImpl service;
    static AuthorizationRequestCreateResponseRepository mockAuthorizationRequestCreateResponseRepository;
    static VPSubmissionRepository mockVPSubmissionRepository;
    static KeyManagementService<OctetKeyPair> mockKeyManagementService;
    static DcqlValidator mockDcqlValidator;

    private static DCQLQueryDto minimalDcqlQuery() throws Exception {
        return OBJECT_MAPPER.readValue(
                "{\"credentials\":[{\"id\":\"cred1\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"cred1\"]}}]}",
                DCQLQueryDto.class);
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        mockAuthorizationRequestCreateResponseRepository = mock(AuthorizationRequestCreateResponseRepository.class);
        mockVPSubmissionRepository = mock(VPSubmissionRepository.class);
        mockKeyManagementService = mock(KeyManagementService.class);
        mockDcqlValidator = mock(DcqlValidator.class);
        service = new VerifiablePresentationRequestServiceImpl(
                mockAuthorizationRequestCreateResponseRepository,
                mockVPSubmissionRepository,
                mockKeyManagementService,
                OBJECT_MAPPER,
                mockDcqlValidator);
    }

    @Test
    public void shouldCreateNewAuthorizationRequest() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                "test_transaction_id",
                null,
                minimalDcqlQuery(),
                false);

        VPRequestResponseDto responseDto = service.createAuthorizationRequest(vpRequestCreateDto);

        assertNotNull(responseDto);
        assertEquals("test_transaction_id", responseDto.getTransactionId());
        assertNotNull(responseDto.getRequestId());
        assertNotNull(responseDto.getAuthorizationDetails());
        assertTrue(responseDto.getExpiresAt() > Instant.now().toEpochMilli());
    }

    @Test
    public void shouldCreateAuthorizationRequestWithMissingTransactionId() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                null,
                null,
                minimalDcqlQuery(),
                false);

        VPRequestResponseDto responseDto = service.createAuthorizationRequest(vpRequestCreateDto);

        assertNotNull(responseDto);
        assertTrue(responseDto.getTransactionId().startsWith(Constants.TRANSACTION_ID_PREFIX));
    }

    @Test
    public void shouldInvokeDcqlValidatorForValidQuery() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        DCQLQueryDto dcqlQuery = minimalDcqlQuery();
        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                "test_transaction_id_dcql_validate",
                null,
                dcqlQuery,
                false);

        service.createAuthorizationRequest(vpRequestCreateDto);

        verify(mockDcqlValidator, times(1)).validate(dcqlQuery);
    }

    @Test
    public void shouldPropagateExceptionWhenDcqlValidationFails() throws Exception {
        DCQLQueryDto dcqlQuery = minimalDcqlQuery();
        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                "test_transaction_id_dcql_invalid",
                null,
                dcqlQuery,
                false);

        doThrow(new VPRequestValidationException(ErrorCode.DCQL_CREDENTIAL_ID_REQUIRED))
                .when(mockDcqlValidator).validate(any(DCQLQueryDto.class));
        try {
            assertThrows(VPRequestValidationException.class,
                    () -> service.createAuthorizationRequest(vpRequestCreateDto));
        } finally {
            // mockDcqlValidator is a shared static mock (initialized once in @BeforeAll); reset it
            // so this failure stub doesn't leak into other tests in this class.
            reset(mockDcqlValidator);
        }
    }

    @Test
    public void shouldGetCurrentAuthorizationRequestStateForExistingRequest() {
        AuthorizationRequestCreateResponse mockResponse =
                new AuthorizationRequestCreateResponse("req_id", "tx_id", null, Instant.now().toEpochMilli() + 10000);
        when(mockAuthorizationRequestCreateResponseRepository.findById("req_id"))
                .thenReturn(java.util.Optional.of(mockResponse));
        when(mockVPSubmissionRepository.findById("req_id")).thenReturn(Optional.empty());

        VPRequestStatusDto vpRequestStatusDto = service.getCurrentRequestStatus("req_id");

        assertEquals(VPRequestStatus.ACTIVE, vpRequestStatusDto.getStatus());
    }

    @Test
    public void shouldGetCurrentAuthorizationRequestStateForNonexistentRequest() {
        when(mockVPSubmissionRepository.findById("req_id")).thenReturn(Optional.empty());
        AuthorizationRequestCreateResponse mockResponse =
                new AuthorizationRequestCreateResponse("req_id", "tx_id", null, Instant.now().toEpochMilli() + 10000);
        when(mockAuthorizationRequestCreateResponseRepository.findById("req_id"))
                .thenReturn(java.util.Optional.of(mockResponse));

        VPRequestStatusDto vpRequestStatusDto = service.getCurrentRequestStatus("nonexistent_id");

        assertNull(vpRequestStatusDto);
    }

    @Test
    void getStatus_requestIdNotFound_returnsNotFoundError() {
        when(mockAuthorizationRequestCreateResponseRepository.findById("req_id")).thenReturn(Optional.empty());

        DeferredResult<VPRequestStatusDto> result = service.getStatus("req_id");

        // Error result is a plain exception (not an HTTP-specific type) so embedding consumers
        // aren't coupled to Spring MVC; the controller's @ExceptionHandler maps this to 404.
        assertInstanceOf(VPRequestNotFoundException.class, Objects.requireNonNull(result.getResult()));
    }

    @Test()
    void getStatus_requestExpired_returnsExpiredStatus() {
        service.defaultTimeout = 1000L;
        String requestId = "req_id";
        AuthorizationRequestCreateResponse response =
                new AuthorizationRequestCreateResponse("req_id", "tx_id", null, Instant.now().toEpochMilli() - 10000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(response));

        DeferredResult<VPRequestStatusDto> result = service.getStatus(requestId);

        assertEquals(
                VPRequestStatus.EXPIRED,
                ((VPRequestStatusDto) Objects.requireNonNull(result.getResult())).getStatus());
    }

    @Test
    @DisplayName("Should produce JWT when verifierDid is null (null issuer path)")
    void getVPRequestJwt_WithNullVerifierDid_ProducesJwt() throws Exception {
        String requestId = "req_null_did";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        null, // null clientId → verifierDid null
                        DcqlTestFixtures.minimalDcqlDto(),
                        null, "nonce", "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String jwt = service.getVPRequestJwt(requestId);

        assertNotNull(jwt);
        // issuer should be null when verifierDid is null
        assertNull(SignedJWT.parse(jwt).getJWTClaimsSet().getIssuer());
    }

    @Test
    @DisplayName("JWT should include client_metadata when clientId starts with decentralized_identifier")
    void getVPRequestJwt_WithDecentralizedIdentifierDid_IncludesClientMetadata() throws Exception {
        String requestId = "req_dec_id";
        String did = "decentralized_identifier:did:example:verifier";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        did,
                        DcqlTestFixtures.minimalDcqlDto(),
                        null, "nonce", "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String jwt = service.getVPRequestJwt(requestId);

        assertNotNull(jwt);
        var claims = SignedJWT.parse(jwt).getJWTClaimsSet();
        // The decentralized_identifier: prefix should be stripped for the issuer
        assertEquals("did:example:verifier", claims.getIssuer());
        // client_metadata claim should be present
        assertNotNull(claims.getClaim("client_metadata"));
    }

    @Test
    @DisplayName("Should return JWT string when authorization request and details are valid")
    void getVPRequestJwt_ValidRequest_ReturnsJwtString() throws Exception {
        String requestId = "testRequestId123";
        String verifierDid = "did:example:verifier123";
        String expectedJwtHeader = "eyJ0eXAiOiJvYXV0aC1hdXRoei1yZXErand0IiwiYWxnIjoiRWREU0EifQ.";

        AuthorizationRequestResponseDto authzDetailsDto =
                new AuthorizationRequestResponseDto(
                        verifierDid,
                        DcqlTestFixtures.minimalDcqlDto(),
                        null,
                        "nonce",
                        "https://verifier.example/resp",
                        false,
                        false);

        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, null, authzDetailsDto, 0L);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();

        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String actualJwt = service.getVPRequestJwt(requestId);

        assertNotNull(actualJwt);
        assertTrue(actualJwt.startsWith(expectedJwtHeader));
        assertJwtContainsDcqlWithoutPresentationDefinition(actualJwt);

        verify(mockAuthorizationRequestCreateResponseRepository, times(1)).findById(requestId);
    }

    @Test
    @DisplayName("Signed request object JWT should carry the fixed self-issued aud claim per OpenID4VP 5.8")
    void getVPRequestJwt_ProducesAudClaim_SelfIssued() throws Exception {
        String requestId = "req_aud_check";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "decentralized_identifier:did:example:verifier", DcqlTestFixtures.minimalDcqlDto(),
                        null, "nonce", "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String jwt = service.getVPRequestJwt(requestId);

        var claims = SignedJWT.parse(jwt).getJWTClaimsSet();
        assertEquals(java.util.List.of("https://self-issued.me/v2"), claims.getAudience());
    }

    private static void assertJwtContainsDcqlWithoutPresentationDefinition(String jwt) throws ParseException {
        var claims = SignedJWT.parse(jwt).getJWTClaimsSet();
        assertNotNull(claims.getClaim("dcql_query"));
        assertNull(claims.getClaim("presentation_definition"));
        assertNull(claims.getClaim("presentation_definition_uri"));
    }

    @Test
    void getVPRequestJwt_NullAuthorizationDetails_Throws_VPRequestNotFoundException() {
        String requestId = "reqWithNullDetails";
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(VPRequestNotFoundException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    void getVPRequestJwt_WhenDcqlMissing_ReturnsJwtWithoutDcqlClaim() throws Exception {
        String requestId = "reqMissingDcql";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto("did:example", null, null, "nonce", "responseUri", false, false);
        AuthorizationRequestCreateResponse response =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 1000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(response));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String jwt = service.getVPRequestJwt(requestId);

        assertNotNull(jwt);
        assertNull(SignedJWT.parse(jwt).getJWTClaimsSet().getClaim("dcql_query"));
    }

    @Test
    void getStatus_WithTimeout_InvokesListener() {
        service.defaultTimeout = 100L;
        String requestId = "timeoutReq";
        AuthorizationRequestCreateResponse response =
                new AuthorizationRequestCreateResponse(
                        requestId,
                        "tx",
                        new AuthorizationRequestResponseDto(
                                "did:example", DcqlTestFixtures.minimalDcqlDto(), null, "nonce", "responseUri", false, false),
                        Instant.now().toEpochMilli() + 2000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(response));

        DeferredResult<VPRequestStatusDto> result = service.getStatus(requestId);
        assertNotNull(result);
    }

    @Test
    void getVPRequestJwt_RequestNotFound_ThrowsException() {
        when(mockAuthorizationRequestCreateResponseRepository.findById("missingId")).thenReturn(Optional.empty());

        assertThrows(VPRequestNotFoundException.class, () -> service.getVPRequestJwt("missingId"));
    }

    @Test
    @DisplayName("Should embed x5c (not kid) in JWT header for x509_san_dns client_id")
    void getVPRequestJwt_WithX509SanDnsClientId_EmbedsCertChain() throws Exception {
        String requestId = "req_x5c";
        String dnsName = "test.example.com";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:" + dnsName, DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        java.security.cert.X509Certificate cert = TestCertUtil.generateSelfSignedCert(edKeyPair, dnsName);
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{cert});

        String jwt = service.getVPRequestJwt(requestId);

        assertNotNull(jwt);
        var header = SignedJWT.parse(jwt).getHeader();
        assertNull(header.getKeyID(), "kid must not be set for x509_san_dns client_id");
        assertNotNull(header.getX509CertChain(), "x5c must be set for x509_san_dns client_id");
        assertEquals(1, header.getX509CertChain().size());
        assertEquals(dnsName, SignedJWT.parse(jwt).getJWTClaimsSet().getIssuer());
    }

    @Test
    @DisplayName("Should embed x5c when client_id's DNS name and the cert's SAN differ only by letter case")
    void getVPRequestJwt_WithX509SanDnsClientIdDifferingCase_EmbedsCertChain() throws Exception {
        String requestId = "req_x5c_case_insensitive";
        String clientIdDnsName = "TEST.EXAMPLE.COM"; // differs in case from x509SanDnsHost default and cert SAN below
        String certSanDnsName = "test.example.com";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:" + clientIdDnsName, DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        java.security.cert.X509Certificate cert = TestCertUtil.generateSelfSignedCert(edKeyPair, certSanDnsName);
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{cert});

        String jwt = service.getVPRequestJwt(requestId);

        assertNotNull(jwt);
        var header = SignedJWT.parse(jwt).getHeader();
        assertNotNull(header.getX509CertChain(), "x5c must be set — SAN match must be case-insensitive");
        assertEquals(clientIdDnsName, SignedJWT.parse(jwt).getJWTClaimsSet().getIssuer());
    }

    @Test
    @DisplayName("Should throw JWTCreationException when x509_san_dns client_id's DNS name isn't in the cert's SAN")
    void getVPRequestJwt_WithX509SanDnsMismatch_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_mismatch";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:mismatched.example.com", DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        // cert's SAN is "test.example.com", clientId claims "mismatched.example.com" — must not match
        java.security.cert.X509Certificate cert = TestCertUtil.generateSelfSignedCert(edKeyPair, "test.example.com");
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{cert});

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    @DisplayName("Should throw JWTCreationException when the signing certificate has no SAN extension at all")
    void getVPRequestJwt_WithX509SanDnsNoSanExtension_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_no_san";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:test.example.com", DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        // no sanDnsName passed — cert has no Subject Alternative Name extension at all, distinct
        // from the "SAN present but doesn't match" case covered above (getSubjectAlternativeNames()
        // returns null here rather than a non-matching list).
        java.security.cert.X509Certificate certWithNoSan = TestCertUtil.generateSelfSignedCert(edKeyPair);
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{certWithNoSan});

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    @DisplayName("createAuthorizationRequest should use the by-reference (request_uri) flow for x509_san_dns client_id")
    void createAuthorizationRequest_X509ClientId_UsesRequestUriFlow() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{
                        TestCertUtil.generateSelfSignedCert(edKeyPair, "test.example.com")});
        try {
            // service.x509SanDnsHost defaults to "test.example.com" (matching the bundled sample keystore)
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509", null, minimalDcqlQuery(), false);

            VPRequestResponseDto response = service.createAuthorizationRequest(dto);

            assertNotNull(response);
            assertNotNull(response.getRequestUri(), "x509_san_dns should use the by-reference (request_uri) flow");
            assertNull(response.getAuthorizationDetails());
        } finally {
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should reject x509_san_dns client_id whose DNS name doesn't match inji.verify.x509-san-dns.host")
    void createAuthorizationRequest_X509ClientIdHostMismatch_ThrowsValidationException() throws Exception {
        VPRequestCreateDto dto = new VPRequestCreateDto(
                "x509_san_dns:other-host.example.com", "tx_x509_mismatch", null, minimalDcqlQuery(), false);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> service.createAuthorizationRequest(dto));
        assertEquals(ErrorCode.CLIENT_ID_HOST_MISMATCH, ex.getErrorCode());
    }

    @Test
    @DisplayName("createAuthorizationRequest should reject an x509_san_dns client_id whose value isn't a syntactically valid DNS name")
    void createAuthorizationRequest_X509ClientIdSyntacticallyInvalidDnsName_ThrowsValidationException() throws Exception {
        for (String invalidDns : new String[]{"foo_bar", "example.com:443", "example..com"}) {
            String originalHost = service.x509SanDnsHost;
            service.x509SanDnsHost = invalidDns; // even if config "matches", syntax must still fail
            try {
                VPRequestCreateDto dto = new VPRequestCreateDto(
                        "x509_san_dns:" + invalidDns, "tx_x509_invalid_dns_" + invalidDns.hashCode(), null,
                        minimalDcqlQuery(), false);

                VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                        () -> service.createAuthorizationRequest(dto),
                        "expected rejection for invalid DNS name: " + invalidDns);
                assertEquals(ErrorCode.CLIENT_ID_DNS_NAME_INVALID, ex.getErrorCode());
            } finally {
                service.x509SanDnsHost = originalHost;
            }
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should honor an overridden inji.verify.x509-san-dns.host")
    void createAuthorizationRequest_X509ClientIdMatchingOverriddenHost_Succeeds() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{
                        TestCertUtil.generateSelfSignedCert(edKeyPair, "verify.acmecorp.example")});
        String originalHost = service.x509SanDnsHost;
        service.x509SanDnsHost = "verify.acmecorp.example";
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:verify.acmecorp.example", "tx_x509_override", null, minimalDcqlQuery(), false);

            VPRequestResponseDto response = service.createAuthorizationRequest(dto);

            assertNotNull(response);
            assertNotNull(response.getRequestUri());
        } finally {
            service.x509SanDnsHost = originalHost;
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should fail fast when no certificate chain is configured for x509_san_dns, before ever reaching request_uri issuance")
    void createAuthorizationRequest_X509ClientIdNoCertChainConfigured_ThrowsValidationException() throws Exception {
        // Simulates a deployment that never configured a certificate chain for this scheme.
        when(mockKeyManagementService.getCertificateChain())
                .thenThrow(new RuntimeException("No certificate chain found in keystore"));
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_no_cert", null, minimalDcqlQuery(), false);

            VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                    () -> service.createAuthorizationRequest(dto));
            assertEquals(ErrorCode.CLIENT_ID_CERTIFICATE_CHAIN_MISSING, ex.getErrorCode());
        } finally {
            // mockKeyManagementService is a shared static mock (initialized once in @BeforeAll); reset
            // so this thenThrow() stub doesn't leak into later tests calling getCertificateChain().
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should fail fast when the keystore returns an empty certificate chain for x509_san_dns")
    void createAuthorizationRequest_X509ClientIdEmptyCertChain_ThrowsValidationException() throws Exception {
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[0]);
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_empty_cert", null, minimalDcqlQuery(), false);

            VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                    () -> service.createAuthorizationRequest(dto));
            assertEquals(ErrorCode.CLIENT_ID_CERTIFICATE_CHAIN_MISSING, ex.getErrorCode());
        } finally {
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("Should throw JWTCreationException with a clear log (not a raw RuntimeException) when no cert chain is configured")
    void getVPRequestJwt_WithX509SanDns_NoCertChainConfigured_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_no_cert";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:test.example.com", DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);
        // Simulates a deployment that never configured a certificate chain for this scheme.
        when(mockKeyManagementService.getCertificateChain())
                .thenThrow(new RuntimeException("No certificate chain found in keystore"));

        try {
            assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
        } finally {
            // mockKeyManagementService is a shared static mock (initialized once in @BeforeAll).
            // A thenThrow() stub on getCertificateChain() persists for the whole class otherwise —
            // any later test's when(...).thenReturn(...) on the same method would trigger this
            // leftover throw while Mockito evaluates the call inside when(), before .thenReturn
            // ever gets a chance to override it. Reset so this failure stub doesn't leak.
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("Should throw JWTCreationException when the signing certificate has expired")
    void getVPRequestJwt_WithExpiredCert_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_expired_cert";
        String dnsName = "test.example.com";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:" + dnsName, DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        // validity window entirely in the past — cert expired an hour ago
        long now = Instant.now().toEpochMilli();
        java.security.cert.X509Certificate expiredCert = TestCertUtil.generateSelfSignedCert(
                edKeyPair, dnsName,
                new java.util.Date(now - 1000L * 60 * 120),
                new java.util.Date(now - 1000L * 60 * 60));
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{expiredCert});

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    @DisplayName("Should throw JWTCreationException when an intermediate certificate in the chain has expired, even though the leaf is valid")
    void getVPRequestJwt_WithExpiredIntermediateCert_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_expired_intermediate_cert";
        String dnsName = "test.example.com";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:" + dnsName, DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        long now = Instant.now().toEpochMilli();

        // Leaf: valid window, correct SAN — would pass on its own.
        java.security.KeyPair leafKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        java.security.cert.X509Certificate leafCert = TestCertUtil.generateSelfSignedCert(leafKeyPair, dnsName);

        // Intermediate: expired an hour ago. No SAN needed — only the leaf's SAN is checked.
        java.security.KeyPair intermediateKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        java.security.cert.X509Certificate expiredIntermediateCert = TestCertUtil.generateSelfSignedCert(
                intermediateKeyPair, null,
                new java.util.Date(now - 1000L * 60 * 120),
                new java.util.Date(now - 1000L * 60 * 60));

        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{leafCert, expiredIntermediateCert});

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    @DisplayName("Should throw JWTCreationException when the signing certificate is not yet valid")
    void getVPRequestJwt_WithNotYetValidCert_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_not_yet_valid_cert";
        String dnsName = "test.example.com";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:" + dnsName, DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        // validity window entirely in the future — cert doesn't start for another hour
        long now = Instant.now().toEpochMilli();
        java.security.cert.X509Certificate futureCert = TestCertUtil.generateSelfSignedCert(
                edKeyPair, dnsName,
                new java.util.Date(now + 1000L * 60 * 60),
                new java.util.Date(now + 1000L * 60 * 120));
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{futureCert});

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    @DisplayName("createAuthorizationRequest should reject a non-HTTPS base URL for x509_san_dns outside local/dev")
    void createAuthorizationRequest_X509ClientId_NonHttpsNonLocalBaseUrl_ThrowsValidationException() throws Exception {
        String originalBaseUrl = service.verifyServiceBaseUrl;
        service.verifyServiceBaseUrl = "http://verify.acmecorp.example";
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_insecure", null, minimalDcqlQuery(), false);

            VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                    () -> service.createAuthorizationRequest(dto));
            assertEquals(ErrorCode.REQUEST_URI_INSECURE, ex.getErrorCode());
        } finally {
            service.verifyServiceBaseUrl = originalBaseUrl;
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should reject a hostless base URL for x509_san_dns, even with an https scheme")
    void createAuthorizationRequest_X509ClientId_HostlessBaseUrl_ThrowsValidationException() throws Exception {
        String originalBaseUrl = service.verifyServiceBaseUrl;
        service.verifyServiceBaseUrl = "https:///path"; // scheme present, no host — must not slip through
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_hostless", null, minimalDcqlQuery(), false);

            VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                    () -> service.createAuthorizationRequest(dto));
            assertEquals(ErrorCode.REQUEST_URI_INSECURE, ex.getErrorCode());
        } finally {
            service.verifyServiceBaseUrl = originalBaseUrl;
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should allow a non-HTTPS base URL for x509_san_dns on localhost (dev)")
    void createAuthorizationRequest_X509ClientId_NonHttpsLocalhostBaseUrl_Succeeds() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{
                        TestCertUtil.generateSelfSignedCert(edKeyPair, "test.example.com")});
        String originalBaseUrl = service.verifyServiceBaseUrl;
        service.verifyServiceBaseUrl = "http://localhost:8090";
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_local", null, minimalDcqlQuery(), false);

            VPRequestResponseDto response = service.createAuthorizationRequest(dto);

            assertNotNull(response);
            assertNotNull(response.getRequestUri());
        } finally {
            service.verifyServiceBaseUrl = originalBaseUrl;
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should allow an HTTPS base URL for x509_san_dns")
    void createAuthorizationRequest_X509ClientId_HttpsBaseUrl_Succeeds() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        java.security.KeyPair edKeyPair = java.security.KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[]{
                        TestCertUtil.generateSelfSignedCert(edKeyPair, "test.example.com")});
        String originalBaseUrl = service.verifyServiceBaseUrl;
        service.verifyServiceBaseUrl = "https://verify.acmecorp.example";
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "x509_san_dns:test.example.com", "tx_x509_https", null, minimalDcqlQuery(), false);

            VPRequestResponseDto response = service.createAuthorizationRequest(dto);

            assertNotNull(response);
            assertNotNull(response.getRequestUri());
        } finally {
            service.verifyServiceBaseUrl = originalBaseUrl;
            reset(mockKeyManagementService);
        }
    }

    @Test
    @DisplayName("createAuthorizationRequest should NOT enforce HTTPS for decentralized_identifier client_id, even with an insecure non-local base URL")
    void createAuthorizationRequest_DecentralizedIdentifierClientId_NonHttpsNonLocalBaseUrl_Succeeds() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        String originalBaseUrl = service.verifyServiceBaseUrl;
        // Deliberately insecure + non-local — would fail validateHttpsForX509SanDns if it applied
        // here, but that check is scoped to x509_san_dns only so this must succeed unchanged.
        service.verifyServiceBaseUrl = "http://verify.acmecorp.example";
        try {
            VPRequestCreateDto dto = new VPRequestCreateDto(
                    "decentralized_identifier:did:example:verifier", "tx_dec_id_http", null, minimalDcqlQuery(), false);

            VPRequestResponseDto response = service.createAuthorizationRequest(dto);

            assertNotNull(response);
            assertNotNull(response.getRequestUri());
        } finally {
            service.verifyServiceBaseUrl = originalBaseUrl;
        }
    }

    @Test
    @DisplayName("Should throw JWTCreationException when the keystore returns a non-null but empty certificate chain")
    void getVPRequestJwt_WithX509SanDns_EmptyCertChainArray_ThrowsJWTCreationException() throws Exception {
        String requestId = "req_x5c_empty_cert_array";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto(
                        "x509_san_dns:test.example.com", DcqlTestFixtures.minimalDcqlDto(), null, "nonce",
                        "https://resp.example/post", false, false);
        AuthorizationRequestCreateResponse authzResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() + 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(authzResponse));

        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);
        // Distinct from the "no cert chain configured" test: here the call succeeds but returns a
        // zero-length array, exercising the explicit certChain.length == 0 guard directly rather
        // than the catch(RuntimeException) branch.
        when(mockKeyManagementService.getCertificateChain())
                .thenReturn(new java.security.cert.X509Certificate[0]);

        assertThrows(io.inji.verify.exception.JWTCreationException.class, () -> service.getVPRequestJwt(requestId));
    }

    @Test
    void getVPRequestJwt_WithExpiredRequest_AllowsJwt() throws Exception {
        String requestId = "expiredReq";
        AuthorizationRequestResponseDto authzDto =
                new AuthorizationRequestResponseDto("did:example", DcqlTestFixtures.minimalDcqlDto(), null, "nonce", "responseUri", false, false);
        AuthorizationRequestCreateResponse expiredResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", authzDto, Instant.now().toEpochMilli() - 5000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(expiredResponse));
        OctetKeyPair mockOKP = new OctetKeyPairGenerator(Curve.Ed25519).generate();
        when(mockKeyManagementService.getKeyPair()).thenReturn(mockOKP);

        String jwt = service.getVPRequestJwt(requestId);
        assertNotNull(jwt);
    }

    @Test
    void getCurrentRequestStatus_WithExpiredRequest_ReturnsExpired() {
        String requestId = "expiredStatusReq";
        AuthorizationRequestCreateResponse expiredResponse =
                new AuthorizationRequestCreateResponse(requestId, "tx", null, Instant.now().toEpochMilli() - 1000);
        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(expiredResponse));
        when(mockVPSubmissionRepository.findById(requestId)).thenReturn(Optional.empty());

        VPRequestStatusDto status = service.getCurrentRequestStatus(requestId);

        assertEquals(VPRequestStatus.EXPIRED, status.getStatus());
    }

    @Test
    void shouldReturnStatusCompletedWhenSubmissionExists() {
        String requestId = "req_with_submission";
        AuthorizationRequestCreateResponse response =
                new AuthorizationRequestCreateResponse(requestId, "tx_id", null, Instant.now().toEpochMilli() + 10000);

        when(mockAuthorizationRequestCreateResponseRepository.findById(requestId)).thenReturn(Optional.of(response));
        when(mockVPSubmissionRepository.findById(requestId)).thenReturn(Optional.of(mock()));

        VPRequestStatusDto result = service.getCurrentRequestStatus(requestId);

        assertNotNull(result);
        assertEquals(VPRequestStatus.VP_SUBMITTED, result.getStatus());
    }

    @Test
    public void shouldCreateNewAuthorizationRequestWithResponseCodeValidationRequired() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                "test_transaction_id",
                null,
                minimalDcqlQuery(),
                true);

        VPRequestResponseDto responseDto = service.createAuthorizationRequest(vpRequestCreateDto);

        assertNotNull(responseDto);
        assertEquals("test_transaction_id", responseDto.getTransactionId());
        assertNotNull(responseDto.getRequestId());
        assertNotNull(responseDto.getAuthorizationDetails());
        assertTrue(responseDto.getAuthorizationDetails().isResponseCodeValidationRequired());
        assertTrue(responseDto.getExpiresAt() > Instant.now().toEpochMilli());
    }

    @Test
    void shouldUseProvidedNonce_whenValidNonceSupplied() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);
        String validNonce = "abcABC123-._~valid";  // 18 chars, all URL-safe

        VPRequestCreateDto dto = new VPRequestCreateDto("client", "tx", validNonce, minimalDcqlQuery(), false);

        VPRequestResponseDto response = service.createAuthorizationRequest(dto);

        assertEquals(validNonce, response.getAuthorizationDetails().getNonce());
    }

    @Test
    void shouldGenerateNonce_whenNonceIsNull() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto dto = new VPRequestCreateDto("client", "tx", null, minimalDcqlQuery(), false);

        VPRequestResponseDto response = service.createAuthorizationRequest(dto);

        assertNotNull(response.getAuthorizationDetails().getNonce());
        assertFalse(response.getAuthorizationDetails().getNonce().isBlank());
    }

    @Test
    void shouldGenerateNonce_whenNonceIsBlank() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto dto = new VPRequestCreateDto("client", "tx", "   ", minimalDcqlQuery(), false);

        VPRequestResponseDto response = service.createAuthorizationRequest(dto);

        // blank nonce must NOT be used — a generated nonce must replace it
        assertNotNull(response.getAuthorizationDetails().getNonce());
        assertFalse(response.getAuthorizationDetails().getNonce().isBlank());
        assertNotEquals("   ", response.getAuthorizationDetails().getNonce());
    }

    @Test
    void shouldCreateAuthorizationRequest_WithDecentralizedIdentifierClientId() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto dto = new VPRequestCreateDto(
                "decentralized_identifier:did:example:verifier",
                "tx_dec_id", null, minimalDcqlQuery(), false);

        VPRequestResponseDto response = service.createAuthorizationRequest(dto);

        assertNotNull(response);
        assertEquals("tx_dec_id", response.getTransactionId());
        // DID-based flow: requestUri is populated, authorizationDetails is null
        assertNull(response.getAuthorizationDetails());
        String requestUri = response.getRequestUri();
        assertNotNull(requestUri);
        // URI must embed the VP request path defined in Constants
        assertTrue(requestUri.contains(Constants.VP_REQUEST_URI),
                "requestUri should contain the VP request path '" + Constants.VP_REQUEST_URI + "' but was: " + requestUri);
        // URI must end with the actual requestId so the wallet can fetch the signed JWT
        String requestId = response.getRequestId();
        assertTrue(requestUri.endsWith("/" + requestId),
                "requestUri should end with '/" + requestId + "' but was: " + requestUri);
        // requestId must follow the expected prefix convention
        assertTrue(requestId.startsWith(Constants.REQUEST_ID_PREFIX),
                "requestId should start with '" + Constants.REQUEST_ID_PREFIX + "' but was: " + requestId);
    }

    @Test
    @DisplayName("createAuthorizationRequest should treat a client_id merely starting with the x509_san_dns "
            + "prefix (no colon) as a plain pre-registered client, not the by-reference x509_san_dns scheme")
    void createAuthorizationRequest_ClientIdWithX509SanDnsPrefixButNoColon_UsesEmbeddedFlow() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        // "x509_san_dnsfoo" starts with the raw prefix constant but isn't "x509_san_dns:<dns-name>" —
        // must not be misdetected as the x509_san_dns scheme (which would previously skip the
        // request-time host/HTTPS checks, then crash later at sign time with a confusing 500).
        VPRequestCreateDto dto = new VPRequestCreateDto(
                "x509_san_dnsfoo", "tx_x509_prefix_no_colon", null, minimalDcqlQuery(), false);

        VPRequestResponseDto response = service.createAuthorizationRequest(dto);

        assertNotNull(response);
        // Falls back to the plain/embedded flow: authorizationDetails populated, no requestUri.
        assertNotNull(response.getAuthorizationDetails());
        assertNull(response.getRequestUri());
    }

    @Test
    void shouldFail_whenNonceContainsDisallowedCharacters() throws Exception {
        VPRequestCreateDto dto = new VPRequestCreateDto("client", "tx", "invalid nonce value!", minimalDcqlQuery(), false);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> service.createAuthorizationRequest(dto));

        assertEquals(ErrorCode.NONCE_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenNonceIsTooShort() throws Exception {
        VPRequestCreateDto dto = new VPRequestCreateDto("client", "tx", "short", minimalDcqlQuery(), false);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> service.createAuthorizationRequest(dto));

        assertEquals(ErrorCode.NONCE_INVALID, ex.getErrorCode());
    }

    @Test
    public void shouldCreateNewAuthorizationRequestWithPresentationFlowCrossDevice() throws Exception {
        when(mockAuthorizationRequestCreateResponseRepository.save(any(AuthorizationRequestCreateResponse.class)))
                .thenReturn(null);

        VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                "test_client_id",
                "test_transaction_id",
                null,
                minimalDcqlQuery(),
                false);

        VPRequestResponseDto responseDto = service.createAuthorizationRequest(vpRequestCreateDto);

        assertNotNull(responseDto);
        assertEquals("test_transaction_id", responseDto.getTransactionId());
        assertNotNull(responseDto.getRequestId());
        assertNotNull(responseDto.getAuthorizationDetails());
        assertFalse(responseDto.getAuthorizationDetails().isResponseCodeValidationRequired());
        assertTrue(responseDto.getExpiresAt() > Instant.now().toEpochMilli());
    }
}
