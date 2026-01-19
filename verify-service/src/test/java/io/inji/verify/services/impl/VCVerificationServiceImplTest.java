package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.VCVerificationRequestDto;
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
    private final String TEST_CWT_VC_STRING ="d83dd2845831a2012604582b5455444c4e4e516b30324a766c4a6d376174774847414332644d363351333259482d2d6d3976574f42746ba058efa501782e68747470733a2f2f3232316633386363336666632e6e67726f6b2d667265652e6170702f76312f63657274696679041a6a57282b051a6969da2b061a6969da2b18a958a7ac016a33393138353932343338020a046c4a616e61726468616e204253086a30342d31382d3139383409010a78294e657720486f7573652c204e656172204d6574726f204c696e652c2042656e67616c7572752c204b410b756a616e61726468616e406578616d706c652e636f6d0c6d2b3931393837363534333231300d62494e183ea3006435323439010002001841a3006435323439010202006568656c6c6f65776f726c64584040a10df719e0fa3079a0ecceb08d4dc6877c4c8c622bf760169a89ee4059b8989e64d733ac28096927c6ac38176cfe50036f2ff3dedcf789116285ff9f18ce0a";

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

        @Test
        public void shouldReturnSuccessForCWTVerifiedVc() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.SUCCESS);

                VCVerificationStatusDto statusDto = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.SUCCESS, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnExpiredForVerifiedCWTVcWhichIsExpired() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.EXPIRED);

                VCVerificationStatusDto statusDto = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.EXPIRED, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnInvalidForCWTVcWhichIsInvalid() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.INVALID);

                VCVerificationStatusDto statusDto = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.INVALID, statusDto.getVerificationStatus());
            }
        }

        @Test
        public void shouldReturnRevokedForRevokedCWTVc() throws CredentialStatusCheckException {
            CredentialVerificationSummary mockSummary = mock(CredentialVerificationSummary.class);
            when(mockCredentialsVerifier.verifyAndGetCredentialStatus(
                    eq(TEST_CWT_VC_STRING),
                    eq(CredentialFormat.CWT_VC),
                    anyList())
            ).thenReturn(mockSummary);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.getVcVerificationStatus(mockSummary))
                        .thenReturn(VerificationStatus.REVOKED);

                VCVerificationStatusDto statusDto = service.verify(TEST_CWT_VC_STRING, "application/vc+cwt");
                assertEquals(VerificationStatus.REVOKED, statusDto.getVerificationStatus());
            }
        }
    }

    @Nested
    class VerifyV2Tests {

        @Test
        void verifyV2_success_skipStatusChecks_true() {
            VCVerificationRequestDto request = new VCVerificationRequestDto("some-vc", true, List.of(), false);

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
            VCVerificationRequestDto request =
                    new VCVerificationRequestDto("some-vc", false, List.of(), false);

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
            VCVerificationRequestDto request =
                    new VCVerificationRequestDto("some-vc",true,List.of(),false);

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
            VCVerificationRequestDto request =
                    new VCVerificationRequestDto("some-vc", false, List.of("revocation"), false);
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
    }
}
