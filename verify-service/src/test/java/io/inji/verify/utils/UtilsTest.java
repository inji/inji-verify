package io.inji.verify.utils;

import com.upokecenter.cbor.CBORObject;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.exception.InvalidCredentialException;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UtilsTest {

    @Test
    void coverPrivateConstructor() throws Exception {
        Constructor<Utils> constructor = Utils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Executable exec = () -> constructor.newInstance();
        assertDoesNotThrow(exec);
    }
    @Test
    void isCwt_shouldReturnFalse_whenCredentialContainsDot() {
        assertFalse(Utils.isCwt("abc.def"));
    }

    @Test
    void isCwt_shouldReturnFalse_whenCredentialStartsWithJson() {
        assertFalse(Utils.isCwt("{ \"key\": \"value\" }"));
    }

    @Test
    void isCwt_shouldReturnFalse_whenHexIsInvalid() {
        assertFalse(Utils.isCwt("ABC"));
    }

    @Test
    void isCwt_shouldReturnFalse_whenHexIsValidButNotCBOR() {
        assertFalse(Utils.isCwt("0A0B0C"));
    }

    @Test
    void hexToBytes_shouldThrowException_whenHexIsNull() throws Exception {
        Method method = Utils.class.getDeclaredMethod("hexToBytes", String.class);
        method.setAccessible(true);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(null, (Object) null));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("Hex string is null", ex.getCause().getMessage());
    }

    @Test
    void hexToBytes_shouldThrowException_whenHexLengthIsOdd() throws Exception {
        Method method = Utils.class.getDeclaredMethod("hexToBytes", String.class);
        method.setAccessible(true);

        Exception ex = assertThrows(Exception.class, () -> method.invoke(null, "ABC"));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("Invalid hex length", ex.getCause().getMessage());
    }

    @Test
    void hexToBytes_shouldWork_forValidHex() throws Exception {
        Method method = Utils.class.getDeclaredMethod("hexToBytes", String.class);
        method.setAccessible(true);

        byte[] result = (byte[]) method.invoke(null, "0A0B");
        assertArrayEquals(new byte[]{0x0A, 0x0B}, result);
    }

    @Test
    void hexToBytes_shouldIgnoreSpaces() throws Exception {
        Method method = Utils.class.getDeclaredMethod("hexToBytes", String.class);
        method.setAccessible(true);

        byte[] result = (byte[]) method.invoke(null, "0A 0B");
        assertArrayEquals(new byte[]{0x0A, 0x0B}, result);
    }

    @Test
    void testExcludeMetaClaimsCoverage() throws Exception {
        Method method = Utils.class.getDeclaredMethod("excludeMetaClaims", List.class, Map.class);
        method.setAccessible(true);

        Map<String, Object> claims = new HashMap<>();
        claims.put("stay", "safe");
        claims.put("remove", "meta");

        assertDoesNotThrow(() -> method.invoke(null, null, claims));

        List<String> metaWithNull = Collections.singletonList(null);
        assertDoesNotThrow(() -> method.invoke(null, metaWithNull, claims));

        List<String> validMeta = List.of("  remove  ");
        method.invoke(null, validMeta, claims);

        assertFalse(claims.containsKey("remove"), "Claim should be removed");
        assertEquals(1, claims.size());

    }

    @Test
    void shouldReturnCwtFormatByMocking() {
        try (var mockedUtils = mockStatic(Utils.class)) {
            mockedUtils.when(() -> Utils.isCwt("abcabcabcbabc")).thenReturn(true);
            mockedUtils.when(() -> Utils.getCredentialFormat("abcabcabcbabc")).thenCallRealMethod();

            assertEquals(CredentialFormat.CWT_VC, Utils.getCredentialFormat("abcabcabcbabc"));
        }
    }

    @Test
    void shouldReturnSdJwtFormatForValidSdJwtString() {
        String sdJwt = "eyJ0eXAiOiJ2YytzZC1qd3QifQ.payload.signature~disclosure";
        CredentialFormat format = Utils.getCredentialFormat(sdJwt);
        assertEquals(CredentialFormat.VC_SD_JWT, format);
    }

    @Test
    void shouldReturnLdpVcForStandardJson() {
        String safeJson = "eyJuYW1lIjogInRlc3QifQ==.payload.signature";
        CredentialFormat format = Utils.getCredentialFormat(safeJson);
        assertEquals(CredentialFormat.LDP_VC, format);
    }

    @Test
    void shouldThrowInvalidCredentialExceptionOnNull() {
        assertThrows(InvalidCredentialException.class, () -> {
            Utils.getCredentialFormat(null);
        });
    }

    @Test
    void testDecodeCwt_Success() throws Exception {
        String hexInput = "A1616101";
        Method method = Utils.class.getDeclaredMethod("decodeCwt", String.class);
        method.setAccessible(true);

        CBORObject result = (CBORObject) method.invoke(null, hexInput);

        assertNotNull(result);
        assertEquals(1, result.get(CBORObject.FromObject("a")).AsInt32());
    }

    @Test
    void testDecodeCwtClaims_Success() throws Exception {
        CBORObject payload = CBORObject.NewMap();
        payload.Add("sub", "123");
        byte[] payloadBytes = payload.EncodeToBytes();

        CBORObject coseArray = CBORObject.NewArray();
        coseArray.Add(0);
        coseArray.Add(0);
        coseArray.Add(payloadBytes);
        coseArray.Add(new byte[0]);

        Method method = Utils.class.getDeclaredMethod("decodeCwtClaims", CBORObject.class);
        method.setAccessible(true);

        CBORObject result = (CBORObject) method.invoke(null, coseArray);

        assertNotNull(result);
        assertEquals("123", result.get(CBORObject.FromObject("sub")).AsString());
    }

    @Test
    void populateStatusCheckDtoList_shouldReturnEmptyList_whenInputIsNull() {
        List<StatusCheckDto> result = Utils.populateStatusCheckDtoList(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void populateStatusCheckDtoList_shouldHandleNullCredentialStatusResult() {
        Map<String, CredentialStatusResult> map = new HashMap<>();
        map.put("revocation", null);

        List<StatusCheckDto> result = Utils.populateStatusCheckDtoList(map);

        assertEquals(1, result.size());

        StatusCheckDto dto = result.get(0);
        assertEquals("revocation", dto.getPurpose());
        assertFalse(dto.isValid());
        assertNotNull(dto.getError());
        assertEquals("NULL_STATUS_RESULT", dto.getError().getErrorCode());
        assertEquals("Credential status result was null.", dto.getError().getErrorMessage());
    }

    @Test
    void populateStatusCheckDtoList_shouldHandleValidResult_withoutError() {
        CredentialStatusResult mockResult = mock(CredentialStatusResult.class);
        when(mockResult.isValid()).thenReturn(true);
        when(mockResult.getError()).thenReturn(null);

        Map<String, CredentialStatusResult> map = Map.of("revocation", mockResult);

        List<StatusCheckDto> result = Utils.populateStatusCheckDtoList(map);

        assertEquals(1, result.size());

        StatusCheckDto dto = result.get(0);
        assertEquals("revocation", dto.getPurpose());
        assertTrue(dto.isValid());
        assertNull(dto.getError());
    }


    @Test
    void extractClaims_shouldCallLdpBranch() {
        String jsonCredential = "{ \"credentialSubject\": { \"name\": \"John\" } }";

        Map<String, Object> result = Utils.extractClaims(
                jsonCredential,
                CredentialFormat.LDP_VC,
                null,
                null
        );

        assertNotNull(result);
        assertEquals("John", result.get("name"));
    }

    @Test
    void extractClaims_shouldCallCwtBranch() {
        String credential = "dummyCwt";
        List<String> metaClaims = List.of("meta");

        Map<String, Object> expected = Map.of("id", "123");

        PixelPass pixelPass = mock(PixelPass.class);

        try (var mockedUtils = mockStatic(Utils.class, CALLS_REAL_METHODS)) {

            mockedUtils.when(() ->
                    Utils.extractCwtClaims(credential, pixelPass, metaClaims)
            ).thenReturn(expected);

            Map<String, Object> result = Utils.extractClaims(
                    credential,
                    CredentialFormat.CWT_VC,
                    metaClaims,
                    pixelPass
            );

            assertEquals(expected, result);
        }
    }

    @Test
    void extractClaims_shouldReturnLdpClaims() {
        String json = "{ \"credentialSubject\": { \"name\": \"John\" } }";

        Map<String, Object> result = Utils.extractClaims(
                json,
                CredentialFormat.LDP_VC,
                null,
                null
        );

        assertNotNull(result);
        assertEquals("John", result.get("name"));
    }
}
