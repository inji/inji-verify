package io.inji.verify.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORTaggedItem;
import com.upokecenter.cbor.CBOREncodeOptions;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectDecoder;
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
import io.mosip.vercred.vcverifier.constants.CredentialValidatorConstants;
import io.mosip.vercred.vcverifier.constants.CredentialVerifierConstants;
import io.mosip.vercred.vcverifier.credentialverifier.types.msomdoc.MsoMdocVerifiableCredential;
import io.mosip.vercred.vcverifier.data.CredentialStatusResult;
import io.mosip.vercred.vcverifier.data.CredentialVerificationSummary;
import io.mosip.vercred.vcverifier.data.VerificationResult;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import io.mosip.vercred.vcverifier.exception.StatusCheckException;
import io.mosip.vercred.vcverifier.utils.Base64Decoder;
import io.mosip.vercred.vcverifier.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.inji.verify.shared.Constants.COOKIE_NAME;
import static io.ipfs.multibase.Base16.bytesToHex;

@Slf4j
@Component
public final class Utils {

    private static final Set<String> VALID_SD_JWT_TYPES = Set.of(Constants.FORMAT_DC_SD_JWT, Constants.FORMAT_VC_SD_JWT);

    public static String generateID(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }

    public static boolean isSdJwt(String vpToken) {
        try {
            return VALID_SD_JWT_TYPES.contains(extractSdJwtTyp(vpToken));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the {@code typ} header claim from an SD-JWT (or any JWT-like token).
     * Splits on {@code ~} to get the credential JWT, then decodes the base64url header.
     * Returns an empty string if the token is malformed or the header cannot be decoded.
     */
    private static String extractSdJwtTyp(String token) {
        try {
            String[] jwtParts = token.split("~")[0].split("\\.");
            if (jwtParts.length != 3) {
                return "";
            }
            String header = decodeBase64Json(jwtParts[0]);
            return new JSONObject(header).optString("typ", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns true if the SD-JWT payload contains a non-null, non-empty {@code cnf} claim,
     * indicating the credential supports Holder Binding. Per OpenID4VP §6.4.2: SD-JWTs without a
     * {@code cnf} claim cannot be returned when {@code require_cryptographic_holder_binding} is true.
     * A {@code cnf} value that is null or an empty object does not carry key material and is treated
     * as absent.
     * Returns false if the payload cannot be decoded, the cnf claim is absent, null, or empty.
     */
    public static boolean hasSdJwtCnfClaim(String sdJwt) {
        try {
            String[] jwtParts = sdJwt.split("~")[0].split("\\.");
            String payloadJson = decodeBase64Json(jwtParts[1]);
            JSONObject payload = new JSONObject(payloadJson);
            if (!payload.has("cnf") || payload.isNull("cnf")) {
                return false;
            }
            Object cnf = payload.get("cnf");
            if (cnf instanceof JSONObject) {
                return !((JSONObject) cnf).isEmpty();
            }
            // cnf can also be a string (JWK thumbprint form) — non-empty string is valid
            if (cnf instanceof String) {
                return !((String) cnf).isBlank();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodes and returns the payload of the Key Binding JWT (KB-JWT) from an SD-JWT string.
     * The KB-JWT is the last '~'-delimited segment and must itself be a three-part JWT.
     * Returns null if the KB-JWT is absent, malformed, or its payload cannot be decoded.
     */
    public static JSONObject extractKbJwtPayload(String sdJwt) {
        String[] parts = sdJwt.split("~", -1);
        String kbJwt = parts[parts.length - 1];
        if (kbJwt.isEmpty()) return null;
        String[] jwtParts = kbJwt.split("\\.");
        if (jwtParts.length != 3) return null;
        try {
            String payloadJson = decodeBase64Json(jwtParts[1]);
            return new JSONObject(payloadJson);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks whether an SD-JWT string contains a Key Binding JWT (KB-JWT).
     * Per the IETF SD-JWT spec, the KB-JWT is the last ~-delimited part and is itself a full JWT
     * (three Base64url segments separated by dots). An SD-JWT without a KB-JWT ends with a trailing ~
     * (the last part after splitting on ~ is empty).
     */
    public static boolean hasSdJwtKeyBinding(String sdJwt) {
        String[] parts = sdJwt.split("~", -1); // -1 preserves trailing empty string
        String lastPart = parts[parts.length - 1];
        return !lastPart.isEmpty() && lastPart.split("\\.").length == 3;
    }

    /**
     * Extracts the vct claim directly from the SD-JWT's JWT payload.
     * Per the SD-JWT VC spec, vct is always in the unsecured payload and is never selectively disclosable.
     * Returns null if the payload cannot be decoded or the vct claim is absent.
     */
    public static String extractSdJwtVct(String sdJwt) {
        try {
            String[] jwtParts = sdJwt.split("~")[0].split("\\.");
            String payloadJson = decodeBase64Json(jwtParts[1]);
            return new JSONObject(payloadJson).optString("vct", null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns true if a credential's type value satisfies a required type from type_values.
     * Supports exact match and local-name extraction via two IRI separators:
     *   '#' fragment:  "VerifiableCredential" matches "https://www.w3.org/2018/credentials#VerifiableCredential"
     *   '/' path:      "DriversLicense"       matches "https://example.org/types/DriversLicense"
     * Per DCQL spec: if a type is not defined in any @context it remains a relative IRI,
     * so JSON-LD processing may be skipped and the local name is treated as the expanded type.
     * '#' is tried before '/' because fragment identifiers are the more common IRI pattern in VC contexts.
     */
    public static boolean ldpTypeMatches(String credentialType, String requiredType) {
        if (credentialType.equals(requiredType)) return true;
        int hashIndex = requiredType.lastIndexOf('#');
        if (hashIndex >= 0) {
            return credentialType.equals(requiredType.substring(hashIndex + 1));
        }
        int slashIndex = requiredType.lastIndexOf('/');
        return slashIndex >= 0 && credentialType.equals(requiredType.substring(slashIndex + 1));
    }

    /**
     * Extracts all values from the "type" field of an ldp_vc JSON node into a Set.
     * Supports both forms allowed by the VC Data Model:
     *   - a single string:  "type": "VerifiableCredential"
     *   - an array of strings: "type": ["VerifiableCredential", "UniversityDegreeCredential"]
     * Returns an empty set for null or any other node type.
     */
    public static Set<String> extractLdpTypes(JsonNode item) {
        Set<String> types = new HashSet<>();
        JsonNode typeNode = item.get("type");
        if (typeNode == null) return types;
        if (typeNode.isTextual()) {
            types.add(typeNode.asText());
        } else if (typeNode.isArray()) {
            typeNode.forEach(t -> types.add(t.asText()));
        }
        return types;
    }

    public static boolean isLdpFormat(JsonNode item, String formatType) {
        JsonNode typeNode = item.get("type");
        if (typeNode == null) {
            return false;
        }
        if (typeNode.isArray()) {
            for (JsonNode typeValue : typeNode) {
                if (formatType.equalsIgnoreCase(typeValue.asText())) {
                    return true;
                }
            }
        } else if (typeNode.isTextual()) {
            return formatType.equalsIgnoreCase(typeNode.asText());
        }
        return false;
    }

    public static boolean isCwt(String credential) {

        if (credential.contains(".")) {
            return false;
        }

        if (credential.trim().startsWith("{")) {
            return false;
        }

        try {
            byte[] data = hexToBytes(credential);

            CBORDecoder decoder = new CBORDecoder(data);
            CBORItem item = decoder.next();

            return item instanceof CBORTaggedItem
                    && ((CBORTaggedItem) item).getTagNumber().intValue() == 61;

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex string is null");
        }


        String cleanHex = hex.replaceAll("\\s", "");

        if (cleanHex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex length");
        }

        int len = cleanHex.length();
        byte[] result = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(
                    cleanHex.substring(i, i + 2),
                    16
            );
        }

        return result;
    }


    private static String decodeBase64Json(String encoded)  {
        byte[] decodedBytes = new Base64Decoder().decodeFromBase64Url(encoded);
        return new String(decodedBytes);
    }

    public static VerificationStatus getVcVerificationStatus(CredentialVerificationSummary credentialVerificationSummary) throws CredentialStatusCheckException {
        log.debug("Credential Verification Summary: {}", credentialVerificationSummary);
        VerificationResult verificationResult = credentialVerificationSummary.getVerificationResult();
        VerificationStatus verificationStatus = Util.INSTANCE.getVerificationStatus(verificationResult);
        boolean isRevoked = checkIfVCIsRevoked(credentialVerificationSummary.getCredentialStatus());
        if (isRevoked) return VerificationStatus.REVOKED;

        log.debug("VC verification status is {}", verificationStatus );
        return verificationStatus;
    }

    public static boolean checkIfVCIsRevoked(Map<String, CredentialStatusResult> credentialStatusResults) throws CredentialStatusCheckException {
        if (!credentialStatusResults.isEmpty()) {
            CredentialStatusResult credentialStatusResult = credentialStatusResults.get(Constants.STATUS_PURPOSE_REVOKED);
            if (credentialStatusResult != null) {
                StatusCheckException error = credentialStatusResult.getError();
                boolean isStatusValid = credentialStatusResult.isValid();
                if (error == null) {
                    // VC is Revoked if status is Not Valid
                    return !isStatusValid;
                } else {
                    log.error("Failed to get Credential Status due to: {} {}", error.getErrorCode(), error.getErrorMessage());
                    throw new CredentialStatusCheckException(error.getErrorCode(), error.getErrorMessage());
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public static VerificationStatus applyRevocationStatus(VerificationStatus originalStatus, Map<String, CredentialStatusResult> credentialStatus) throws CredentialStatusCheckException {
        boolean isRevoked = checkIfVCIsRevoked(credentialStatus);
        return isRevoked ? VerificationStatus.REVOKED : originalStatus;
    }

    /**
     * Builds the error body for a {@link CredentialStatusCheckException}. Building the HTTP
     * response itself (status code, ResponseEntity) is left to the caller, since that's an
     * HTTP-transport concern and this is a plain utility class.
     */
    public static CredentialStatusErrorDto buildCredentialStatusErrorDto(CredentialStatusCheckException ex) {
        String errorMessage = ex.getErrorCode() + " - " + ex.getErrorDescription();
        return new CredentialStatusErrorDto(Instant.now().toString(), 500, errorMessage);
    }

    public static List<StatusCheckDto> populateStatusCheckDtoList(Map<String, CredentialStatusResult> credentialStatusResult) {
        if (credentialStatusResult == null) return List.of();

        return credentialStatusResult.entrySet().stream()
                .map(entry -> {
                    String purpose = entry.getKey();
                    CredentialStatusResult res = entry.getValue();
                    if (res == null) {
                        return new StatusCheckDto(purpose, false, new ErrorDto("NULL_STATUS_RESULT", "Credential status result was null."));
                    }
                    ErrorDto error = populateErrorDto(res);
                    return new StatusCheckDto(purpose, res.isValid(), error);
                })
                .collect(Collectors.toList());
    }

    private static ErrorDto populateErrorDto(CredentialStatusResult res) {
        return res.getError() != null
                ? new ErrorDto(res.getError().getErrorCode().toString(), res.getError().getErrorMessage())
                : null;
    }

    /** Clock tolerance the library applies to MSO validity dates. */
    private static final long MDOC_VALIDITY_TOLERANCE_MILLIS = 3000;

    /**
     * Reports the MSO validity window ({@code validFrom} / {@code validUntil}) through
     * {@code expiryCheck}, as for every other format. vc-verifier gets two cases wrong:
     * <ol>
     *   <li><b>Not yet valid.</b> The library does not check {@code validFrom} against the current
     *       time. A verified mdoc whose {@code validFrom} is in the future is rewritten as the
     *       library's "expired" outcome (verified, {@code ERR_VC_EXPIRED}).</li>
     *   <li><b>Expired.</b> The library stops at an expired {@code validUntil} and never checks the
     *       issuer signature, the element digests or the remaining validity rule ({@code validUntil}
     *       after {@code validFrom}). Those checks run here: a signature or digest failure becomes
     *       the library's generic verification failure, an inconsistent window its invalid-date
     *       failure, and otherwise the result becomes the "expired" outcome.</li>
     * </ol>
     * Call it after every {@code CredentialsVerifier} call whose format may be mdoc; it is a no-op
     * otherwise. It belongs in vc-verifier and can be removed once the library handles both cases.
     */
    public static void reportMdocValidityThroughExpiry(String credential, CredentialFormat format, VerificationResult verificationResult) {
        if (format != CredentialFormat.MSO_MDOC || verificationResult == null) {
            return;
        }
        if (verificationResult.getVerificationStatus()) {
            reportNotYetValidMdocAsExpiry(credential, verificationResult);
            return;
        }
        if (!CredentialValidatorConstants.ERROR_CODE_INVALID_VALID_UNTIL_MSO.equals(verificationResult.getVerificationErrorCode())) {
            return;
        }
        boolean signatureAndDigestsValid;
        try {
            signatureAndDigestsValid = new MsoMdocVerifiableCredential().verify(credential);
        } catch (Exception e) {
            // Class name only: the message can carry digest IDs or certificate details.
            log.warn("Expired mdoc failed signature or digest verification: {}", e.getClass().getSimpleName());
            signatureAndDigestsValid = false;
        }
        if (!signatureAndDigestsValid) {
            verificationResult.setVerificationMessage(CredentialVerifierConstants.ERROR_MESSAGE_VERIFICATION_FAILED);
            verificationResult.setVerificationErrorCode(CredentialVerifierConstants.ERROR_CODE_VERIFICATION_FAILED);
            return;
        }
        MdocValidityWindow window = mdocValidityWindow(credential);
        // Fails closed: an unreadable window is treated as inconsistent.
        if (window == null || !window.validUntil().isAfter(window.validFrom())) {
            verificationResult.setVerificationMessage(CredentialValidatorConstants.ERROR_MESSAGE_INVALID_DATE_MSO);
            verificationResult.setVerificationErrorCode(CredentialValidatorConstants.ERROR_CODE_INVALID_DATE_MSO);
            return;
        }
        markMdocExpiryFailed(verificationResult, CredentialValidatorConstants.ERROR_MESSAGE_VC_EXPIRED);
    }

    private static void reportNotYetValidMdocAsExpiry(String credential, VerificationResult verificationResult) {
        if (CredentialValidatorConstants.ERROR_CODE_VC_EXPIRED.equals(verificationResult.getVerificationErrorCode())) {
            return;
        }
        MdocValidityWindow window = mdocValidityWindow(credential);
        if (window == null) {
            // The library already read this window to verify the document, so this is not expected.
            log.warn("Could not read the validity window of a verified mdoc; validFrom was not checked");
            return;
        }
        if (window.validFrom().isAfter(Instant.now().plusMillis(MDOC_VALIDITY_TOLERANCE_MILLIS))) {
            markMdocExpiryFailed(verificationResult, Constants.ERROR_MESSAGE_MDOC_NOT_YET_VALID);
        }
    }

    /** The library's "expired" outcome: signature verified, {@code ERR_VC_EXPIRED}, read by {@code populateExpiryCheck}. */
    private static void markMdocExpiryFailed(VerificationResult verificationResult, String message) {
        verificationResult.setVerificationStatus(true);
        verificationResult.setVerificationMessage(message);
        verificationResult.setVerificationErrorCode(CredentialValidatorConstants.ERROR_CODE_VC_EXPIRED);
    }

    private record MdocValidityWindow(Instant validFrom, Instant validUntil) {
    }

    /**
     * {@code validityInfo} from the MSO of the same document the library verifies, or
     * {@code null} when it cannot be read.
     */
    private static MdocValidityWindow mdocValidityWindow(String credential) {
        try {
            CBORObject issuerAuth = mdocIssuerAuth(readMdocRoot(credential.trim()));
            CBORObject mso = issuerAuth.get(2);
            for (int attempt = 0; attempt < 3 && (mso.HasMostOuterTag(24) || mso.getType() == CBORType.ByteString); attempt++) {
                mso = mso.HasMostOuterTag(24) ? mso.Untag() : CBORObject.DecodeFromBytes(mso.GetByteString());
            }
            CBORObject validityInfo = mso.get("validityInfo");
            return new MdocValidityWindow(
                    OffsetDateTime.parse(validityInfo.get("validFrom").AsString()).toInstant(),
                    OffsetDateTime.parse(validityInfo.get("validUntil").AsString()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }

    private static CBORObject mdocIssuerAuth(CBORObject root) {
        if (root.ContainsKey(Constants.KEY_DOCUMENTS)) {
            return root.get(Constants.KEY_DOCUMENTS).get(0).get(Constants.KEY_ISSUER_SIGNED).get(Constants.KEY_ISSUER_AUTH);
        }
        if (root.ContainsKey(Constants.KEY_ISSUER_SIGNED)) {
            return root.get(Constants.KEY_ISSUER_SIGNED).get(Constants.KEY_ISSUER_AUTH);
        }
        return root.get(Constants.KEY_ISSUER_AUTH);
    }

    public static SchemaAndSignatureCheckDto populateSchemaAndSignature(VerificationResult verificationResult) {
        boolean isValid = verificationResult.getVerificationStatus();
        ErrorDto error = isValid ? null : new ErrorDto(verificationResult.getVerificationErrorCode(), verificationResult.getVerificationMessage());

        return new SchemaAndSignatureCheckDto(isValid, error);
    }

    public static ExpiryCheckDto populateExpiryCheck(VerificationResult verificationResult) {
        VerificationStatus verificationStatus = Util.INSTANCE.getVerificationStatus(verificationResult);
        boolean isValid = verificationStatus != VerificationStatus.EXPIRED;

        return new ExpiryCheckDto(isValid);
    }

    public static boolean populateAllChecksSuccessful(
            SchemaAndSignatureCheckDto schemaAndSignatureCheckDto,
            ExpiryCheckDto expiryCheckDto,
            List<StatusCheckDto> statusCheckDto,
            HolderProofCheckDto holderProofCheckDto) {

        return schemaAndSignatureCheckDto != null
                && schemaAndSignatureCheckDto.isValid()
                && (expiryCheckDto == null || expiryCheckDto.isValid())
                && (statusCheckDto == null
                || statusCheckDto.isEmpty()
                || statusCheckDto.stream().allMatch(c -> c != null && c.isValid()))
                && (holderProofCheckDto == null || holderProofCheckDto.isValid());
    }

    public static Map<String, Object> extractClaims(String verifiableCredential, CredentialFormat format, List<String> metaClaims, PixelPass pixelPass) {
        return extractClaims(verifiableCredential, format, metaClaims, pixelPass, List.of());
    }

    public static Map<String, Object> extractClaims(String verifiableCredential, CredentialFormat format, List<String> metaClaims, PixelPass pixelPass, List<String> mdocMetaClaims) {
        return switch (format) {
            case VC_SD_JWT, DC_SD_JWT -> extractSdJwtClaims(verifiableCredential, metaClaims);
            case LDP_VC -> extractLdpClaims(verifiableCredential);
            case CWT_VC -> extractCwtClaims(verifiableCredential, pixelPass, metaClaims);
            case MSO_MDOC -> extractMdocClaims(verifiableCredential, mdocMetaClaims);
            case JWT_VC_JSON -> Map.of();
        };
    }

    private static final int DEFAULT_MAX_MDOC_SIZE_BYTES = 1_048_576;
    private static volatile int maxMdocSizeBytes = DEFAULT_MAX_MDOC_SIZE_BYTES;
    private static final int MAX_MDOC_DEPTH = 32;

    /** Maximum decoded mdoc size in bytes (not base64 characters). */
    @Value("${inji.verify.mso-mdoc.max-size:1048576}")
    public void setMaxMdocSizeBytes(int maxMdocSizeBytes) {
        Utils.maxMdocSizeBytes = maxMdocSizeBytes > 0 ? maxMdocSizeBytes : DEFAULT_MAX_MDOC_SIZE_BYTES;
    }

    /**
     * Data elements from an ISO mdoc, grouped by namespace then element identifier.
     * The verifier only returns whether the signature and digests matched.
     */
    public static Map<String, Object> extractMdocClaims(String credential) {
        return extractMdocClaims(credential, List.of());
    }

    public static Map<String, Object> extractMdocClaims(String credential, List<String> mdocMetaClaims) {
        try {
            String trimmed = credential.trim();
            ensureMdocSize(trimmed);
            CBORObject namespaces = mdocNamespaces(readMdocRoot(trimmed));
            if (namespaces == null || namespaces.getType() != CBORType.Map) {
                throw new InvalidCredentialException("Failed to extract mdoc claims", null);
            }
            Map<String, Object> claims = new LinkedHashMap<>();
            for (CBORObject namespace : namespaces.getKeys()) {
                String namespaceName = namespace.getType() == CBORType.TextString ? namespace.AsString() : namespace.toString();
                CBORObject items = namespaces.get(namespace);
                if (items.getType() != CBORType.Array) {
                    throw new InvalidCredentialException("Invalid mdoc namespace: expected an item array", null);
                }
                Map<String, Object> namespaceClaims = new LinkedHashMap<>();
                claims.put(namespaceName, namespaceClaims);
                for (int index = 0; index < items.size(); index++) {
                    CBORObject item = unwrapMdocItem(items.get(index));
                    CBORObject identifier = item == null ? null : item.get(Constants.KEY_ELEMENT_IDENTIFIER);
                    if (identifier == null
                            || identifier.getType() != CBORType.TextString
                            || !item.ContainsKey(Constants.KEY_ELEMENT_VALUE)) {
                        throw new InvalidCredentialException("Invalid mdoc issuer-signed item", null);
                    }
                    String elementIdentifier = identifier.AsString();
                    if (namespaceClaims.containsKey(elementIdentifier)) {
                        throw new InvalidCredentialException("Duplicate mdoc element identifier", null);
                    }
                    namespaceClaims.put(elementIdentifier,
                            mdocValueToJava(item.get(Constants.KEY_ELEMENT_VALUE), 0));
                }
            }
            excludeMdocMetaClaims(mdocMetaClaims, claims);
            return claims;
        } catch (InvalidCredentialException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to extract mdoc claims", e);
        }
    }

    private static void ensureMdocSize(String credential) {
        if (decodedBase64Length(credential) > maxMdocSizeBytes) {
            throw new InvalidCredentialException("Credential exceeds the maximum supported size", null);
        }
    }

    /** Decoded length of a base64 / base64url string, computed without decoding it. */
    private static long decodedBase64Length(String encoded) {
        int length = encoded.length();
        int padding = 0;
        while (padding < 2 && length - padding > 0 && encoded.charAt(length - padding - 1) == '=') {
            padding++;
        }
        return ((long) (length - padding) * 3) / 4;
    }

    /** Reads the first CBOR item only; DecodeFromBytes rejects a buffer that continues past it. */
    private static CBORObject readMdocRoot(String trimmed) {
        return CBORObject.Read(new ByteArrayInputStream(Base64.getUrlDecoder().decode(trimmed)));
    }

    private static void ensureMdocHasDocument(CBORObject root) {
        if (root.ContainsKey(Constants.KEY_DOCUMENTS)) {
            CBORObject documents = root.get(Constants.KEY_DOCUMENTS);
            if (documents.getType() != CBORType.Array || documents.size() == 0) {
                throw new InvalidCredentialException("mdoc contains no documents", null);
            }
        }
    }

    private static CBORObject mdocNamespaces(CBORObject root) {
        if (root.ContainsKey(Constants.KEY_DOCUMENTS)) {
            CBORObject documents = root.get(Constants.KEY_DOCUMENTS);
            if (documents.getType() != CBORType.Array || documents.size() == 0) {
                throw new InvalidCredentialException("mdoc contains no documents", null);
            }
            CBORObject issuerSigned = documents.get(0).get(Constants.KEY_ISSUER_SIGNED);
            if (issuerSigned != null && issuerSigned.ContainsKey(Constants.KEY_NAMESPACES)) {
                return issuerSigned.get(Constants.KEY_NAMESPACES);
            }
            throw new InvalidCredentialException("Failed to extract mdoc claims", null);
        }
        if (root.ContainsKey(Constants.KEY_ISSUER_SIGNED) && root.get(Constants.KEY_ISSUER_SIGNED).ContainsKey(Constants.KEY_NAMESPACES)) {
            return root.get(Constants.KEY_ISSUER_SIGNED).get(Constants.KEY_NAMESPACES);
        }
        if (root.ContainsKey(Constants.KEY_ISSUER_AUTH) && root.ContainsKey(Constants.KEY_NAMESPACES)) {
            return root.get(Constants.KEY_NAMESPACES);
        }
        throw new InvalidCredentialException("Failed to extract mdoc claims", null);
    }

    private static CBORObject unwrapMdocItem(CBORObject item) {
        CBORObject current = item;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (current.HasMostOuterTag(24)) {
                current = current.Untag();
            }
            if (current.getType() == CBORType.ByteString) {
                current = CBORObject.DecodeFromBytes(current.GetByteString());
                continue;
            }
            break;
        }
        return current.getType() == CBORType.Map ? current : null;
    }

    private static Object mdocValueToJava(CBORObject value, int depth) {
        if (depth > MAX_MDOC_DEPTH) {
            throw new IllegalArgumentException("mdoc claim nesting is too deep");
        }
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.HasMostOuterTag(4)) {
            if (value.getType() != CBORType.Array
                    || value.size() != 2
                    || value.get(0).getType() != CBORType.Integer
                    || value.get(1).getType() != CBORType.Integer) {
                throw new IllegalArgumentException("Invalid CBOR decimal fraction");
            }
            int exponent = value.get(0).AsEIntegerValue().ToInt32Checked();
            BigDecimal mantissa = new BigDecimal(value.get(1).AsEIntegerValue().toString());
            return mantissa.scaleByPowerOfTen(exponent);
        }
        if (value.HasMostOuterTag(0) && value.getType() == CBORType.TextString) {
            return normalizeMdocDateTime(value.AsString());
        }
        if (value.HasMostOuterTag(1) && (value.getType() == CBORType.Integer || value.getType() == CBORType.FloatingPoint)) {
            CBORObject epoch = value.UntagOne();
            Instant instant = epoch.getType() == CBORType.Integer
                    ? Instant.ofEpochSecond(epoch.AsNumber().ToInt64Checked())
                    : Instant.ofEpochMilli(Math.round(epoch.AsDouble() * 1000));
            return instant.toString();
        }
        if (value.HasMostOuterTag(24) && value.getType() == CBORType.ByteString) {
            return mdocValueToJava(CBORObject.DecodeFromBytes(value.Untag().GetByteString()), depth + 1);
        }
        if (value.isTagged() && !value.HasMostOuterTag(2) && !value.HasMostOuterTag(3)) {
            // Other tags (e.g. 1004 full-date) are not interpreted: use the tagged content, as the UI does.
            // Bignums (tags 2 and 3) are kept tagged so they still read as integers.
            return mdocValueToJava(value.UntagOne(), depth + 1);
        }
        return switch (value.getType()) {
            case TextString -> value.AsString();
            case Integer, Number -> {
                var number = value.AsNumber();
                // CanFitInInt64 is false for non-integral values, so nothing is truncated.
                yield number.CanFitInInt64() ? (Object) number.ToInt64Checked() : number.toString();
            }
            case Boolean -> value.AsBoolean();
            case ByteString -> Base64.getEncoder().encodeToString(value.GetByteString());
            case FloatingPoint -> value.AsDouble();
            case Array -> {
                List<Object> values = new ArrayList<>();
                for (int index = 0; index < value.size(); index++) {
                    values.add(mdocValueToJava(value.get(index), depth + 1));
                }
                yield values;
            }
            case Map -> {
                Map<String, Object> values = new LinkedHashMap<>();
                for (CBORObject key : value.getKeys()) {
                    String name = key.getType() == CBORType.TextString ? key.AsString() : key.toString();
                    values.put(name, mdocValueToJava(value.get(key), depth + 1));
                }
                yield values;
            }
            default -> value.toString();
        };
    }

    private static Map<String, Object> extractLdpClaims(String verifiableCredential) {
        try {
            JSONObject vcObject = new JSONObject(verifiableCredential);
            JSONObject credentialSubject = vcObject.optJSONObject(Constants.KEY_CREDENTIAL_SUBJECT);
            return credentialSubject != null ? credentialSubject.toMap() : Map.of();
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to extract JSON claims", e);
        }
    }

    /** Returns all claims from an SD-JWT (payload + disclosures decoded), minus any meta claims. */
    public static Map<String, Object> extractSdJwtClaims(String verifiableCredential, List<String> metaClaims) {
        try {
            SDJWT sdjwt = SDJWT.parse(verifiableCredential);
            String payloadJson = decodeBase64Json(sdjwt.getCredentialJwt().split("\\.")[1]);
            Map<String, Object> payloadClaims = new JSONObject(payloadJson).toMap();
            List<Disclosure> disclosures = sdjwt.getDisclosures();
            SDObjectDecoder decoder = new SDObjectDecoder();
            Map<String, Object> claims = new HashMap<>(decoder.decode(payloadClaims, disclosures));
            // Authlete's top-level decode does not always expand nested `_sd` digests that
            // appear inside a selectively disclosed object (common for Multipaz EU PID
            // age_equal_or_over / "18"). Walk the tree and decode remaining SD objects.
            expandNestedSdClaims(claims, disclosures, decoder);
            excludeMetaClaims(metaClaims, claims);
            return claims;
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to extract SD-JWT claims", e);
        }
    }

    /**
     * Recursively applies remaining SD-JWT disclosures to nested maps/lists that still
     * contain {@code _sd} (or array ellipsis) after the top-level {@link SDObjectDecoder#decode}.
     */
    private static void expandNestedSdClaims(Map<String, Object> claims,
                                             List<Disclosure> disclosures,
                                             SDObjectDecoder decoder) {
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            entry.setValue(expandSdValue(entry.getValue(), disclosures, decoder));
        }
    }

    private static Object expandSdValue(Object value,
                                        List<Disclosure> disclosures,
                                        SDObjectDecoder decoder) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    nested.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            if (nested.containsKey("_sd")) {
                nested = new HashMap<>(decoder.decode(nested, disclosures));
            }
            for (Map.Entry<String, Object> e : nested.entrySet()) {
                e.setValue(expandSdValue(e.getValue(), disclosures, decoder));
            }
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> decoded = new ArrayList<>(decoder.decode(list, disclosures));
            for (int i = 0; i < decoded.size(); i++) {
                decoded.set(i, expandSdValue(decoded.get(i), disclosures, decoder));
            }
            return decoded;
        }
        return value;
    }

    private static void excludeMetaClaims(List<String> metaClaims, Map<String, Object> claims) {
        for (String metaClaim : Optional.ofNullable(metaClaims).orElseGet(List::of)) {
            if (metaClaim != null) {
                claims.remove(metaClaim.trim());
            }
        }
    }

    /**
     * tdate (tag 0) as an ISO-8601 UTC instant, the same text the UI shows. Unparseable text is
     * returned unchanged rather than failing claim extraction.
     */
    private static String normalizeMdocDateTime(String text) {
        try {
            return OffsetDateTime.parse(text).toInstant().toString();
        } catch (Exception e) {
            return text;
        }
    }

    /**
     * Removes configured mdoc meta elements. Each entry is {@code <namespace>.<element>}
     * and is removed from that namespace only. Namespaces and element identifiers can both
     * contain dots, so every split point is tried against the namespaces actually present.
     */
    private static void excludeMdocMetaClaims(List<String> mdocMetaClaims, Map<String, Object> claims) {
        for (String metaClaim : Optional.ofNullable(mdocMetaClaims).orElseGet(List::of)) {
            if (metaClaim == null) {
                continue;
            }
            String trimmed = metaClaim.trim();
            for (int separator = trimmed.indexOf('.'); separator >= 0; separator = trimmed.indexOf('.', separator + 1)) {
                if (separator == 0 || separator == trimmed.length() - 1) {
                    continue;
                }
                Object namespaceClaims = claims.get(trimmed.substring(0, separator));
                if (namespaceClaims instanceof Map<?, ?> map) {
                    map.remove(trimmed.substring(separator + 1));
                }
            }
        }
    }

    /**
     * An ISO mdoc is base64url CBOR. It is not a CWT and not JSON, so it has to be
     * recognised before those checks or it is sent to the JSON-LD verifier.
     * Size is not part of detection: an oversized mdoc is still an mdoc and is refused later.
     */
    public static boolean isMsoMdoc(String credential) {
        return detectMdoc(credential).isMdoc();
    }

    /** Detection result; {@code root} is the parsed CBOR when it could be read within the size limit. */
    private record MdocDetection(boolean isMdoc, CBORObject root) {
        private static final MdocDetection NOT_MDOC = new MdocDetection(false, null);
    }

    private static MdocDetection detectMdoc(String credential) {
        if (credential == null) {
            return MdocDetection.NOT_MDOC;
        }
        String trimmed = credential.trim();
        if (trimmed.isEmpty() || trimmed.indexOf('.') >= 0 || trimmed.charAt(0) == '{' || trimmed.charAt(0) == '[') {
            return MdocDetection.NOT_MDOC;
        }
        byte[] encoded;
        try {
            encoded = Base64.getUrlDecoder().decode(trimmed);
        } catch (IllegalArgumentException invalidBase64) {
            return MdocDetection.NOT_MDOC;
        }
        if (encoded.length > maxMdocSizeBytes) {
            // Oversized input is not parsed into a CBOR tree. A linear scan of the whole
            // payload still recognises an mdoc, so getCredentialFormat reports the size error.
            return new MdocDetection(hasMdocShapeKeys(encoded), null);
        }
        try {
            CBORObject root = CBORObject.Read(new ByteArrayInputStream(encoded));
            boolean isMdoc = root.getType() == CBORType.Map
                    && (root.ContainsKey(Constants.KEY_DOCUMENTS)
                    || root.ContainsKey(Constants.KEY_ISSUER_SIGNED)
                    || (root.ContainsKey(Constants.KEY_ISSUER_AUTH) && root.ContainsKey(Constants.KEY_NAMESPACES)));
            return isMdoc ? new MdocDetection(true, root) : MdocDetection.NOT_MDOC;
        } catch (Exception corrupt) {
            return new MdocDetection(hasMdocShapeKeys(encoded), null);
        }
    }

    private static boolean hasMdocShapeKeys(byte[] encoded) {
        return containsMdocShapeKey(encoded, Constants.KEY_DOCUMENTS)
                || containsMdocShapeKey(encoded, Constants.KEY_ISSUER_SIGNED)
                || (containsMdocShapeKey(encoded, Constants.KEY_ISSUER_AUTH)
                && containsMdocShapeKey(encoded, Constants.KEY_NAMESPACES));
    }

    private static boolean containsMdocShapeKey(byte[] encoded, String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.US_ASCII);
        int cborTextHeader = 0x60 | keyBytes.length;
        for (int start = 0; start <= encoded.length - keyBytes.length - 1; start++) {
            if ((encoded[start] & 0xff) != cborTextHeader) {
                continue;
            }
            int offset = 1;
            while (offset <= keyBytes.length && encoded[start + offset] == keyBytes[offset - 1]) {
                offset++;
            }
            if (offset > keyBytes.length) {
                return true;
            }
        }
        return false;
    }

    public static CredentialFormat getCredentialFormat(String verifiableCredential) {
        try {
            MdocDetection mdoc = detectMdoc(verifiableCredential);
            if (mdoc.isMdoc()) {
                ensureMdocSize(verifiableCredential.trim());
                if (mdoc.root() == null) {
                    throw new InvalidCredentialException("Failed to decode mdoc credential", null);
                }
                try {
                    ensureMdocHasDocument(mdoc.root());
                } catch (InvalidCredentialException e) {
                    throw e;
                } catch (Exception e) {
                    throw new InvalidCredentialException("Failed to decode mdoc credential", e);
                }
                return CredentialFormat.MSO_MDOC;
            }

            if (Utils.isCwt(verifiableCredential)) {
                return CredentialFormat.CWT_VC;
            }

            if (Utils.isSdJwt(verifiableCredential)) {
                return Constants.FORMAT_VC_SD_JWT.equals(extractSdJwtTyp(verifiableCredential))
                        ? CredentialFormat.VC_SD_JWT
                        : CredentialFormat.DC_SD_JWT;
            }

            return CredentialFormat.LDP_VC;

        } catch (InvalidCredentialException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to determine credential type.", e);
        }
    }

    public static Map<String, Object> extractCwtClaims(String credential, PixelPass pixelPass, List<String> metaClaims) {

        try {

            CBORObject cwt = decodeCwt(credential);
            CBORObject claims = decodeCwtClaims(cwt);

            JSONObject finalClaimsJson;

            CBORObject claim169 = claims.get(CBORObject.FromObject(169));
            if (claim169 != null) {

                CBORObject decodedClaim169 = CBORObject.DecodeFromBytes(
                        claim169.GetByteString(),
                        new CBOREncodeOptions("allowduplicatekeys=false")
                );

                String claim169Hex = bytesToHex(decodedClaim169.EncodeToBytes());
                String decodedClaim169Json = pixelPass.decodeMappedData(claim169Hex);

                finalClaimsJson = new JSONObject(decodedClaim169Json);
            } else {
                finalClaimsJson = new JSONObject();
            }

            Map<String, Object> finalClaimsMap = finalClaimsJson.toMap();
            excludeMetaClaims(metaClaims, finalClaimsMap);
            return finalClaimsMap;
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to extract CWT claims", e);
        }
    }

    private static CBORObject decodeCwt(String credential) {
        byte[] decodedBytes = hexToBytes(credential);
        return CBORObject.DecodeFromBytes(decodedBytes);
    }

    private static CBORObject decodeCwtClaims(CBORObject coseObj) {
        byte[] payloadBytes = coseObj.get(2).GetByteString();
        return CBORObject.DecodeFromBytes(
                payloadBytes,
                new CBOREncodeOptions("allowduplicatekeys=false")
        );
    }
}
