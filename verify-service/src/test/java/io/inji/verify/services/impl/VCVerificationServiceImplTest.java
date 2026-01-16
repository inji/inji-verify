package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.StatusCheckDto;
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
        String vc = "{\"credentialSubject\":{\"VID\":9876543210,\"gender\":[{\"language\":\"eng\",\"value\":\"Male\"},{\"language\":\"fra\",\"value\":\"Mâle\"},{\"language\":\"ara\",\"value\":\"ذكر\"}],\"province\":[{\"language\":\"eng\",\"value\":\"yuanwee\"}],\"phone\":\"+919427357934\",\"postalCode\":45009,\"fullName\":[{\"language\":\"fra\",\"value\":\"Siddharth K Mansour\"},{\"language\":\"ara\",\"value\":\"تتگلدكنسَزقهِقِفل دسييسيكدكنوڤو\"},{\"language\":\"eng\",\"value\":\"Siddharth K Mansour\"}],\"dateOfBirth\":\"1987/11/25\",\"id\":\"did:jwk:eyJrdHkiOiJSU0EiLCJlIjoiQVFBQiIsInVzZSI6InNpZyIsImtpZCI6IkZ2WU5MSWR3b1VESUp6cG9IelNadXNHV215VkV2Vnk5UmZ3TWY0VU9OODQiLCJhbGciOiJSUzI1NiIsIm4iOiJ3akJCcmNKTThjRHAwTTVQVy1yWmRMNExQdk95TzZvQ292YmR0SlBKTzEwNHVEV0Rib2JFbU5nYkwxOHN1cDlFZFJYLTdIVWxQR1hGSGJINXhZamhJWFl3bDF2NUJCRDVqSjBseUxWQnB5YjczSFE3SHRVaEhaTWxuNUtMNXluUW9wLVctVDNvYWxBRVFRTmJxTHhuUGJRVEZORjItSEpRYUxlOXg2bDdWeVdidG9SSjhCaV9mLTBXaW9WWF9NUk9OQWpjNTlLMzgwZ2VWYmg0YjExaWdzVTk2TnR2OENBTzVpcHBZcXhZdDE0UUd2WHVaczBwcTdiV05GbWFGZGd5X3dPa3E1UXdYbVdkWGo3NWpLNWprNXRiV1NReV9icEZPUnR1MWFmZmNaWXF5enpSOXZrbGZieU9NM0gtQVZmU3JpR3lmOVdkMU95elVVQTJkdkFLc1EifQ==\",\"UIN\":9876543210,\"region\":[{\"language\":\"eng\",\"value\":\"yuanwee\"}],\"email\":\"siddhartha.km@gmail.com\"},\"validUntil\":\"2028-01-08T07:19:56.385Z\",\"validFrom\":\"2026-01-08T07:19:56.385Z\",\"id\":\"https://mosip.io/credential/ec7abcf7-2dbd-4124-b860-971621452d2b\",\"type\":[\"VerifiableCredential\",\"MockVerifiableCredential\"],\"@context\":[\"https://www.w3.org/ns/credentials/v2\",\"https://api.released.mosip.net/.well-known/mosip-ida-context.json\",\"https://w3id.org/security/suites/ed25519-2020/v1\"],\"issuer\":\"did:web:inji.github.io:inji-config:dev-int-inji:mock-identity\",\"credentialStatus\":{\"statusPurpose\":\"revocation\",\"statusListIndex\":\"94010\",\"id\":\"https://injicertify-mock.dev-int-inji.mosip.net/v1/certify/credentials/status-list/25b62812-7d5d-4660-b233-5e2d1263c83a#94010\",\"type\":\"BitstringStatusListEntry\",\"statusListCredential\":\"https://injicertify-mock.dev-int-inji.mosip.net/v1/certify/credentials/status-list/25b62812-7d5d-4660-b233-5e2d1263c83a\"},\"proof\":{\"type\":\"Ed25519Signature2020\",\"created\":\"2026-01-08T07:19:56Z\",\"proofPurpose\":\"assertionMethod\",\"verificationMethod\":\"did:web:inji.github.io:inji-config:dev-int-inji:mock-identity#BF6EnRlmWN2x2T0HsTBbDtbci_dD2qYPdtxlVpv2Lz8\",\"proofValue\":\"z2jBgb8JmCH3b5LyfeqF1gFsgBTmQcr18hTD98SDGhoL3Nrr8vwjWw7iGxVECYz9yD5SWCrPP83YAQ81yH2WvDY1b\"}}";

        @Test
        void shouldReturnAllChecksTrueForVerifiedVC() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(vc);
            request.setSkipStatusChecks(true);

            VerificationResult verificationResult = mock(VerificationResult.class);
            when(verificationResult.getVerificationStatus()).thenReturn(true);
            when(mockCredentialsVerifier.verify(anyString(), any(CredentialFormat.class)))
                    .thenReturn(verificationResult);

            try (MockedStatic<Utils> utilsMock = mockStatic(Utils.class)) {
                utilsMock.when(() -> Utils.isSdJwt(anyString())).thenReturn(false);
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
            VCVerificationRequestDto request = new VCVerificationRequestDto(vc);

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

                assertFalse(result.isAllChecksSuccessful());
                assertTrue(result.getSchemaAndSignatureCheck().isValid());
                assertTrue(result.getExpiryCheck().isValid());
                assertTrue(result.getStatusCheck().isEmpty());
            }
        }

        @Test
        void verifyV2_failure_invalidSignature() {
            VCVerificationRequestDto request = new VCVerificationRequestDto(vc);
            request.setSkipStatusChecks(true);

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
            VCVerificationRequestDto request = new VCVerificationRequestDto(vc);
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
                utilsMock.when(() -> Utils.isSdJwt(anyString()))
                        .thenReturn(false);
                utilsMock.when(() -> Utils.getVcVerificationStatus(summary))
                        .thenReturn(VerificationStatus.REVOKED);
                utilsMock.when(() -> Utils.populateStatusCheck(summary.getCredentialStatus()))
                        .thenReturn(List.of(new StatusCheckDto("revocation", false, null)));
                VCVerificationResultDto result = service.verifyV2(request);

                assertFalse(result.isAllChecksSuccessful(), "Overall check should fail");
                assertTrue(result.getSchemaAndSignatureCheck().isValid(), "Schema check should be valid");
                assertTrue(result.getExpiryCheck().isValid(), "Expiry check should be valid");
                assertFalse(result.getStatusCheck().isEmpty());
            }
        }
    }
}
