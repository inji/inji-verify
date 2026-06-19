package io.inji.verify.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORTaggedItem;
import com.upokecenter.cbor.CBOREncodeOptions;
import com.upokecenter.cbor.CBORObject;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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

    public static ResponseEntity<Object> getResponseEntityForCredentialStatusException(CredentialStatusCheckException ex) {
        String errorMessage = ex.getErrorCode() + " - " + ex.getErrorDescription();
        CredentialStatusErrorDto credentialStatusErrorDto =
                new CredentialStatusErrorDto(Instant.now().toString(), 500, errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(credentialStatusErrorDto);
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
        return switch (format) {
            case VC_SD_JWT, DC_SD_JWT -> extractSdJwtClaims(verifiableCredential, metaClaims);
            case LDP_VC -> extractLdpClaims(verifiableCredential);
            case CWT_VC -> extractCwtClaims(verifiableCredential, pixelPass, metaClaims);
            default -> null;
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
            Map<String, Object> claims = new HashMap<>(new SDObjectDecoder().decode(payloadClaims, disclosures));
            excludeMetaClaims(metaClaims, claims);
            return claims;
        } catch (Exception e) {
            throw new InvalidCredentialException("Failed to extract SD-JWT claims", e);
        }
    }


    private static void excludeMetaClaims(List<String> metaClaims, Map<String, Object> claims) {
        for (String metaClaim : Optional.ofNullable(metaClaims).orElseGet(List::of)) {
            if (metaClaim != null) {
                claims.remove(metaClaim.trim());
            }
        }
    }

    public static CredentialFormat getCredentialFormat(String verifiableCredential) {
        try {
            if (Utils.isCwt(verifiableCredential)) {
                return CredentialFormat.CWT_VC;
            }

            if (Utils.isSdJwt(verifiableCredential)) {
                return Constants.FORMAT_VC_SD_JWT.equals(extractSdJwtTyp(verifiableCredential))
                        ? CredentialFormat.VC_SD_JWT
                        : CredentialFormat.DC_SD_JWT;
            }

            return CredentialFormat.LDP_VC;

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
            throw new InvalidCredentialException("Failed to determine credential type.", e);
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
