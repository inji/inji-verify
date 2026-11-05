package io.inji.verify.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upokecenter.cbor.CBORObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.inji.verify.dto.core.CredentialStatusErrorDto;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import java.util.HexFormat;
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

    // mdoc tests below use the fixed samples in docs/postman_collection/mdl-samples (see its README).
    // Tests further down that build CBOR by hand cover decoder edge cases, not credentials.

    private static final String MDL_NAMESPACE = "org.iso.18013.5.1";

    @Test
    void shouldDetectFixedMdlSampleAsMsoMdocBeforeCwt() throws Exception {
        String mdl = mdlSample("mdl-valid.txt");

        assertTrue(Utils.isMsoMdoc(mdl));
        assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(mdl));
        assertFalse(Utils.isMsoMdoc("{\"type\":[\"VerifiableCredential\"]}"));
        assertFalse(Utils.isMsoMdoc("eyJ0eXAiOiJkYytzZC1qd3QifQ.payload.signature"));

        byte[] decoded = Base64.getUrlDecoder().decode(mdl);
        String withTrailingBytes = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Arrays.copyOf(decoded, decoded.length + 8));
        assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(withTrailingBytes));
    }

    @Test
    void shouldIdentifyCorruptMdocAndReportMdocDecodeError() throws Exception {
        String corruptMdoc = mdlSample("mdl-corrupt-cbor.txt");

        assertTrue(Utils.isMsoMdoc(corruptMdoc));
        InvalidCredentialException error = assertThrows(
                InvalidCredentialException.class, () -> Utils.getCredentialFormat(corruptMdoc));
        assertEquals("Failed to decode mdoc credential", error.getMessage());
    }

    @Test
    void shouldExtractMandatoryMdlElementsFromFixedSample() throws Exception {
        Map<String, Object> claims = Utils.extractMdocClaims(mdlSample("mdl-valid.txt"), List.of());

        assertEquals(Set.of(MDL_NAMESPACE), claims.keySet());
        Map<?, ?> mdl = (Map<?, ?>) claims.get(MDL_NAMESPACE);
        assertEquals(Set.of("family_name", "given_name", "birth_date", "issue_date", "expiry_date",
                "issuing_country", "issuing_authority", "document_number", "portrait",
                "driving_privileges", "un_distinguishing_sign"), mdl.keySet());
        assertEquals("Doe", mdl.get("family_name"));
        assertEquals("Jane", mdl.get("given_name"));
        // full-date (tag 1004) is kept as its text.
        assertEquals("1990-01-15", mdl.get("birth_date"));
        assertEquals("2026-11-05", mdl.get("issue_date"));
        assertEquals("2031-11-05", mdl.get("expiry_date"));
        assertEquals("IN", mdl.get("issuing_country"));
        assertEquals("Inji Verify Test Authority", mdl.get("issuing_authority"));
        assertEquals("TEST-DL-0000001", mdl.get("document_number"));
        assertEquals("IND", mdl.get("un_distinguishing_sign"));
        assertEquals(List.of(Map.of("vehicle_category_code", "B", "issue_date", "2026-11-05", "expiry_date", "2031-11-05")),
                mdl.get("driving_privileges"));
        // portrait is a JPEG, returned as standard Base64.
        byte[] portrait = Base64.getDecoder().decode((String) mdl.get("portrait"));
        assertArrayEquals(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}, Arrays.copyOf(portrait, 3));
        assertFalse(claims.containsKey("docType"));
    }

    @Test
    void shouldExtractSameClaimsFromAllThreeMdocShapes() throws Exception {
        Map<String, Object> shapeA = Utils.extractMdocClaims(mdlSample("mdl-valid.txt"), List.of());

        assertEquals(shapeA, Utils.extractMdocClaims(mdlSample("mdl-shape-b.txt"), List.of()));
        assertEquals(shapeA, Utils.extractMdocClaims(mdlSample("mdl-shape-c.txt"), List.of()));
    }

    @Test
    void shouldDetectAllThreeMdocShapesAlongsideOtherFormats() throws Exception {
        for (String sample : List.of("mdl-valid.txt", "mdl-shape-b.txt", "mdl-shape-c.txt")) {
            String mdl = mdlSample(sample);
            assertTrue(Utils.isMsoMdoc(mdl), sample);
            assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(mdl), sample);
        }
        assertEquals(CredentialFormat.DC_SD_JWT,
                Utils.getCredentialFormat("eyJ0eXAiOiJkYytzZC1qd3QifQ.payload.signature~disclosure"));
        assertEquals(CredentialFormat.LDP_VC, Utils.getCredentialFormat("{\"type\":[\"VerifiableCredential\"]}"));
    }

    @Test
    void shouldPreferDocumentClaimsWhenMdocShapesAreMixed() throws Exception {
        // Shape C (family_name "Doe") with the tampered shape A fields (family_name "Smith") added alongside.
        CBORObject root = mdlSampleCbor("mdl-shape-c.txt");
        CBORObject tampered = mdlSampleCbor("mdl-tampered-element.txt");
        root.Add("issuerAuth", tampered.get("issuerAuth"));
        root.Add("nameSpaces", tampered.get("nameSpaces"));

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(root), List.of());

        assertEquals("Doe", ((Map<?, ?>) claims.get(MDL_NAMESPACE)).get("family_name"));
    }

    @Test
    void shouldRejectMalformedMdocClaims() {
        assertThrows(InvalidCredentialException.class, () -> Utils.extractMdocClaims("%%%", List.of()));
    }

    @Test
    void shouldRejectOversizedNonMdocInput() {
        String oversized = "a".repeat(1_398_104);

        assertFalse(Utils.isMsoMdoc(oversized));
        InvalidCredentialException error = assertThrows(InvalidCredentialException.class, () -> Utils.extractMdocClaims(oversized, List.of()));
        assertEquals("Credential exceeds the maximum supported size", error.getMessage());
    }

    @Test
    void shouldDetectOversizedMdocThenRefuseIt() throws Exception {
        String oversized = mdlSample("mdl-oversized.txt");

        assertTrue(Utils.isMsoMdoc(oversized));
        InvalidCredentialException formatError = assertThrows(InvalidCredentialException.class,
                () -> Utils.getCredentialFormat(oversized));
        assertEquals("Credential exceeds the maximum supported size", formatError.getMessage());
        InvalidCredentialException claimsError = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(oversized, List.of()));
        assertEquals("Credential exceeds the maximum supported size", claimsError.getMessage());
    }

    @Test
    void shouldRejectEmptyMdocDocuments() throws Exception {
        String emptyDocuments = mdlSample("mdl-empty-documents.txt");

        assertTrue(Utils.isMsoMdoc(emptyDocuments));
        InvalidCredentialException formatError = assertThrows(InvalidCredentialException.class,
                () -> Utils.getCredentialFormat(emptyDocuments));
        assertEquals("mdoc contains no documents", formatError.getMessage());
        InvalidCredentialException claimsError = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(emptyDocuments, List.of()));
        assertEquals("mdoc contains no documents", claimsError.getMessage());
    }

    @Test
    void shouldRemoveConfiguredMdocMetaElementsFromThatNamespaceOnly() throws Exception {
        Map<String, Object> claims = Utils.extractMdocClaims(mdlSample("mdl-valid.txt"), Arrays.asList(
                "org.iso.18013.5.1.portrait",
                // Same element in a namespace the sample does not have: nothing else is removed.
                "org.iso.18013.5.1.aamva.family_name",
                "", null));

        Map<?, ?> mdl = (Map<?, ?>) claims.get(MDL_NAMESPACE);
        assertFalse(mdl.containsKey("portrait"));
        assertEquals("Doe", mdl.get("family_name"));
        assertEquals(10, mdl.size());
    }

    @Test
    void shouldGroupMdocClaimsByNamespaceWhenIdentifiersCollide() {
        CBORObject first = CBORObject.NewMap()
                .Add("elementIdentifier", "family_name")
                .Add("elementValue", "First");
        CBORObject second = CBORObject.NewMap()
                .Add("elementIdentifier", "family_name")
                .Add("elementValue", "Second");
        CBORObject docTypeItem = CBORObject.NewMap()
                .Add("elementIdentifier", "docType")
                .Add("elementValue", "element-doc-type");
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray().Add(CBORObject.FromObject(first.EncodeToBytes())))
                .Add("org.iso.18013.5.1.custom", CBORObject.NewArray()
                        .Add(CBORObject.FromObject(second.EncodeToBytes()))
                        .Add(CBORObject.FromObject(docTypeItem.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("docType", "org.iso.18013.5.1.mDL")
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(document), List.of());

        Map<?, ?> firstNamespace = (Map<?, ?>) claims.get("org.iso.18013.5.1");
        Map<?, ?> secondNamespace = (Map<?, ?>) claims.get("org.iso.18013.5.1.custom");
        assertEquals("First", firstNamespace.get("family_name"));
        assertEquals("Second", secondNamespace.get("family_name"));
        assertEquals("element-doc-type", secondNamespace.get("docType"));
        assertFalse(firstNamespace.containsKey("org.iso.18013.5.1.custom.family_name"));
    }

    @Test
    void shouldPreserveLargeMdocIntegersAsStrings() {
        CBORObject item = CBORObject.NewMap()
                .Add("elementIdentifier", "large_number")
                .Add("elementValue", CBORObject.FromObject(new java.math.BigInteger("18446744073709551615")));
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray().Add(CBORObject.FromObject(item.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("docType", "org.iso.18013.5.1.mDL")
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(document), List.of());

        assertEquals("18446744073709551615",
                ((Map<?, ?>) claims.get("org.iso.18013.5.1")).get("large_number"));
    }

    @Test
    void shouldConvertMdocDecimalFractionToNumericValue() {
        CBORObject decimalFraction = CBORObject.NewArray()
                .Add(-1)
                .Add(15)
                .WithTag(4);
        CBORObject item = CBORObject.NewMap()
                .Add("elementIdentifier", "decimal_claim")
                .Add("elementValue", decimalFraction);
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray().Add(CBORObject.FromObject(item.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("docType", "org.iso.18013.5.1.mDL")
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(document), List.of());

        assertEquals(new BigDecimal("1.5"),
                ((Map<?, ?>) claims.get("org.iso.18013.5.1")).get("decimal_claim"));
    }

    @Test
    void shouldExcludeMdocMetaClaimWithDottedElementIdentifier() {
        CBORObject dotted = CBORObject.NewMap()
                .Add("elementIdentifier", "age.over18")
                .Add("elementValue", true);
        CBORObject plain = CBORObject.NewMap()
                .Add("elementIdentifier", "family_name")
                .Add("elementValue", "Doe");
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray()
                        .Add(CBORObject.FromObject(dotted.EncodeToBytes()))
                        .Add(CBORObject.FromObject(plain.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("docType", "org.iso.18013.5.1.mDL")
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(document),
                List.of("org.iso.18013.5.1.age.over18"));

        Map<?, ?> namespaceClaims = (Map<?, ?>) claims.get("org.iso.18013.5.1");
        assertFalse(namespaceClaims.containsKey("age.over18"));
        assertEquals("Doe", namespaceClaims.get("family_name"));
    }

    @Test
    void shouldExcludeEveryMdocMetaClaimSplitThatMatches() {
        // "org.iso.18013.5.1.age" names both org.iso / 18013.5.1.age and org.iso.18013.5.1 / age.
        CBORObject shortNamespaceItem = CBORObject.NewMap().Add("elementIdentifier", "18013.5.1.age").Add("elementValue", 1);
        CBORObject shortNamespaceOther = CBORObject.NewMap().Add("elementIdentifier", "kept").Add("elementValue", 2);
        CBORObject longNamespaceItem = CBORObject.NewMap().Add("elementIdentifier", "age").Add("elementValue", 3);
        CBORObject longNamespaceOther = CBORObject.NewMap().Add("elementIdentifier", "family_name").Add("elementValue", "Doe");
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso", CBORObject.NewArray()
                        .Add(CBORObject.FromObject(shortNamespaceItem.EncodeToBytes()))
                        .Add(CBORObject.FromObject(shortNamespaceOther.EncodeToBytes())))
                .Add("org.iso.18013.5.1", CBORObject.NewArray()
                        .Add(CBORObject.FromObject(longNamespaceItem.EncodeToBytes()))
                        .Add(CBORObject.FromObject(longNamespaceOther.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        Map<String, Object> claims = Utils.extractMdocClaims(encodeMdoc(document), List.of("org.iso.18013.5.1.age"));

        assertEquals(Map.of("kept", 2L), claims.get("org.iso"));
        assertEquals(Map.of("family_name", "Doe"), claims.get("org.iso.18013.5.1"));
    }

    @Test
    void shouldRejectCollidingKeysInNestedMdocMap() {
        // Integer key 1 and text key "1" both render as "1" inside an element value.
        CBORObject value = CBORObject.NewMap()
                .Add(CBORObject.FromObject(1), "first")
                .Add(CBORObject.FromObject("1"), "second");

        InvalidCredentialException error = assertThrows(InvalidCredentialException.class,
                () -> mdocElementValue(mdocWithElementValue(value)));
        assertEquals("Failed to extract mdoc claims", error.getMessage());
    }

    @Test
    void shouldRejectDuplicateMdocElementIdentifierInNamespace() {
        CBORObject first = CBORObject.NewMap().Add("elementIdentifier", "family_name").Add("elementValue", "Doe");
        CBORObject second = CBORObject.NewMap().Add("elementIdentifier", "family_name").Add("elementValue", "Roe");
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray()
                        .Add(CBORObject.FromObject(first.EncodeToBytes()))
                        .Add(CBORObject.FromObject(second.EncodeToBytes())));
        CBORObject document = CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces);

        InvalidCredentialException error = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(encodeMdoc(document), List.of()));
        assertEquals("Duplicate mdoc element identifier", error.getMessage());
    }

    private static String mdocWithElementValue(CBORObject elementValue) {
        CBORObject item = CBORObject.NewMap()
                .Add("elementIdentifier", "value")
                .Add("elementValue", elementValue);
        CBORObject namespaces = CBORObject.NewMap()
                .Add("org.iso.18013.5.1", CBORObject.NewArray().Add(CBORObject.FromObject(item.EncodeToBytes())));
        return encodeMdoc(CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", namespaces));
    }

    private static Object mdocElementValue(String mdoc) {
        return ((Map<?, ?>) Utils.extractMdocClaims(mdoc, List.of()).get("org.iso.18013.5.1")).get("value");
    }

    @Test
    void shouldKeepTdateAsSentAndRenderEpochDatesAsUtcInstants() {
        assertEquals("2024-05-03T08:00:00Z",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject(1714723200L).WithTag(1))));
        assertEquals("2024-05-03T08:00:00.500Z",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject(1714723200.5).WithTag(1))));
        // A tdate is not converted to UTC: that would give 2024-05-04T04:30:00Z, a different date.
        assertEquals("2024-05-03T23:30:00-05:00",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject("2024-05-03T23:30:00-05:00").WithTag(0))));
        assertEquals("not-a-date",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject("not-a-date").WithTag(0))));
        assertEquals("1985-04-29",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject("1985-04-29").WithTag(1004))));
    }

    @Test
    void shouldReturnNullForCborNullAndUndefined() {
        for (CBORObject value : List.of(CBORObject.Null, CBORObject.Undefined)) {
            Map<?, ?> claims = (Map<?, ?>) Utils.extractMdocClaims(mdocWithElementValue(value), List.of()).get(MDL_NAMESPACE);
            assertTrue(claims.containsKey("value"), value.toString());
            assertNull(claims.get("value"), value.toString());
        }
    }

    @Test
    void shouldKeepSubMillisecondDigitsOfFloatingPointMdocEpochs() {
        // Previously rounded to whole milliseconds (.123Z); the raw double fraction would give .123456001Z.
        assertEquals("2024-05-03T08:00:00.123456Z",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject(1714723200.123456).WithTag(1))));
        assertEquals("1969-12-31T23:59:58.500Z",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject(-1.5).WithTag(1))));
        assertEquals("NaN",
                mdocElementValue(mdocWithElementValue(CBORObject.FromObject(Double.NaN).WithTag(1))));
    }

    @Test
    void shouldKeepNonIntegralMdocNumbersExact() {
        // Plain floats are FloatingPoint, never truncated to an integer.
        assertEquals(1.5, mdocElementValue(mdocWithElementValue(CBORObject.FromObject(1.5))));
        assertEquals(-0.25, mdocElementValue(mdocWithElementValue(CBORObject.FromObject(-0.25))));
    }

    @Test
    void shouldRejectMalformedMdocNamespacesAndItems() {
        String mapNamespace = encodeMdoc(CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", CBORObject.NewMap().Add("org.example", CBORObject.NewMap())));
        InvalidCredentialException namespaceError = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(mapNamespace, List.of()));
        assertEquals("Invalid mdoc namespace: expected an item array", namespaceError.getMessage());

        CBORObject noValue = CBORObject.NewMap().Add("elementIdentifier", "family_name");
        String missingValue = encodeMdoc(CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", CBORObject.NewMap().Add("org.iso.18013.5.1",
                        CBORObject.NewArray().Add(CBORObject.FromObject(noValue.EncodeToBytes())))));
        InvalidCredentialException itemError = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(missingValue, List.of()));
        assertEquals("Invalid mdoc issuer-signed item", itemError.getMessage());

        // Integer key 1 and text key "1" both render as the namespace name "1".
        String collidingNamespaces = encodeMdoc(CBORObject.NewMap()
                .Add("issuerAuth", CBORObject.NewArray())
                .Add("nameSpaces", CBORObject.NewMap()
                        .Add(CBORObject.FromObject(1), CBORObject.NewArray())
                        .Add(CBORObject.FromObject("1"), CBORObject.NewArray())));
        InvalidCredentialException duplicateError = assertThrows(InvalidCredentialException.class,
                () -> Utils.extractMdocClaims(collidingNamespaces, List.of()));
        assertEquals("Duplicate mdoc namespace", duplicateError.getMessage());
    }

    @Test
    void shouldDetectOversizedMdocWhoseKeysUseLongerLengthHeaders() {
        // {"issuerAuth": [], "nameSpaces": {}} with each key's length in every non-shortest form.
        byte[][] lengthHeaders = {
                {0x78, 10},
                {0x79, 0, 10},
                {0x7a, 0, 0, 0, 10},
                {0x7b, 0, 0, 0, 0, 0, 0, 0, 10},
        };
        for (byte[] header : lengthHeaders) {
            java.io.ByteArrayOutputStream cbor = new java.io.ByteArrayOutputStream();
            cbor.write(0xa2);
            cbor.writeBytes(header);
            cbor.writeBytes("issuerAuth".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            cbor.write(0x80);
            cbor.writeBytes(header);
            cbor.writeBytes("nameSpaces".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            cbor.write(0xa0);
            byte[] bytes = cbor.toByteArray();
            String mdoc = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            try {
                new Utils().setMaxMdocSizeBytes(bytes.length - 1);
                InvalidCredentialException error = assertThrows(InvalidCredentialException.class,
                        () -> Utils.getCredentialFormat(mdoc), "header 0x" + Integer.toHexString(header[0] & 0xff));
                assertEquals("Credential exceeds the maximum supported size", error.getMessage());
            } finally {
                new Utils().setMaxMdocSizeBytes(1_048_576);
            }
        }
    }

    @Test
    void shouldApplyMdocSizeLimitToDecodedBytes() throws Exception {
        String mdl = mdlSample("mdl-valid.txt");
        int decodedBytes = Base64.getUrlDecoder().decode(mdl).length;
        try {
            new Utils().setMaxMdocSizeBytes(decodedBytes);
            assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(mdl));
            new Utils().setMaxMdocSizeBytes(decodedBytes - 1);
            InvalidCredentialException error = assertThrows(InvalidCredentialException.class, () -> Utils.getCredentialFormat(mdl));
            assertEquals("Credential exceeds the maximum supported size", error.getMessage());
        } finally {
            new Utils().setMaxMdocSizeBytes(1_048_576);
        }
    }

    @Test
    void shouldRejectNonPositiveMdocSizeLimit() throws Exception {
        Utils utils = new Utils();
        assertThrows(IllegalArgumentException.class, () -> utils.setMaxMdocSizeBytes(0));
        assertThrows(IllegalArgumentException.class, () -> utils.setMaxMdocSizeBytes(-1));
        // The previous limit is kept, not replaced by a default.
        String mdl = mdlSample("mdl-valid.txt");
        assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(mdl));
    }

    @Test
    void shouldReturnEmptyClaimsMapForJwtVcJson() {
        Map<String, Object> claims = Utils.extractClaims("unused", CredentialFormat.JWT_VC_JSON, null, null);

        assertNotNull(claims);
        assertTrue(claims.isEmpty());
    }

    private static String encodeMdoc(CBORObject document) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(document.EncodeToBytes());
    }

    /** A fixed sample from docs/postman_collection/mdl-samples, as the base64url string sent to the API. */
    static String mdlSample(String fileName) throws Exception {
        return Files.readString(repositoryFile("docs/postman_collection/mdl-samples/" + fileName)).trim();
    }

    private static CBORObject mdlSampleCbor(String fileName) throws Exception {
        return CBORObject.DecodeFromBytes(Base64.getUrlDecoder().decode(mdlSample(fileName)));
    }

    private static Path repositoryFile(String relativePath) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        fail("File must be available from the repository root: " + relativePath);
        return null;
    }

    @Test
    void shouldDetectCredentialFormatsFromPostmanAndSignedSdJwtSamples() throws Exception {
        String ldp = postmanCredential("ldp_vc valid");
        String vcSdJwt = postmanCredential("sd-jwt valid");
        String cwt = postmanCredential("cwt valid");
        String mdoc = postmanCredential("mso_mdoc valid");

        assertEquals(CredentialFormat.LDP_VC, Utils.getCredentialFormat(ldp));
        assertEquals(CredentialFormat.VC_SD_JWT, Utils.getCredentialFormat(vcSdJwt));
        assertEquals(CredentialFormat.CWT_VC, Utils.getCredentialFormat(cwt));
        assertEquals(CredentialFormat.MSO_MDOC, Utils.getCredentialFormat(mdoc));

        String dcSdJwt = createSignedDcSdJwt();
        assertEquals(CredentialFormat.DC_SD_JWT, Utils.getCredentialFormat(dcSdJwt));
    }

    @Test
    void shouldReturnSdJwtFormatForValidSdJwtString() {
        // eyJ0eXAiOiJkYytzZC1qd3QifQ = {"typ":"dc+sd-jwt"}
        String sdJwt = "eyJ0eXAiOiJkYytzZC1qd3QifQ.payload.signature~disclosure";
        CredentialFormat format = Utils.getCredentialFormat(sdJwt);
        assertEquals(CredentialFormat.DC_SD_JWT, format);
    }

    private static String postmanCredential(String requestName) throws Exception {
        Path collectionPath = repositoryFile("docs/postman_collection/Inji_Verify_Postman_Collection.json");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode collection = mapper.readTree(collectionPath.toFile());
        JsonNode request = findNamedRequest(collection, requestName);
        assertNotNull(request, "Postman request not found: " + requestName);
        JsonNode body = mapper.readTree(request.path("request").path("body").path("raw").asText());
        return body.path("verifiableCredential").asText();
    }

    private static JsonNode findNamedRequest(JsonNode node, String requestName) {
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode request = findNamedRequest(child, requestName);
                if (request != null) {
                    return request;
                }
            }
        } else if (node.isObject()) {
            if (requestName.equals(node.path("name").asText()) && node.has("request")) {
                return node;
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                JsonNode request = findNamedRequest(fields.next().getValue(), requestName);
                if (request != null) {
                    return request;
                }
            }
        }
        return null;
    }

    private static String createSignedDcSdJwt() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dc+sd-jwt")).build(),
                new JWTClaimsSet.Builder().subject("sample").build());
        signedJwt.sign(new ECDSASigner((ECPrivateKey) keyPair.getPrivate()));
        return signedJwt.serialize() + "~";
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
    void extractCwtClaims_shouldReportClaimExtractionFailure() {
        CBORObject payload = CBORObject.NewMap()
                .Add(169, CBORObject.FromObject(new byte[] {(byte) 0xA0}));
        CBORObject coseArray = CBORObject.NewArray()
                .Add(0)
                .Add(0)
                .Add(payload.EncodeToBytes())
                .Add(new byte[0]);
        String credential = HexFormat.of().formatHex(coseArray.EncodeToBytes());
        PixelPass pixelPass = mock(PixelPass.class);
        when(pixelPass.decodeMappedData(anyString())).thenThrow(new IllegalStateException("mapping failed"));

        InvalidCredentialException error = assertThrows(
                InvalidCredentialException.class,
                () -> Utils.extractCwtClaims(credential, pixelPass, List.of()));

        assertEquals("Failed to extract CWT claims", error.getMessage());
        assertInstanceOf(IllegalStateException.class, error.getCause());
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

    // ── buildCredentialStatusErrorDto ──────────────────────────────────────────

    @Test
    void buildCredentialStatusErrorDto_shouldReturn500() {
        CredentialStatusCheckException ex = mock(CredentialStatusCheckException.class);
        when(ex.getErrorCode()).thenReturn(StatusCheckErrorCode.UNKNOWN_ERROR);
        when(ex.getErrorDescription()).thenReturn("some error");
        CredentialStatusErrorDto errorDto = Utils.buildCredentialStatusErrorDto(ex);
        assertEquals(500, errorDto.getStatus());
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
