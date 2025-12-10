package io.inji.verify.services.impl;

import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.utils.Utils;
import io.mosip.vercred.vcverifier.CredentialsVerifier;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class VCVerificationServiceImplTest {

    static VCVerificationServiceImpl service;
    static CredentialsVerifier mockCredentialsVerifier;

    @BeforeAll
    public static void beforeAll() {
        mockCredentialsVerifier = mock(CredentialsVerifier.class);
        service = new VCVerificationServiceImpl(mockCredentialsVerifier);
    }

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
