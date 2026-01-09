package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.VCVerificationRequestBodyDto;
import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.dto.verification.VCVerificationResultDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class VCVerificationServiceImplTest {

    static VCVerificationServiceImpl service;
    static CredentialsVerifier mockCredentialsVerifier;

    @BeforeAll
    public static void beforeAll() {
        mockCredentialsVerifier = mock(CredentialsVerifier.class);
        service = new VCVerificationServiceImpl(mockCredentialsVerifier);
    }

    @Nested
    class VerifyTests {
        @Test
        public void shouldReturnSuccessForVerifiedVc() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    anyString(),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.SUCCESS);

                VCVerificationStatusDto statusDto = service.verify("some_vc", "application/ldp+json");
                assertEquals(VerificationStatus.SUCCESS, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnExpiredForVerifiedVcWhichIsExpired() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    anyString(),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.EXPIRED);

                VCVerificationStatusDto statusDto = service.verify("some_vc", "application/ldp+json");
                assertEquals(VerificationStatus.EXPIRED, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnInvalidForVcWhichIsInvalid() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    anyString(),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.INVALID);

                VCVerificationStatusDto statusDto = service.verify("some_vc", "application/ldp+json");
                assertEquals(VerificationStatus.INVALID, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldUseLDPFormatForOtherContentTypes() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    anyString(),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.SUCCESS);

                VCVerificationStatusDto statusDto = service.verify("some_vc", "application/other");
                assertEquals(VerificationStatus.SUCCESS, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnRevokedForRevokedVc() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    anyString(),
                    eq(CredentialFormat.LDP_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.REVOKED);

                VCVerificationStatusDto statusDto = service.verify("some_vc", "application/ldp+json");
                assertEquals(VerificationStatus.REVOKED, statusDto.getVerificationStatus());
            }
        }

    }

    @Nested
    class VerifyV2Tests {

        @Test
        void verifyV2_success_skipStatusChecks_true() {
            VCVerificationRequestBodyDto request =
                    new VCVerificationRequestBodyDto("some-vc", true, List.of(), false);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);
                VCVerificationResultDto result = service.verifyV2(request);

                assertTrue(result.isAllChecksSuccessful());
                assertTrue(result.getSchemaAndSignatureCheck().isValid());
                assertTrue(result.getExpiryCheck().isValid());
                assertTrue(result.getStatusCheck().isEmpty());
            }
        }

        @Test
        void verifyV2_success_skipStatusChecks_false() {
            VCVerificationRequestBodyDto request =
                    new VCVerificationRequestBodyDto("some-vc", false, List.of(), false);

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
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);
                VCVerificationResultDto result = service.verifyV2(request);

                assertTrue(result.isAllChecksSuccessful());
                assertTrue(result.getSchemaAndSignatureCheck().isValid());
                assertTrue(result.getExpiryCheck().isValid());
                assertFalse(result.getStatusCheck().isEmpty());
                assertTrue(result.getStatusCheck().getFirst().isValid());
            }
        }

        @Test
        void verifyV2_failure_invalidSignature() {
            VCVerificationRequestBodyDto request =
                    new VCVerificationRequestBodyDto("some-vc",true,List.of(),false);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(false);
            when(verificationResult.getVerificationErrorCode()).thenReturn("SOME_ERROR");
            when(verificationResult.getVerificationMessage()).thenReturn("Some error message");


            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful());
                assertFalse(result.getSchemaAndSignatureCheck().isValid());
            }
        }

        @Test
        void verifyV2_failure_statusCheckFails() {
            VCVerificationRequestBodyDto request =
                    new VCVerificationRequestBodyDto("some-vc", false, List.of("revocation"), false);
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
                utilsMock.when(() -> Utils.isSdJwt(anyString()))
                        .thenReturn(false);
                utilsMock.when(() -> Utils.getVcVerificationStatus(summary))
                        .thenReturn(VerificationStatus.SUCCESS);
                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful(), "Overall check should fail");
                assertTrue(result.getSchemaAndSignatureCheck().isValid(), "Schema check should be valid");
                assertTrue(result.getExpiryCheck().isValid(), "Expiry check should be valid");
                assertFalse(result.getStatusCheck().getFirst().isValid(), "Status check (revocation) should be invalid");
            }
        }

        @Test
        void verifyV2_verification_throws_exception() {
            VCVerificationRequestBodyDto request = new VCVerificationRequestBodyDto("some-vc", true, List.of(), false);

            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenThrow(new RuntimeException("Verification failed"));

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);

                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful());
                assertFalse(result.getSchemaAndSignatureCheck().isValid());
                assertEquals("VERIFICATION_FAILED", result.getSchemaAndSignatureCheck().getError().getErrorCode());
                assertEquals("Verification failed", result.getSchemaAndSignatureCheck().getError().getErrorMessage());
            }
        }
    }
}
