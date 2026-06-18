package io.inji.verify.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upokecenter.cbor.CBORObject;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.dto.result.HolderProofCheckDto;
import io.inji.verify.dto.verification.ExpiryCheckDto;
import io.inji.verify.dto.verification.SchemaAndSignatureCheckDto;
import io.inji.verify.dto.verification.StatusCheckDto;
import io.inji.verify.exception.CredentialStatusCheckException;
import io.inji.verify.exception.InvalidCredentialException;
import io.inji.verify.shared.Constants;
import io.mosip.pixelpass.PixelPass;
import io.mosip.vercred.vcverifier.constants.CredentialFormat;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.exception.StatusCheckErrorCode;
import io.mosip.vercred.vcverifier.exception.StatusCheckException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
        // eyJ0eXAiOiJkYytzZC1qd3QifQ = {"typ":"dc+sd-jwt"}
        String sdJwt = "eyJ0eXAiOiJkYytzZC1qd3QifQ.payload.signature~disclosure";
        CredentialFormat format = Utils.getCredentialFormat(sdJwt);
        assertEquals(CredentialFormat.DC_SD_JWT, format);
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

    // ── generateID ────────────────────────────────────────────────────────────

    @Test
    void generateID_shouldReturnStringWithPrefix() {
        String result = Utils.generateID("test");
        assertTrue(result.startsWith("test_"));
        assertTrue(result.length() > 5);
    }

    // ── isSdJwt ───────────────────────────────────────────────────────────────

    @Test
    void isSdJwt_shouldReturnTrue_forDcSdJwt() {
        // header = {"alg":"ES256","typ":"dc+sd-jwt"}
        String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        assertTrue(Utils.isSdJwt(token));
    }

    @Test
    void isSdJwt_shouldReturnTrue_forVcSdJwt() {
        // header = {"alg":"ES256","typ":"vc+sd-jwt"}
        String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        assertTrue(Utils.isSdJwt(token));
    }

    @Test
    void isSdJwt_shouldReturnFalse_forMalformedToken() {
        assertFalse(Utils.isSdJwt("not-a-jwt"));
        assertFalse(Utils.isSdJwt("only.two.parts"));
    }

    // ── hasSdJwtCnfClaim ──────────────────────────────────────────────────────

    @Test
    void hasSdJwtCnfClaim_shouldReturnTrue_whenCnfIsNonEmptyObject() {
        // payload = {"cnf":{"kid":"k1"}} = eyJjbmYiOnsia2lkIjoiazEifX0
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOnsia2lkIjoiazEifX0.sig~";
        assertTrue(Utils.hasSdJwtCnfClaim(sdJwt));
    }

    @Test
    void hasSdJwtCnfClaim_shouldReturnFalse_whenCnfIsEmptyObject() {
        // payload = {"cnf":{}} = eyJjbmYiOnt9fQ
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOnt9fQ.sig~";
        assertFalse(Utils.hasSdJwtCnfClaim(sdJwt));
    }

    @Test
    void hasSdJwtCnfClaim_shouldReturnTrue_whenCnfIsNonBlankString() {
        // payload = {"cnf":"thumbprint"} = eyJjbmYiOiJ0aHVtYnByaW50In0
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOiJ0aHVtYnByaW50In0.sig~";
        assertTrue(Utils.hasSdJwtCnfClaim(sdJwt));
    }

    @Test
    void hasSdJwtCnfClaim_shouldReturnFalse_whenCnfIsBlankString() {
        // payload = {"cnf":""} = eyJjbmYiOiIifQ
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOiIifQ.sig~";
        assertFalse(Utils.hasSdJwtCnfClaim(sdJwt));
    }

    @Test
    void hasSdJwtCnfClaim_shouldReturnFalse_whenCnfAbsent() {
        // payload = {"sub":"user123"} = eyJzdWIiOiJ1c2VyMTIzIn0
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        assertFalse(Utils.hasSdJwtCnfClaim(sdJwt));
    }

    @Test
    void hasSdJwtCnfClaim_shouldReturnFalse_whenMalformed() {
        assertFalse(Utils.hasSdJwtCnfClaim("not.a.valid.jwt"));
    }

    // ── hasSdJwtKeyBinding ────────────────────────────────────────────────────

    @Test
    void hasSdJwtKeyBinding_shouldReturnTrue_whenKbJwtPresent() {
        String sdJwt = "header.payload.sig~disclosure~kb-header.kb-payload.kb-sig";
        assertTrue(Utils.hasSdJwtKeyBinding(sdJwt));
    }

    @Test
    void hasSdJwtKeyBinding_shouldReturnFalse_whenNoKbJwt() {
        String sdJwt = "header.payload.sig~disclosure~";
        assertFalse(Utils.hasSdJwtKeyBinding(sdJwt));
    }

    // ── extractSdJwtVct ───────────────────────────────────────────────────────

    @Test
    void extractSdJwtVct_shouldReturnVct_whenPresent() {
        // payload = {"vct": "https://example.com/MyCredential"} = eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9NeUNyZWRlbnRpYWwifQ
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
                ".eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9NeUNyZWRlbnRpYWwifQ.sig~";
        assertEquals("https://example.com/MyCredential", Utils.extractSdJwtVct(sdJwt));
    }

    @Test
    void extractSdJwtVct_shouldReturnNull_whenAbsent() {
        // payload = {"sub":"user123"}
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        assertNull(Utils.extractSdJwtVct(sdJwt));
    }

    @Test
    void extractSdJwtVct_shouldReturnNull_whenMalformed() {
        assertNull(Utils.extractSdJwtVct("not-valid"));
    }

    // ── extractKbJwtPayload ───────────────────────────────────────────────────

    @Test
    void extractKbJwtPayload_shouldReturnPayload_whenKbJwtPresent() {
        // KB-JWT payload = {} = e30 (base64url of "{}")
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
                ".eyJjbmYiOnsia2lkIjoiazEifX0.sig~disclosure~kb-header.e30.kb-sig";
        JSONObject result = Utils.extractKbJwtPayload(sdJwt);
        assertNotNull(result);
    }

    @Test
    void extractKbJwtPayload_shouldReturnNull_whenNoKbJwt() {
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
                ".eyJjbmYiOnsia2lkIjoiazEifX0.sig~";
        assertNull(Utils.extractKbJwtPayload(sdJwt));
    }

    @Test
    void extractKbJwtPayload_shouldReturnNull_whenKbJwtPayloadMalformed() {
        // Last segment is not a valid 3-part JWT with decodable payload
        String sdJwt = "header.payload.sig~disclosure~kb-header.!!!not-base64!!!.kb-sig";
        assertNull(Utils.extractKbJwtPayload(sdJwt));
    }

    // ── ldpTypeMatches ────────────────────────────────────────────────────────

    @Test
    void ldpTypeMatches_shouldReturnTrue_whenExactMatch() {
        assertTrue(Utils.ldpTypeMatches("VerifiableCredential", "VerifiableCredential"));
    }

    @Test
    void ldpTypeMatches_shouldReturnTrue_whenHashMatch() {
        assertTrue(Utils.ldpTypeMatches("VerifiableCredential",
                "https://www.w3.org/2018/credentials#VerifiableCredential"));
    }

    @Test
    void ldpTypeMatches_shouldReturnTrue_whenSlashMatch() {
        assertTrue(Utils.ldpTypeMatches("DriversLicense",
                "https://example.org/types/DriversLicense"));
    }

    @Test
    void ldpTypeMatches_shouldReturnFalse_whenNoMatch() {
        assertFalse(Utils.ldpTypeMatches("SomeType",
                "https://example.org/types/OtherType"));
    }

    // ── extractLdpTypes ───────────────────────────────────────────────────────

    @Test
    void extractLdpTypes_shouldReturnSingleType_whenTextual() throws Exception {
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"VerifiableCredential\"}");
        Set<String> types = Utils.extractLdpTypes(node);
        assertEquals(Set.of("VerifiableCredential"), types);
    }

    @Test
    void extractLdpTypes_shouldReturnMultipleTypes_whenArray() throws Exception {
        JsonNode node = new ObjectMapper().readTree(
                "{\"type\":[\"VerifiableCredential\",\"UniversityDegreeCredential\"]}");
        Set<String> types = Utils.extractLdpTypes(node);
        assertEquals(Set.of("VerifiableCredential", "UniversityDegreeCredential"), types);
    }

    @Test
    void extractLdpTypes_shouldReturnEmpty_whenNoTypeField() throws Exception {
        JsonNode node = new ObjectMapper().readTree("{\"id\":\"123\"}");
        assertTrue(Utils.extractLdpTypes(node).isEmpty());
    }

    // ── isLdpFormat ───────────────────────────────────────────────────────────

    @Test
    void isLdpFormat_shouldReturnTrue_whenTypeInArray() throws Exception {
        JsonNode node = new ObjectMapper().readTree(
                "{\"type\":[\"VerifiablePresentation\",\"UniversityDegree\"]}");
        assertTrue(Utils.isLdpFormat(node, "VerifiablePresentation"));
    }

    @Test
    void isLdpFormat_shouldReturnFalse_whenTypeNotInArray() throws Exception {
        JsonNode node = new ObjectMapper().readTree("{\"type\":[\"UniversityDegree\"]}");
        assertFalse(Utils.isLdpFormat(node, "VerifiablePresentation"));
    }

    @Test
    void isLdpFormat_shouldReturnTrue_whenTextualTypeMatches() throws Exception {
        JsonNode node = new ObjectMapper().readTree("{\"type\":\"VerifiablePresentation\"}");
        assertTrue(Utils.isLdpFormat(node, "verifiablepresentation")); // case-insensitive
    }

    @Test
    void isLdpFormat_shouldReturnFalse_whenNoTypeField() throws Exception {
        JsonNode node = new ObjectMapper().readTree("{\"id\":\"123\"}");
        assertFalse(Utils.isLdpFormat(node, "VerifiablePresentation"));
    }

    // ── checkIfVCIsRevoked ────────────────────────────────────────────────────

    @Test
    void checkIfVCIsRevoked_shouldReturnFalse_whenEmptyMap() throws Exception {
        assertFalse(Utils.checkIfVCIsRevoked(Map.of()));
    }

    @Test
    void checkIfVCIsRevoked_shouldReturnFalse_whenStatusResultIsNull() throws Exception {
        Map<String, CredentialStatusResult> map = new HashMap<>();
        map.put(Constants.STATUS_PURPOSE_REVOKED, null);
        assertFalse(Utils.checkIfVCIsRevoked(map));
    }

    @Test
    void checkIfVCIsRevoked_shouldReturnFalse_whenStatusIsValid() throws Exception {
        CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
        when(statusResult.isValid()).thenReturn(true);
        when(statusResult.getError()).thenReturn(null);
        assertFalse(Utils.checkIfVCIsRevoked(Map.of(Constants.STATUS_PURPOSE_REVOKED, statusResult)));
    }

    @Test
    void checkIfVCIsRevoked_shouldReturnTrue_whenStatusIsInvalid() throws Exception {
        CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
        when(statusResult.isValid()).thenReturn(false);
        when(statusResult.getError()).thenReturn(null);
        assertTrue(Utils.checkIfVCIsRevoked(Map.of(Constants.STATUS_PURPOSE_REVOKED, statusResult)));
    }

    @Test
    void checkIfVCIsRevoked_shouldThrow_whenErrorPresent() {
        CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
        StatusCheckException mockError = mock(StatusCheckException.class);
        when(statusResult.getError()).thenReturn(mockError);
        when(mockError.getErrorCode()).thenReturn(StatusCheckErrorCode.UNKNOWN_ERROR);
        when(mockError.getErrorMessage()).thenReturn("check failed");
        assertThrows(CredentialStatusCheckException.class,
                () -> Utils.checkIfVCIsRevoked(Map.of(Constants.STATUS_PURPOSE_REVOKED, statusResult)));
    }

    // ── applyRevocationStatus ─────────────────────────────────────────────────

    @Test
    void applyRevocationStatus_shouldReturnRevoked_whenRevoked() throws Exception {
        CredentialStatusResult statusResult = mock(CredentialStatusResult.class);
        when(statusResult.isValid()).thenReturn(false);
        when(statusResult.getError()).thenReturn(null);
        VerificationStatus result = Utils.applyRevocationStatus(
                VerificationStatus.SUCCESS,
                Map.of(Constants.STATUS_PURPOSE_REVOKED, statusResult));
        assertEquals(VerificationStatus.REVOKED, result);
    }

    @Test
    void applyRevocationStatus_shouldReturnOriginalStatus_whenNotRevoked() throws Exception {
        assertEquals(VerificationStatus.SUCCESS,
                Utils.applyRevocationStatus(VerificationStatus.SUCCESS, Map.of()));
    }

    // ── getResponseEntityForCredentialStatusException ─────────────────────────

    @Test
    void getResponseEntityForCredentialStatusException_shouldReturn500() {
        CredentialStatusCheckException ex = mock(CredentialStatusCheckException.class);
        when(ex.getErrorCode()).thenReturn(StatusCheckErrorCode.UNKNOWN_ERROR);
        when(ex.getErrorDescription()).thenReturn("some error");
        ResponseEntity<Object> response = Utils.getResponseEntityForCredentialStatusException(ex);
        assertEquals(500, response.getStatusCode().value());
    }

    // ── populateSchemaAndSignature ────────────────────────────────────────────

    @Test
    void populateSchemaAndSignature_shouldReturnValid_whenVerificationPasses() {
        VerificationResult vr = new VerificationResult(true, "", "");
        SchemaAndSignatureCheckDto dto = Utils.populateSchemaAndSignature(vr);
        assertTrue(dto.isValid());
        assertNull(dto.getError());
    }

    @Test
    void populateSchemaAndSignature_shouldReturnInvalid_whenVerificationFails() {
        VerificationResult vr = new VerificationResult(false, "Schema error", "SCHEMA_INVALID");
        SchemaAndSignatureCheckDto dto = Utils.populateSchemaAndSignature(vr);
        assertFalse(dto.isValid());
        assertNotNull(dto.getError());
        assertEquals("Schema error", dto.getError().getErrorMessage());
    }

    // ── populateExpiryCheck ───────────────────────────────────────────────────

    @Test
    void populateExpiryCheck_shouldReturnValid_whenNotExpired() {
        VerificationResult vr = new VerificationResult(true, "", "");
        ExpiryCheckDto dto = Utils.populateExpiryCheck(vr);
        assertTrue(dto.isValid()); // SUCCESS != EXPIRED
    }

    // ── populateAllChecksSuccessful ───────────────────────────────────────────

    @Test
    void populateAllChecksSuccessful_shouldReturnFalse_whenSchemaNull() {
        assertFalse(Utils.populateAllChecksSuccessful(null, null, null, null));
    }

    @Test
    void populateAllChecksSuccessful_shouldReturnFalse_whenSchemaInvalid() {
        SchemaAndSignatureCheckDto schema = new SchemaAndSignatureCheckDto(false,
                new ErrorDto("ERR", "err"));
        assertFalse(Utils.populateAllChecksSuccessful(schema, null, null, null));
    }

    @Test
    void populateAllChecksSuccessful_shouldReturnFalse_whenExpiryInvalid() {
        SchemaAndSignatureCheckDto schema = new SchemaAndSignatureCheckDto(true, null);
        ExpiryCheckDto expiry = new ExpiryCheckDto(false);
        assertFalse(Utils.populateAllChecksSuccessful(schema, expiry, null, null));
    }

    @Test
    void populateAllChecksSuccessful_shouldReturnFalse_whenStatusInvalid() {
        SchemaAndSignatureCheckDto schema = new SchemaAndSignatureCheckDto(true, null);
        StatusCheckDto status = new StatusCheckDto("purpose", false, null);
        assertFalse(Utils.populateAllChecksSuccessful(schema, null, List.of(status), null));
    }

    @Test
    void populateAllChecksSuccessful_shouldReturnFalse_whenHolderProofInvalid() {
        SchemaAndSignatureCheckDto schema = new SchemaAndSignatureCheckDto(true, null);
        HolderProofCheckDto holder = new HolderProofCheckDto(false, null);
        assertFalse(Utils.populateAllChecksSuccessful(schema, null, null, holder));
    }

    @Test
    void populateAllChecksSuccessful_shouldReturnTrue_whenAllValid() {
        SchemaAndSignatureCheckDto schema = new SchemaAndSignatureCheckDto(true, null);
        ExpiryCheckDto expiry = new ExpiryCheckDto(true);
        StatusCheckDto status = new StatusCheckDto("purpose", true, null);
        HolderProofCheckDto holder = new HolderProofCheckDto(true, null);
        assertTrue(Utils.populateAllChecksSuccessful(schema, expiry, List.of(status), holder));
    }

    // ── populateStatusCheckDtoList (with non-null error) ─────────────────────

    @Test
    void populateStatusCheckDtoList_shouldPopulateError_whenCredentialStatusHasError() {
        CredentialStatusResult mockResult = mock(CredentialStatusResult.class);
        StatusCheckException mockError = mock(StatusCheckException.class);
        when(mockResult.isValid()).thenReturn(false);
        when(mockResult.getError()).thenReturn(mockError);
        when(mockError.getErrorCode()).thenReturn(StatusCheckErrorCode.UNKNOWN_ERROR);
        when(mockError.getErrorMessage()).thenReturn("Status check failed");

        List<StatusCheckDto> result = Utils.populateStatusCheckDtoList(Map.of("revocation", mockResult));

        assertEquals(1, result.size());
        assertFalse(result.get(0).isValid());
        assertNotNull(result.get(0).getError());
        assertEquals("UNKNOWN_ERROR", result.get(0).getError().getErrorCode());
    }

    // ── extractClaims SD-JWT branches ────────────────────────────────────────

    @Test
    void extractClaims_shouldHandleDcSdJwtBranch() {
        // header=dc+sd-jwt, payload={"sub":"user123"}, no disclosures
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
                ".eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        Map<String, Object> result = Utils.extractClaims(sdJwt, CredentialFormat.DC_SD_JWT, null, null);
        assertNotNull(result);
        assertTrue(result.containsKey("sub"));
    }

    @Test
    void extractClaims_shouldHandleVcSdJwtBranch() {
        // header=vc+sd-jwt, same payload
        String sdJwt = "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9" +
                ".eyJzdWIiOiJ1c2VyMTIzIn0.sig~";
        Map<String, Object> result = Utils.extractClaims(sdJwt, CredentialFormat.VC_SD_JWT, null, null);
        assertNotNull(result);
        assertTrue(result.containsKey("sub"));
    }
}
