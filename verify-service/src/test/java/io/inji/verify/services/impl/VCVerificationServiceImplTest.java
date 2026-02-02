package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.*;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.utils.Utils;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
public class VCVerificationServiceImplTest {

    static VCVerificationServiceImpl service;
    static CredentialsVerifier mockCredentialsVerifier;
    static PixelPass mockPixelPass;
    private final String TEST_CWT_VC_STRING = "abcabcabcabcabacabac";
    private final String TEST_JSON_VC_STRING = "{\"credentialSubject\":{},\"type\":[\"VerifiableCredential\",\"MockVerifiableCredential\"],\"credentialStatus\":{},\"proof\":{}}";
    private final String TEST_SDJWT_VC_STRING = "abcdabcdabcdabcdabcd";
    @BeforeAll
    public static void beforeAll() {
        mockCredentialsVerifier = mock(CredentialsVerifier.class);
        mockPixelPass = mock(PixelPass.class);
        service = new VCVerificationServiceImpl(mockCredentialsVerifier,mockPixelPass);
    }

    @Nested
    class VerifyTests {
        @Test
        public void shouldReturnSuccessForLDPVerifiedVc() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(true, "", "");
            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_JSON_VC_STRING),
                    eq(CredentialFormat.LDP_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_JSON_VC_STRING, "application/ldp+json");
                assertEquals(VerificationStatus.SUCCESS, result.getVerificationStatus());
        }

        @Test
        public void shouldReturnInvalidForLDPVcWhichIsInvalid() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(false, "", "");
            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of());

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_JSON_VC_STRING),
                    eq(CredentialFormat.LDP_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_JSON_VC_STRING, "application/ldp+json");
                assertEquals(VerificationStatus.INVALID, result.getVerificationStatus());
        }

        @Test
        public void shouldReturnRevokedForRevokedLDPVc() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(true, "", "");
            CredentialStatusResult revokedStatus = mock(CredentialStatusResult.class);
            when(revokedStatus.isValid()).thenReturn(false);
            when(revokedStatus.getError()).thenReturn(null);

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);

            when(summary.getCredentialStatus()).thenReturn(Map.of("revocation", revokedStatus));

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_JSON_VC_STRING),
                    eq(CredentialFormat.LDP_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_JSON_VC_STRING, "application/ldp+json");
                assertEquals(VerificationStatus.REVOKED, result.getVerificationStatus());
        }

        @Test
        public void shouldUseLDPFormatForOtherContentTypes() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(true, "", "");

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of());

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_JSON_VC_STRING),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(summary);

                VCVerificationStatusDto statusDto = service.verify(TEST_JSON_VC_STRING, "application/other");
                assertEquals(VerificationStatus.SUCCESS, statusDto.getVerificationStatus());
        }

        @Test
        public void shouldReturnExpiredForLDPVcWhichIsExpired() throws CredentialStatusCheckException {
            VerificationResult vResult = mock(VerificationResult.class);
            when(vResult.getVerificationStatus()).thenReturn(true);
            when(vResult.getVerificationMessage()).thenReturn("EXPIRED");
            when(vResult.getVerificationErrorCode()).thenReturn("ERR_VC_EXPIRED");

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of());

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_JSON_VC_STRING),
                    eq(CredentialFormat.LDP_VC),
                    anyList()))
                    .thenReturn(summary);
            when(vResult.getVerificationMessage()).thenReturn("EXPIRED");
            when(vResult.getVerificationErrorCode()).thenReturn("ERR_VC_EXPIRED");

                VCVerificationStatusDto result =
                        service.verify(TEST_JSON_VC_STRING, "application/ldp+json");

                assertEquals(VerificationStatus.EXPIRED, result.getVerificationStatus());
        }

        @Test
        public void shouldReturnSuccessForCWTVerifiedVc() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(true, "", "");
            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.SUCCESS, result.getVerificationStatus());
        }

        @Test
        public void shouldReturnInvalidForCWTVcWhichIsInvalid() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(false, "", "");

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of());

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.INVALID, result.getVerificationStatus());
        }


        @Test
        public void shouldReturnRevokedForRevokedCWTVc() throws CredentialStatusCheckException {
            VerificationResult vResult = new VerificationResult(true, "", "");
            CredentialStatusResult revokedStatus = mock(CredentialStatusResult.class);
            when(revokedStatus.isValid()).thenReturn(false);
            when(revokedStatus.getError()).thenReturn(null);

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);

            when(summary.getCredentialStatus()).thenReturn(Map.of("revocation", revokedStatus));

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList()))
                    .thenReturn(summary);

                VCVerificationStatusDto result = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.REVOKED, result.getVerificationStatus());
        }

        @Test
        public void shouldReturnExpiredForCWTVcWhichIsExpired() throws CredentialStatusCheckException {
            VerificationResult vResult = mock(VerificationResult.class);
            when(vResult.getVerificationStatus()).thenReturn(true);
            when(vResult.getVerificationMessage()).thenReturn("EXPIRED");
            when(vResult.getVerificationErrorCode()).thenReturn("ERR_VC_EXPIRED");

            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(vResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of());

            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList()))
                    .thenReturn(summary);
            when(vResult.getVerificationMessage()).thenReturn("EXPIRED");
            when(vResult.getVerificationErrorCode()).thenReturn("ERR_VC_EXPIRED");

            VCVerificationStatusDto result =
                    service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");

            assertEquals(VerificationStatus.EXPIRED, result.getVerificationStatus());
        }
    }

    @Nested
    class VerifyV2Tests {
        @Test
        void shouldReturnAllChecksTrueForVerifiedVC() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);
            request.setSkipStatusChecks(true);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
                utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
                utilsMock.when(() -> Utils.populateAllChecksSuccessful(any(), any(), any(), any())).thenCallRealMethod();
                VCVerificationResultDto result = service.verifyV2(request);

                assertTrue(result.isAllChecksSuccessful());
                assertTrue(result.getSchemaAndSignatureCheck().isValid());
                assertTrue(result.getExpiryCheck().isValid());
                assertTrue(result.getStatusCheck().isEmpty());
            }
        }

        @Test
        void verifyV2_success_skipStatusChecks_false() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
            when(statusResult.isValid()).thenReturn(true);
            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(verificationResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of("revocation", statusResult));
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(anyString(), any(CredentialFormat.class), anyList()))
                    .thenReturn(summary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getCredentialFormat(TEST_JSON_VC_STRING)).thenReturn(CredentialFormat.LDP_VC);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
                utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful());
                assertTrue(result.getSchemaAndSignatureCheck().isValid());
                assertTrue(result.getExpiryCheck().isValid());
                assertTrue(result.getStatusCheck().isEmpty());
            }
        }

        @Test
        void verifyV2_failure_invalidSignature() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);
            request.setSkipStatusChecks(true);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(false);
            when(verificationResult.getVerificationErrorCode()).thenReturn("SOME_ERROR");
            when(verificationResult.getVerificationMessage()).thenReturn("Some error message");


            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(false, null));

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful());
                assertFalse(result.getSchemaAndSignatureCheck().isValid());
            }
        }

        @Test
        void verifyV2_failure_statusCheckFails() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);
            request.setStatusCheckFilters(List.of("revocation"));

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
            when(statusResult.isValid()).thenReturn(false);
            CredentialVerificationSummary summary = mock(CredentialVerificationSummary.class);
            when(summary.getVerificationResult()).thenReturn(verificationResult);
            when(summary.getCredentialStatus()).thenReturn(Map.of("revocation", statusResult));
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(anyString(), any(CredentialFormat.class), anyList()))
                    .thenReturn(summary);
            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getCredentialFormat(TEST_JSON_VC_STRING)).thenReturn(CredentialFormat.LDP_VC);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
                utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
                utilsMock.when(() -> Utils.getVcVerificationStatus(summary)).thenReturn(VerificationStatus.REVOKED);
                utilsMock.when(() -> Utils.populateStatusCheckDtoList(summary.getCredentialStatus())).thenReturn(List.of(new StatusCheckDto("revocation", false, null)));
                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful(), "Overall check should fail");
                assertTrue(result.getSchemaAndSignatureCheck().isValid(), "Schema check should be valid");
                assertTrue(result.getExpiryCheck().isValid(), "Expiry check should be valid");
                assertFalse(result.getStatusCheck().isEmpty());
            }
        }

        @Test
        void verifyV2_success_shouldReturnExtractedClaims() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);
            request.setSkipStatusChecks(true);
            request.setIncludeClaims(true);
            Map<String, Object> expectedClaims = Map.of("VID", 9876543210L, "email", "siddhartha.km@gmail.com");

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class))).thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getCredentialFormat(anyString())).thenReturn(CredentialFormat.LDP_VC);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
                utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
                utilsMock.when(() -> Utils.populateAllChecksSuccessful(any(), any(), any(), any())).thenCallRealMethod();
                utilsMock.when(() -> Utils.extractClaims(anyString(), any(), any(),any())).thenReturn(expectedClaims);

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.getClaims().isEmpty());
                assertEquals(9876543210L, result.getClaims().get("VID"));
            }
        }

        @Test
        void verifyV2_success_extractSdJwtClaims_withMetaFiltering() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_SDJWT_VC_STRING);
            request.setIncludeClaims(true);
            request.setSkipStatusChecks(true);
            Map<String, Object> expectedClaims = Map.of("VID", 9876543210L, "email", "siddhartha.km@gmail.com");

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getCredentialFormat(TEST_SDJWT_VC_STRING)).thenReturn(CredentialFormat.VC_SD_JWT);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));
                utilsMock.when(() -> Utils.populateExpiryCheck(any())).thenReturn(new ExpiryCheckDto(true));
                utilsMock.when(() -> Utils.extractClaims(anyString(), any(), any(),any())).thenReturn(expectedClaims);

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.getClaims().isEmpty());
            }
        }

        @Test
        void verifyV2_success_shouldNotIncludeClaimsWhenFlagIsFalse() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_JSON_VC_STRING);
            request.setIncludeClaims(false);
            request.setSkipStatusChecks(true);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class))).thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getCredentialFormat(TEST_JSON_VC_STRING)).thenReturn(CredentialFormat.LDP_VC);
                utilsMock.when(() -> Utils.populateSchemaAndSignature(any())).thenReturn(new SchemaAndSignatureCheckDto(true, null));

                VCVerificationResultDto result = service.verifyV2(request);

                assertTrue(result.getClaims().isEmpty());
            }
        }

        @Test
        void verifyV2_success_shouldReturnExtractedClaims_forCwt() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_CWT_VC_STRING);
            request.setSkipStatusChecks(true);
            request.setIncludeClaims(true);

            Map<String, Object> expectedClaims =
                    Map.of("VID", 9876543210L, "email", "siddhartha.km@gmail.com");

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {

                utilsMock.when(() -> Utils.getCredentialFormat(TEST_CWT_VC_STRING))
                        .thenReturn(CredentialFormat.CWT_VC);

                utilsMock.when(() -> Utils.populateSchemaAndSignature(any()))
                        .thenReturn(new SchemaAndSignatureCheckDto(true, null));

                utilsMock.when(() -> Utils.populateExpiryCheck(any()))
                        .thenReturn(new ExpiryCheckDto(true));

                utilsMock.when(() -> Utils.populateAllChecksSuccessful(any(), any(), any(), any()))
                        .thenCallRealMethod();

                utilsMock.when(() ->
                                Utils.extractClaims(anyString(), any(), any(), any()))
                        .thenReturn(expectedClaims);

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.getClaims().isEmpty());
                assertEquals(9876543210L, result.getClaims().get("VID"));
                assertEquals("siddhartha.km@gmail.com", result.getClaims().get("email"));
            }
        }

        @Test
        void verifyV2_success_shouldNotIncludeClaims_forCwt_whenFlagIsFalse() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(TEST_CWT_VC_STRING);
            request.setIncludeClaims(false);
            request.setSkipStatusChecks(true);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {

                utilsMock.when(() -> Utils.getCredentialFormat(TEST_CWT_VC_STRING))
                        .thenReturn(CredentialFormat.CWT_VC);

                utilsMock.when(() -> Utils.populateSchemaAndSignature(any()))
                        .thenReturn(new SchemaAndSignatureCheckDto(true, null));

                VCVerificationResultDto result = service.verifyV2(request);

                assertTrue(result.getClaims().isEmpty());
            }
        }

    }
}
