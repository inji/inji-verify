package io.inji.verify.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.dcql.*;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.shared.Constants;
import io.inji.verify.utils.Utils;
import org.springframework.stereotype.Component;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DcqlValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Per DCQL spec: non-empty string of alphanumeric, underscore, or hyphen characters. */
    private static final java.util.regex.Pattern ID_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            Constants.FORMAT_DC_SD_JWT,
            Constants.FORMAT_VC_SD_JWT,
            Constants.FORMAT_LDP_VC
    );

    private static boolean isSdJwtFormat(String format) {
        return Constants.FORMAT_DC_SD_JWT.equals(format) || Constants.FORMAT_VC_SD_JWT.equals(format);
    }

    public void validate(DCQLQueryDto query) {
        validateCredentialIds(query);
        validateCredentialSets(query);

        for (CredentialQueryDto credential : query.getCredentials()) {
            validateSupportedFormat(credential);
            validateCredentialMeta(credential);
            validateMetaValues(credential.getMeta());
            validateClaimIds(credential);
            validateClaimPaths(credential);
            validateClaimValues(credential);
            validateClaimSets(credential);
        }
    }

    private static void validateCredentialIds(DCQLQueryDto query) {
        Set<String> credentialIds = new HashSet<>();

        for (CredentialQueryDto credential : query.getCredentials()) {
            String id = credential.getId();
            if (id == null || id.isBlank()) {
                throw new VPRequestValidationException(ErrorCode.DCQL_CREDENTIAL_ID_REQUIRED);
            }
            if (!ID_PATTERN.matcher(id).matches()) {
                throw new VPRequestValidationException(ErrorCode.DCQL_CREDENTIAL_ID_INVALID);
            }
            if (!credentialIds.add(id)) {
                throw new VPRequestValidationException(ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID);
            }
        }
    }

    private static void validateCredentialSets(DCQLQueryDto query) {
        if (query.getCredentialSets() == null) {
            return;
        }

        boolean anyRequired = query.getCredentialSets().stream()
                .anyMatch(CredentialSetQueryDto::isRequired);
        if (!anyRequired) {
            throw new VPRequestValidationException(ErrorCode.DCQL_ALL_CREDENTIAL_SETS_OPTIONAL);
        }

        Set<String> credentialIds = query.getCredentials()
                .stream()
                .map(CredentialQueryDto::getId)
                .collect(Collectors.toSet());

        for (CredentialSetQueryDto credentialSet : query.getCredentialSets()) {
            for (List<String> option : credentialSet.getOptions()) {
                for (String credentialId : option) {
                    if (!credentialIds.contains(credentialId)) {
                        throw new VPRequestValidationException(
                                ErrorCode.DCQL_INVALID_CREDENTIAL_SET);
                    }
                }
            }
        }

        for (CredentialSetQueryDto credentialSet : query.getCredentialSets()) {
            for (List<String> option : credentialSet.getOptions()) {

                Set<String> uniqueIds = new HashSet<>();

                for (String credentialId : option) {
                    if (!uniqueIds.add(credentialId)) {
                        throw new VPRequestValidationException(
                                ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID);
                    }
                }
            }
        }
    }

    private static void validateClaimIds(CredentialQueryDto credential) {
        if (credential.getClaims() == null) {
            return;
        }

        Set<String> claimIds = new HashSet<>();

        if (credential.getClaimSets() != null) {
            for (ClaimQueryDto claim : credential.getClaims()) {
                if (claim.getId() == null || claim.getId().isBlank()) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_MISSING_CLAIM_ID);
                }
            }
        }

        for (ClaimQueryDto claim : credential.getClaims()) {
            String claimId = claim.getId();

            if (claimId == null || claimId.isBlank()) {
                continue;
            }

            if (!ID_PATTERN.matcher(claimId).matches()) {
                throw new VPRequestValidationException(ErrorCode.DCQL_CLAIM_ID_INVALID);
            }

            if (!claimIds.add(claimId)) {
                throw new VPRequestValidationException(ErrorCode.DCQL_DUPLICATE_CLAIM_ID);
            }
        }
    }

    /**
     * Per the DCQL spec:
     * - Each path element must be a non-blank string, null (wildcard), or a non-negative integer.
     * - The same claim path MUST NOT appear more than once within a single credential query,
     *   unless {@code claim_sets} is present — in that case duplicate paths are allowed because
     *   different claim IDs may intentionally alias the same path for use in separate options.
     */
    private static void validateClaimPaths(CredentialQueryDto credential) {
        if (credential.getClaims() == null) {
            return;
        }
        Set<List<Object>> seenPaths = new HashSet<>();
        for (ClaimQueryDto claim : credential.getClaims()) {
            for (Object element : claim.getPath()) {
                if (element == null) continue; // null is valid (wildcard)
                if (element instanceof String) {
                    if (((String) element).isBlank()) {
                        throw new VPRequestValidationException(ErrorCode.DCQL_CLAIM_PATH_INVALID);
                    }
                    continue;
                }
                if (element instanceof Integer || element instanceof Long) {
                    long value = element instanceof Integer ? (Integer) element : (Long) element;
                    if (value < 0 || value > Integer.MAX_VALUE) {
                        throw new VPRequestValidationException(ErrorCode.DCQL_CLAIM_PATH_INVALID);
                    }
                    continue;
                }
                throw new VPRequestValidationException(ErrorCode.DCQL_CLAIM_PATH_INVALID);
            }
            // Duplicate paths are allowed when claim_sets is present, because different
            // claim IDs may intentionally alias the same path for use in separate options.
            boolean hasClaimSets = credential.getClaimSets() != null && !credential.getClaimSets().isEmpty();
            if (!hasClaimSets && !seenPaths.add(claim.getPath())) {
                throw new VPRequestValidationException(ErrorCode.DCQL_DUPLICATE_CLAIM_PATH);
            } else {
                seenPaths.add(claim.getPath());
            }
        }
    }

    private static void validateSupportedFormat(CredentialQueryDto credential) {
        if (!SUPPORTED_FORMATS.contains(credential.getFormat())) {
            throw new VPRequestValidationException(ErrorCode.DCQL_CREDENTIAL_FORMAT_INVALID);
        }
    }

    private static void validateCredentialMeta(CredentialQueryDto credential) {
        CredentialMetaDto meta = credential.getMeta();

        switch (credential.getFormat()) {
            case Constants.FORMAT_DC_SD_JWT:
            case Constants.FORMAT_VC_SD_JWT:
                if (meta.getTypeValues() != null) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_META_NOT_MATCHING_FORMAT);
                }
                break;

            case Constants.FORMAT_LDP_VC:
                if (meta.getVctValues() != null) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_META_NOT_MATCHING_FORMAT);
                }
                break;
        }
    }

    private static void validateMetaValues(CredentialMetaDto meta) {
        if (meta.getVctValues() != null &&
                meta.getVctValues().size() != new HashSet<>(meta.getVctValues()).size()) {
            throw new VPRequestValidationException(
                    ErrorCode.DCQL_META_DUPLICATES);
        }

        if (meta.getTypeValues() != null &&
                meta.getTypeValues().size() != new HashSet<>(meta.getTypeValues()).size()) {
            throw new VPRequestValidationException(
                    ErrorCode.DCQL_META_DUPLICATES);
        }
    }

    /**
     * Validates that each element in a claim's {@code values} array is a string, integer (int or long),
     * boolean, or null. Floats/doubles and all other types are not permitted per the DCQL spec.
     */
    private static void validateClaimValues(CredentialQueryDto credential) {
        if (credential.getClaims() == null) {
            return;
        }
        for (ClaimQueryDto claim : credential.getClaims()) {
            if (claim.getValues() == null) {
                continue;
            }
            for (Object value : claim.getValues()) {
                if (value == null
                        || value instanceof String
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Boolean) {
                    continue;
                }
                throw new VPRequestValidationException(ErrorCode.DCQL_CLAIM_VALUES_INVALID);
            }
        }
    }

    private static void validateClaimSets(CredentialQueryDto credential) {
        if (credential.getClaimSets() == null) {
            return;
        }

        if (credential.getClaims() == null) {
            throw new VPRequestValidationException(ErrorCode.DCQL_INVALID_CLAIM_SET);
        }

        Set<String> claimIds = credential.getClaims()
                .stream()
                .map(ClaimQueryDto::getId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());

        for (List<String> claimSet : credential.getClaimSets()) {
            for (String claimId : claimSet) {
                if (!claimIds.contains(claimId)) {
                    throw new VPRequestValidationException(ErrorCode.DCQL_INVALID_CLAIM_SET);
                }
            }
        }
        //reject claim ids that are duplicated within a single claim set
        for (List<String> claimSet : credential.getClaimSets()) {
            Set<String> uniqueIds = new HashSet<>();

            for (String claimId : claimSet) {
                if (!uniqueIds.add(claimId)) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_DUPLICATE_CLAIM_ID);
                }
            }
        }
    }

    /**
     * Validates the vp_token against the DCQL query at submission time.
     *
     * A: vp_token keys must only contain credential ids declared in the DCQL query.
     * B: submitted ids must satisfy credential_sets; if absent, all declared ids must be present.
     * C: each submitted credential must match the format declared in its DCQL query entry.
     * D: if multiple=false (default), each credential array must contain exactly one element.
     * E: each element must satisfy require_cryptographic_holder_binding (default true):
     *      ldp_vc  — true: must be a VerifiablePresentation; false: must be a VerifiableCredential.
     *      sd-jwt  — true: KB-JWT required; false: KB-JWT optional (per IETF SD-JWT spec).
     * F: each ldp_vc element must satisfy meta.type_values if present (OR-of-ANDs per DCQL spec):
     *      at least one inner type array must be fully present in the credential's type field.
     *      When holderBindingRequired=true, type check is applied to each inner VC inside the VP.
     * G: each SD-JWT element must satisfy meta.vct_values if present:
     *      the vct claim must be present and exactly match one of the allowed values.
     * H: each ldp_vc element must satisfy all declared claims (credential.claims):
     *      - path is resolved within credentialSubject per DCQL §7.1.1 (string→object key,
     *        null→array wildcard, integer→array index; type mismatch aborts with error).
     *      - if values are declared, the resolved value must type-and-value match at least one
     *        declared value (OR). Matching is type-exact per DCQL spec (String/Boolean/Integer/Long/null).
     *      - when holderBindingRequired=true, all inner VCs inside the VP must satisfy requirements.
     *      - when claim_sets is present, at least one option must be fully satisfied (OR-of-ANDs).
     * I: each SD-JWT element must satisfy all declared claims (credential.claims):
     *      - only selectively disclosed claims are checked (§6.4.1: claims targets selectively
     *        disclosable claims; mandatory payload claims like iss/vct/iat are excluded).
     *      - path is resolved at the top level of the disclosed claims (no credentialSubject wrapper).
     *      - value matching uses the same type-exact rules as H.
     *      - when claim_sets is present, at least one option must be fully satisfied (OR-of-ANDs).
     */
    public void validateVpTokenAgainstDcql(DCQLQueryDto dcqlQuery, JsonNode vpTokenNode) {
        Map<String, CredentialQueryDto> dcqlCredentialQueries = dcqlQuery.getCredentials().stream()
                .collect(Collectors.toMap(CredentialQueryDto::getId, cq -> cq));

        validateNoUnknownCredentialIds(vpTokenNode, dcqlCredentialQueries.keySet());

        Set<String> submittedCredentialIds = fieldNamesAsSet(vpTokenNode);

        validateCredentialSetsSatisfied(dcqlQuery, submittedCredentialIds, dcqlCredentialQueries.keySet());

        for (String credentialId : submittedCredentialIds) {
            CredentialQueryDto matchingQuery = dcqlCredentialQueries.get(credentialId);
            if (matchingQuery == null) {
                continue; // already caught by validateNoUnknownCredentialIds
            }
            JsonNode submittedCredentials = vpTokenNode.get(credentialId);
            try {
                validateCredentialFormat(matchingQuery, submittedCredentials);
                validateMultipleConstraint(matchingQuery, submittedCredentials);

                boolean holderBindingRequired = isHolderBindingRequired(matchingQuery);

                validateHolderBinding(matchingQuery, submittedCredentials, holderBindingRequired);
                validateTypeValues(matchingQuery, submittedCredentials, holderBindingRequired);
                validateVctValues(matchingQuery, submittedCredentials);
                validateLdpClaims(matchingQuery, submittedCredentials, holderBindingRequired);
                validateSdJwtClaims(matchingQuery, submittedCredentials);
            } catch (VPRequestValidationException e) {
                throw e.withCredentialId(credentialId);
            }
        }
    }

    /** Collects the field names of a JSON object node into a Set. */
    private static Set<String> fieldNamesAsSet(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /** A: vp_token keys must only contain credential ids declared in the DCQL query. */
    private static void validateNoUnknownCredentialIds(JsonNode vpTokenNode, Set<String> requestedIds) {
        vpTokenNode.fieldNames().forEachRemaining(id -> {
            if (!requestedIds.contains(id)) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_UNKNOWN_CREDENTIAL_ID);
            }
        });
    }

    /** B: submitted ids must satisfy credential_sets; if absent, all declared ids must be present. */
    private static void validateCredentialSetsSatisfied(DCQLQueryDto dcqlQuery, Set<String> submittedIds, Set<String> requestedIds) {
        if (dcqlQuery.getCredentialSets() == null || dcqlQuery.getCredentialSets().isEmpty()) {
            for (String requestedId : requestedIds) {
                if (!submittedIds.contains(requestedId)) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_MISSING_CREDENTIAL_ID);
                }
            }
        } else {
            for (CredentialSetQueryDto credentialSet : dcqlQuery.getCredentialSets()) {
                if (!credentialSet.isRequired()) {
                    continue;
                }
                boolean anyOptionSatisfied = credentialSet.getOptions().stream()
                        .anyMatch(submittedIds::containsAll);
                if (!anyOptionSatisfied) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED);
                }
            }
        }
    }

    /** C: each submitted credential must match the format declared in its DCQL query entry. */
    private static void validateCredentialFormat(CredentialQueryDto matchingQuery, JsonNode submittedCredentials) {
        for (JsonNode submittedCredential : submittedCredentials) {
            switch (matchingQuery.getFormat()) {
                case Constants.FORMAT_LDP_VC:
                    if (!submittedCredential.isObject()
                            || (!Utils.isLdpFormat(submittedCredential, Constants.LDP_TYPE_VERIFIABLE_PRESENTATION)
                                && !Utils.isLdpFormat(submittedCredential, Constants.LDP_TYPE_VERIFIABLE_CREDENTIAL))) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH);
                    }
                    break;
                case Constants.FORMAT_DC_SD_JWT:
                case Constants.FORMAT_VC_SD_JWT:
                    if (!submittedCredential.isTextual() || !Utils.isSdJwt(submittedCredential.asText())) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** D: if multiple=false (default), each credential array must contain exactly one element. */
    private static void validateMultipleConstraint(CredentialQueryDto matchingQuery, JsonNode submittedCredentials) {
        if (!matchingQuery.isMultiple() && submittedCredentials.size() > 1) {
            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_MULTIPLE_CREDENTIALS_NOT_ALLOWED);
        }
    }

    /** Returns true if require_cryptographic_holder_binding is true (defaults to true per spec). */
    private static boolean isHolderBindingRequired(CredentialQueryDto matchingQuery) {
        return matchingQuery.isRequire_cryptographic_holder_binding();
    }

    /**
     * E: each element must satisfy require_cryptographic_holder_binding (default true):
     *      ldp_vc  — true: must be a VerifiablePresentation; false: must be a VerifiableCredential.
     *      sd-jwt  — true: cnf claim must be present AND KB-JWT required (per OpenID4VP §6.4.2 /
     *                IETF SD-JWT spec); SD-JWTs without a cnf claim do not support Holder Binding
     *                and cannot satisfy this requirement.
     *                false: KB-JWT optional.
     */
    private static void validateHolderBinding(CredentialQueryDto matchingQuery, JsonNode submittedCredentials, boolean holderBindingRequired) {
        switch (matchingQuery.getFormat()) {
            case Constants.FORMAT_LDP_VC:
                for (JsonNode submittedCredential : submittedCredentials) {
                    if (holderBindingRequired && !Utils.isLdpFormat(submittedCredential, Constants.LDP_TYPE_VERIFIABLE_PRESENTATION)) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_EXPECTED_VERIFIABLE_PRESENTATION);
                    }
                    if (!holderBindingRequired && !Utils.isLdpFormat(submittedCredential, Constants.LDP_TYPE_VERIFIABLE_CREDENTIAL)) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_EXPECTED_VERIFIABLE_CREDENTIAL);
                    }
                }
                break;
            case Constants.FORMAT_DC_SD_JWT:
            case Constants.FORMAT_VC_SD_JWT:
                if (holderBindingRequired) {
                    for (JsonNode submittedCredential : submittedCredentials) {
                        if (!Utils.hasSdJwtCnfClaim(submittedCredential.asText())) {
                            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_SD_JWT_MISSING_CNF);
                        }
                        if (!Utils.hasSdJwtKeyBinding(submittedCredential.asText())) {
                            throw new VPRequestValidationException(ErrorCode.VP_TOKEN_SD_JWT_MISSING_KEY_BINDING);
                        }
                    }
                }
                break;
        }
    }

    /**
     * F: each ldp_vc element must satisfy meta.type_values if present (OR-of-ANDs per DCQL spec):
     *      at least one inner type array must be fully present in the credential's type field.
     *      When holderBindingRequired=true, types are checked against each inner VC inside the VP.
     *      All inner VCs must match — the wallet must not bundle mismatched credential types.
     */
    private static void validateTypeValues(CredentialQueryDto matchingQuery, JsonNode submittedCredentials, boolean holderBindingRequired) {
        if (!Constants.FORMAT_LDP_VC.equals(matchingQuery.getFormat())
                || matchingQuery.getMeta() == null
                || matchingQuery.getMeta().getTypeValues() == null
                || matchingQuery.getMeta().getTypeValues().isEmpty()) {
            return;
        }
        for (JsonNode submittedCredential : submittedCredentials) {
            if (!isTypeValuesSatisfied(matchingQuery, submittedCredential, holderBindingRequired)) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH);
            }
        }
    }

    private static boolean isTypeValuesSatisfied(CredentialQueryDto matchingQuery, JsonNode submittedCredential, boolean holderBindingRequired) {
        if (holderBindingRequired) {
            JsonNode vcArray = submittedCredential.get(Constants.KEY_VERIFIABLE_CREDENTIAL);
            if (vcArray == null || !vcArray.isArray() || vcArray.isEmpty()) {
                // VP without a verifiableCredential array is invalid
                return false;
            }
            for (JsonNode innerVc : vcArray) {
                Set<String> innerTypes = Utils.extractLdpTypes(innerVc);
                boolean innerVcMatchesTypeValues = matchingQuery.getMeta().getTypeValues().stream()
                        .anyMatch(option -> typeOptionMatches(option, innerTypes));
                if (!innerVcMatchesTypeValues) {
                    return false;
                }
            }
            return true;
        } else {
            Set<String> credentialTypes = Utils.extractLdpTypes(submittedCredential);
            return matchingQuery.getMeta().getTypeValues().stream()
                    .anyMatch(option -> typeOptionMatches(option, credentialTypes));
        }
    }

    /**
     * G: each SD-JWT element must satisfy meta.vct_values if present (per OpenID4VP spec):
     *      the vct claim must be present and match one of the allowed values (exact match only;
     *      type inheritance is not currently supported).
     */
    private static void validateVctValues(CredentialQueryDto matchingQuery, JsonNode submittedCredentials) {
        if (!isSdJwtFormat(matchingQuery.getFormat())
                || matchingQuery.getMeta() == null
                || matchingQuery.getMeta().getVctValues() == null
                || matchingQuery.getMeta().getVctValues().isEmpty()) {
            return;
        }
        for (JsonNode submittedCredential : submittedCredentials) {
            String vct = Utils.extractSdJwtVct(submittedCredential.asText());
            if (vct == null || !matchingQuery.getMeta().getVctValues().contains(vct)) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_SD_JWT_VCT_MISMATCH);
            }
        }
    }

    /**
     * H: each ldp_vc element must contain all declared claim paths.
     *    Paths are resolved within credentialSubject (e.g. ["name"] → credentialSubject.name).
     *    When holderBindingRequired=true, paths are resolved against each inner VC inside the VP.
     *    All inner VCs must satisfy the claim requirements.
     *    When claim_sets is present, at least one option (inner array of claim IDs) must be fully
     *    satisfied; otherwise all declared claims are required.
     */
    private static void validateLdpClaims(CredentialQueryDto matchingQuery, JsonNode submittedCredentials, boolean holderBindingRequired) {
        if (!Constants.FORMAT_LDP_VC.equals(matchingQuery.getFormat())
                || matchingQuery.getClaims() == null
                || matchingQuery.getClaims().isEmpty()) {
            return;
        }

        boolean hasClaimSets = matchingQuery.getClaimSets() != null && !matchingQuery.getClaimSets().isEmpty();

        for (JsonNode submittedCredential : submittedCredentials) {
            if (holderBindingRequired) {
                JsonNode vcArray = submittedCredential.get(Constants.KEY_VERIFIABLE_CREDENTIAL);
                if (vcArray == null || !vcArray.isArray()) {
                    throw new VPRequestValidationException(ErrorCode.VP_TOKEN_MISSING_VERIFIABLE_CREDENTIAL);
                }
                for (JsonNode innerVc : vcArray) {
                    JsonNode subject = innerVc.path(Constants.KEY_CREDENTIAL_SUBJECT);
                    if (hasClaimSets) {
                        validateClaimsAgainstClaimSets(matchingQuery.getClaims(), matchingQuery.getClaimSets(), subject);
                    } else {
                        validateClaims(matchingQuery.getClaims(), subject);
                    }
                }
            } else {
                JsonNode subject = submittedCredential.path(Constants.KEY_CREDENTIAL_SUBJECT);
                if (hasClaimSets) {
                    validateClaimsAgainstClaimSets(matchingQuery.getClaims(), matchingQuery.getClaimSets(), subject);
                } else {
                    validateClaims(matchingQuery.getClaims(), subject);
                }
            }
        }
    }

    /**
     * I: each SD-JWT element must satisfy all declared claims (credential.claims).
     * Only selectively disclosed claims are checked — mandatory payload claims (iss, vct, iat, etc.)
     * are excluded. This reflects §6.4.1: DCQL claims target selectively disclosable claims.
     *
     * If {@code claims} is absent, no claim validation is performed and extra disclosures are ignored.
     * The DCQL spec places a MUST on the wallet not to over-disclose, but the verifier MUST NOT rely
     * on the wallet to enforce constraints (OpenID4VP §6.4): we use what we need and ignore the rest.
     * This is consistent with our handling of extra disclosures when claims are declared.
     *
     * When {@code claim_sets} is present, at least one option (inner array of claim IDs) must be
     * fully satisfied by the disclosed claims (OR-of-ANDs); otherwise all declared claims are required.
     */
    private static void validateSdJwtClaims(CredentialQueryDto matchingQuery, JsonNode submittedCredentials) {
        if (!isSdJwtFormat(matchingQuery.getFormat())) {
            return;
        }
        if (matchingQuery.getClaims() == null || matchingQuery.getClaims().isEmpty()) {
            return;
        }

        boolean hasClaimSets = matchingQuery.getClaimSets() != null && !matchingQuery.getClaimSets().isEmpty();

        for (JsonNode submittedCredential : submittedCredentials) {
            JsonNode disclosedClaims = sdJwtDisclosedClaimsAsJson(submittedCredential.asText());
            if (hasClaimSets) {
                validateClaimsAgainstClaimSets(matchingQuery.getClaims(), matchingQuery.getClaimSets(), disclosedClaims);
            } else {
                validateClaims(matchingQuery.getClaims(), disclosedClaims);
            }
        }
    }

    /** Returns only the selectively disclosed claims from an SD-JWT directly as a JsonNode. */
    private static JsonNode sdJwtDisclosedClaimsAsJson(String sdJwt) {
        try {
            List<Disclosure> disclosures = SDJWT.parse(sdJwt).getDisclosures();
            ObjectNode result = MAPPER.createObjectNode();
            if (disclosures == null) {
                return result;
            }
            for (Disclosure d : disclosures) {
                if (d.getClaimName() != null) {
                    result.set(d.getClaimName(), claimValueToJsonNode(d.getClaimValue()));
                }
            }
            return result;
        } catch (VPRequestValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new VPRequestValidationException(ErrorCode.INVALID_VP_TOKEN);
        }
    }

    /**
     * Converts a disclosure claim value to a JsonNode, normalizing Double values that are
     * mathematically integers (e.g. 42.0 → IntNode(42)). This is necessary because some JSON
     * parsers (e.g. Gson) deserialize all JSON numbers as Double when the target type is Object.
     * Without this normalization, a JSON integer in a disclosure would produce a DoubleNode, which
     * would fail the type-exact integer match in claimValueMatches.
     */
    private static JsonNode claimValueToJsonNode(Object value) {
        if (value instanceof Double) {
            double d = (Double) value;
            if (d % 1.0 == 0.0 && d >= Long.MIN_VALUE && d <= (double) Long.MAX_VALUE) {
                long l = (long) d;
                return l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE
                        ? MAPPER.valueToTree((int) l)
                        : MAPPER.valueToTree(l);
            }
        }
        return MAPPER.valueToTree(value);
    }

    /**
     * Validates that at least one option (inner array of claim IDs) in {@code claimSets} is fully
     * satisfied by the given {@code root} node. Throws {@code VP_TOKEN_CLAIM_SETS_NOT_SATISFIED}
     * with a per-option failure reason if none pass, e.g.:
     * "... Option 1 [first_name, last_name]: claim 'last_name' (path: [last_name]) not found.
     *      Option 2 [given_name]: claim 'given_name' value mismatch (found: Alice, expected one of: [Bob])."
     */
    private static void validateClaimsAgainstClaimSets(List<ClaimQueryDto> claims,
                                                        List<List<String>> claimSets,
                                                        JsonNode root) {
        Map<String, ClaimQueryDto> claimById = new HashMap<>();
        for (ClaimQueryDto claim : claims) {
            if (claim.getId() != null) {
                claimById.put(claim.getId(), claim);
            }
        }
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < claimSets.size(); i++) {
            List<String> option = claimSets.get(i);
            String failure = describeOptionFailure(option, claimById, root);
            if (failure == null) {
                return; // this option is fully satisfied
            }
            detail.append(" Option ").append(i + 1).append(" ").append(option).append(": ").append(failure).append(".");
        }
        String fullMessage = ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED.getErrorMessage() + detail;
        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, fullMessage);
    }

    /**
     * Returns {@code null} if every claim in {@code option} is present and value-matched,
     * or a human-readable reason string describing the first failure found.
     * Path resolution errors count as "not satisfied" so the caller can try the next option.
     */
    private static String describeOptionFailure(List<String> option,
                                                 Map<String, ClaimQueryDto> claimById,
                                                 JsonNode root) {
        for (String claimId : option) {
            ClaimQueryDto claim = claimById.get(claimId);
            if (claim == null) {
                return "unknown claim id '" + claimId + "'";
            }
            try {
                List<JsonNode> resolved = resolvePath(claim.getPath(), root);
                if (resolved.isEmpty()) {
                    return "claim '" + claimId + "' (path: " + claim.getPath() + ") not found";
                }
                if (claim.getValues() != null && !claimValueMatches(resolved, claim.getValues())) {
                    String found = resolved.stream().map(JsonNode::asText).collect(Collectors.joining(", "));
                    return "claim '" + claimId + "' value mismatch (found: " + found
                            + ", expected one of: " + claim.getValues() + ")";
                }
            } catch (VPRequestValidationException e) {
                return "claim '" + claimId + "' (path: " + claim.getPath() + ") path type mismatch";
            }
        }
        return null; // all claims in this option satisfied
    }

    /**
     * Validates that all declared claim paths are present and, when {@code values} is specified,
     * that the resolved value matches at least one declared value.
     *
     * Claims listed in {@code claims} are always required — absence throws
     * {@code VP_TOKEN_CLAIM_NOT_FOUND}. When {@code values} is present and the claim IS found but
     * its value does not match, throws {@code VP_TOKEN_CLAIM_VALUE_MISMATCH}.
     *
     * Note: per DCQL spec §6.3, Verifiers MUST NOT rely on {@code values} as a security gate since
     * wallets may not always filter claims by value. Other security mechanisms should be employed.
     */
    private static void validateClaims(List<ClaimQueryDto> claims, JsonNode root) {
        for (ClaimQueryDto claim : claims) {
            String path = claim.getPath().toString();
            List<JsonNode> resolved = resolvePath(claim.getPath(), root);
            if (resolved.isEmpty()) {
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND,
                        ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND.getErrorMessage() + " Path: " + path);
            }
            if (claim.getValues() != null && !claimValueMatches(resolved, claim.getValues())) {
                String found = resolved.stream().map(JsonNode::asText).collect(Collectors.joining(", "));
                throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH,
                        ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH.getErrorMessage()
                                + " Path: " + path + ", found: " + found + ", expected one of: " + claim.getValues());
            }
        }
    }

    /**
     * Walks the path one step at a time per DCQL §7.1.1:
     *   string  — key lookup in object;  abort (throw) if current node is not an object, skip if key absent.
     *   null    — wildcard over array elements; abort if current node is not an array.
     *   integer — index into array;       abort if current node is not an array, skip if out of bounds.
     * Returns all matched nodes, or an empty list if a key/index is absent at any step.
     */
    private static List<JsonNode> resolvePath(List<Object> path, JsonNode root) {
        List<JsonNode> current = List.of(root);
        for (Object step : path) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                if (step instanceof String) {
                    if (!node.isObject()) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND,
                        ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND.getErrorMessage() + " Path: " + path);
                    }
                    JsonNode value = node.get((String) step);
                    if (value != null) next.add(value);
                } else if (step == null) {
                    if (!node.isArray()) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND,
                        ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND.getErrorMessage() + " Path: " + path);
                    }
                    node.elements().forEachRemaining(next::add);
                } else {
                    // non-negative integer
                    if (!node.isArray()) {
                        throw new VPRequestValidationException(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND,
                        ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND.getErrorMessage() + " Path: " + path);
                    }
                    int idx = step instanceof Integer ? (Integer) step : ((Long) step).intValue();
                    JsonNode value = node.get(idx);
                    if (value != null) next.add(value);
                }
            }
            current = next;
            if (current.isEmpty()) return current;
        }
        return current;
    }

    /**
     * Returns true if any resolved node matches any of the declared values (OR logic).
     * Per DCQL spec, both type and value must match exactly:
     *   null    → JSON null
     *   String  → JSON text node with identical string value
     *   Boolean → JSON boolean node with identical boolean value
     *   Integer/Long → JSON integral number node with identical numeric value
     */
    private static boolean claimValueMatches(List<JsonNode> resolved, List<Object> values) {
        for (JsonNode node : resolved) {
            for (Object value : values) {
                if (value == null   && node.isNull()) return true;
                if (value instanceof String  && node.isTextual()      && node.asText().equals(value)) return true;
                if (value instanceof Boolean && node.isBoolean()      && node.booleanValue() == (Boolean) value) return true;
                if (value instanceof Integer && node.isIntegralNumber() && node.asInt()  == (Integer) value) return true;
                if (value instanceof Long    && node.isIntegralNumber() && node.asLong() == (Long)    value) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all required types in one type_values option are satisfied by the
     * credential's type set. Supports exact match and IRI fragment match per DCQL spec.
     */
    private static boolean typeOptionMatches(List<String> requiredTypes, Set<String> credentialTypes) {
        for (String requiredType : requiredTypes) {
            boolean matched = credentialTypes.stream()
                    .anyMatch(ct -> Utils.ldpTypeMatches(ct, requiredType));
            if (!matched) return false;
        }
        return true;
    }

}
