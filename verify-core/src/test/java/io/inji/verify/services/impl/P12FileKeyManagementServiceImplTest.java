package io.inji.verify.services.impl;

import com.nimbusds.jose.jwk.OctetKeyPair;
import io.inji.verify.key.impl.P12KeyExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class P12FileKeyManagementServiceImplTest {

    P12KeyExtractor extractor;
    P12FileKeyManagementServiceImpl service;
    KeyPair keyPair;

    @BeforeEach
    void setup() throws Exception {
        extractor = mock(P12KeyExtractor.class);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        keyPair = kpg.generateKeyPair();
        when(extractor.extractKeyPair()).thenReturn(keyPair);

        service = new P12FileKeyManagementServiceImpl(extractor);
        service.loadKey(); // simulate @PostConstruct
    }

    @Test
    void getKeyPair_ShouldReturnOctetKeyPair() {
        OctetKeyPair okp = service.getKeyPair();
        assertNotNull(okp);
        assertEquals("Ed25519", okp.getCurve().getName());
    }

    @Test
    void getKeyPair_WhenKeyNotLoaded_ShouldThrowException() {
        P12FileKeyManagementServiceImpl emptyService = new P12FileKeyManagementServiceImpl(extractor);
        IllegalStateException ex = assertThrows(IllegalStateException.class, emptyService::getKeyPair);
        assertEquals("Ed25519 Key not loaded.", ex.getMessage());
    }

    @Test
    void getKeyPair_WhenExceptionDuringConversion_ShouldReturnNull() throws Exception {
        KeyPair faultyKeyPair = mock(KeyPair.class);
        when(faultyKeyPair.getPrivate()).thenThrow(new RuntimeException("private key error"));
        when(extractor.extractKeyPair()).thenReturn(faultyKeyPair);

        P12FileKeyManagementServiceImpl faultyService = new P12FileKeyManagementServiceImpl(extractor);
        faultyService.loadKey();

        assertNull(faultyService.getKeyPair());
    }
}
