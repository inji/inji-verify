package io.inji.verify.validator;

import io.inji.verify.dto.dcql.ClaimQueryDto;
import io.inji.verify.dto.dcql.CredentialMetaDto;
import io.inji.verify.dto.dcql.CredentialQueryDto;
import io.inji.verify.dto.dcql.CredentialSetQueryDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPRequestValidationException;
import io.inji.verify.shared.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DcqlQueryValidatorTest {

    private DcqlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DcqlValidator();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CredentialQueryDto cred(String id) {
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC, new CredentialMetaDto(null, null), true, false, null, null);
    }

    private static CredentialQueryDto credWithFormat(String id, String format) {
        return new CredentialQueryDto(id, format, new CredentialMetaDto(null, null), true, false, null, null);
    }

    private static CredentialQueryDto credWithMeta(String id, String format, CredentialMetaDto meta) {
        return new CredentialQueryDto(id, format, meta, true, false, null, null);
    }

    private static CredentialQueryDto credWithClaims(String id, List<ClaimQueryDto> claims, List<List<String>> claimSets) {
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC, new CredentialMetaDto(null, null), true, false, claims, claimSets);
    }

    private static ClaimQueryDto claim(String claimId, String... path) {
        return new ClaimQueryDto(claimId, List.of(path), null);
    }

    private static CredentialSetQueryDto requiredSet(String... ids) {
        return new CredentialSetQueryDto(List.of(List.of(ids)), true);
    }

    private static CredentialSetQueryDto optionalSet(String... ids) {
        return new CredentialSetQueryDto(List.of(List.of(ids)), false);
    }

    // -------------------------------------------------------------------------
    // validateCredentialIds — format and uniqueness
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenCredentialIdsAreUnique() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1"), cred("cred2")), null);
        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldPass_whenCredentialIdContainsAlphanumericUnderscoreHyphen() {
        // all allowed chars: letters, digits, underscore, hyphen
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred_1-A")), null);
        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldFail_whenCredentialIdContainsSpace() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred 1")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CREDENTIAL_ID_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenCredentialIdContainsSpecialChar() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred@1")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CREDENTIAL_ID_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenDuplicateCredentialIds() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1"), cred("cred1")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateCredentialSets — all-optional guard
    // -------------------------------------------------------------------------

    @Test
    void shouldFail_whenSingleCredentialSetIsOptional() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1")),
                List.of(optionalSet("cred1")));

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_ALL_CREDENTIAL_SETS_OPTIONAL, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenAllCredentialSetsAreOptional() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(optionalSet("cred1"), optionalSet("cred2")));

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_ALL_CREDENTIAL_SETS_OPTIONAL, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenAtLeastOneCredentialSetIsRequired() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(requiredSet("cred1"), optionalSet("cred2")));

        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldPass_whenMultipleCredentialSetsAreAllRequired() {
        // e.g. {"credential_sets":[{"options":[["passport_query"]]},{"options":[["national_id_query"]]}]}
        // both sets omit "required" so they default to true — verifier demands both credentials
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(requiredSet("passport_query"), requiredSet("national_id_query")));

        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldPass_whenCredentialSetsAbsent() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validate(query));
    }

    // -------------------------------------------------------------------------
    // validateCredentialSets — unknown and duplicate id references
    // -------------------------------------------------------------------------

    @Test
    void shouldFail_whenCredentialSetReferencesUnknownId() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1")),
                List.of(requiredSet("unknown_id")));

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_INVALID_CREDENTIAL_SET, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenCredentialSetOptionHasDuplicateIds() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1")),
                List.of(new CredentialSetQueryDto(List.of(List.of("cred1", "cred1")), true)));

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateCredentialFormat
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenFormatIsLdpVc() {
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithFormat("c", Constants.FORMAT_LDP_VC)), null)));
    }

    @Test
    void shouldPass_whenFormatIsDcSdJwt() {
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithFormat("c", Constants.FORMAT_DC_SD_JWT)), null)));
    }

    @Test
    void shouldFail_whenFormatIsUnsupported() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("c", "jwt_vc_json")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CREDENTIAL_FORMAT_INVALID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateCredentialMeta — format/meta field cross-check
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenLdpVcHasTypeValues() {
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of(List.of("SomeCredential")));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null)));
    }

    @Test
    void shouldFail_whenLdpVcHasVctValues() {
        CredentialMetaDto meta = new CredentialMetaDto(List.of("https://example.com/SomeCredential"), null);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_NOT_MATCHING_FORMAT, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDcSdJwtHasVctValues() {
        CredentialMetaDto meta = new CredentialMetaDto(List.of("https://example.com/SomeCredential"), null);
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithMeta("c", Constants.FORMAT_DC_SD_JWT, meta)), null)));
    }

    @Test
    void shouldFail_whenDcSdJwtHasTypeValues() {
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of(List.of("SomeCredential")));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_DC_SD_JWT, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_NOT_MATCHING_FORMAT, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateMetaValues — duplicate vct_values / type_values
    // -------------------------------------------------------------------------

    @Test
    void shouldFail_whenVctValuesContainDuplicates() {
        CredentialMetaDto meta = new CredentialMetaDto(
                List.of("https://example.com/A", "https://example.com/A"), null);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_DC_SD_JWT, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_DUPLICATES, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesContainDuplicateOptions() {
        List<String> option = List.of("SomeCredential");
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of(option, option));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_DUPLICATES, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesOuterArrayIsEmpty() {
        // type_values: [] — outer array must be non-empty per spec
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of());
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesInnerArrayIsEmpty() {
        // type_values: [[]] — each inner array (AND-set) must not be empty
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of(List.of()));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesInnerArrayContainsDuplicateTypes() {
        // type_values: [["A", "A"]] — duplicate within an AND-set is invalid
        CredentialMetaDto meta = new CredentialMetaDto(null,
                List.of(List.of("VerifiableCredential", "VerifiableCredential")));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_DUPLICATES, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesInnerArrayContainsNullEntry() {
        List<String> option = new ArrayList<>();
        option.add("VerifiableCredential");
        option.add(null);
        CredentialMetaDto meta = new CredentialMetaDto(null, List.of(option));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTypeValuesInnerArrayContainsBlankEntry() {
        CredentialMetaDto meta = new CredentialMetaDto(null,
                List.of(List.of("VerifiableCredential", "   ")));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVctValuesContainsNullEntry() {
        List<String> vctValues = new ArrayList<>();
        vctValues.add("https://example.org/vct/MyCredential");
        vctValues.add(null);
        CredentialMetaDto meta = new CredentialMetaDto(vctValues, null);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_DC_SD_JWT, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVctValuesContainsBlankEntry() {
        CredentialMetaDto meta = new CredentialMetaDto(List.of("https://example.org/vct/MyCredential", "  "), null);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_DC_SD_JWT, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenTypeValuesContainAbsoluteIris() {
        List<List<String>> typeValues = List.of(
                List.of("https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/types/UniversityDegreeCredential"));
        CredentialMetaDto meta = new CredentialMetaDto(null, typeValues);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldPass_whenTypeValuesContainRelativeIris() {
        // Per spec, a type not defined in any @context remains a relative IRI after JSON-LD processing
        // and that relative IRI IS the fully expanded type — so it must be accepted.
        List<List<String>> typeValues = List.of(List.of("VerifiableCredential", "UniversityDegreeCredential"));
        CredentialMetaDto meta = new CredentialMetaDto(null, typeValues);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldFail_whenTypeValuesContainMalformedIri() {
        // A string with spaces or illegal characters is not a valid IRI reference
        List<List<String>> typeValues = List.of(List.of("not a valid iri"));
        CredentialMetaDto meta = new CredentialMetaDto(null, typeValues);
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithMeta("c", Constants.FORMAT_LDP_VC, meta)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_META_TYPE_VALUE_INVALID_IRI, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateClaimIds — format and uniqueness
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenClaimsIsNull() {
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", null, null)), null)));
    }

    @Test
    void shouldPass_whenClaimsHaveNullIdsAndNoClaimSets() {
        // ids are optional when claim_sets is absent
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("given_name"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldPass_whenClaimIdContainsAlphanumericUnderscoreHyphen() {
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto("claim_1-A", List.of("name"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldFail_whenClaimIdContainsSpecialChar() {
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto("claim@1", List.of("name"), null));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_ID_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimIdContainsSpace() {
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto("claim 1", List.of("name"), null));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_ID_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimSetsRequireIdsButClaimIdIsNull() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("given_name"), null));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, List.of(List.of("claim1")))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_MISSING_CLAIM_ID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimIdsAreDuplicated() {
        List<ClaimQueryDto> claims = List.of(
                claim("c1", "given_name"),
                claim("c1", "family_name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CLAIM_ID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateClaimSets
    // -------------------------------------------------------------------------

    @Test
    void shouldFail_whenClaimSetsPresent_butClaimsIsNull() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", null, List.of(List.of("c1")))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_INVALID_CLAIM_SET, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimSetReferencesUnknownClaimId() {
        List<ClaimQueryDto> claims = List.of(claim("c1", "given_name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, List.of(List.of("unknown_claim")))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_INVALID_CLAIM_SET, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimSetHasDuplicateClaimIds() {
        List<ClaimQueryDto> claims = List.of(claim("c1", "given_name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, List.of(List.of("c1", "c1")))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CLAIM_ID, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenClaimSetsReferenceValidClaimIds() {
        List<ClaimQueryDto> claims = List.of(
                claim("c1", "given_name"),
                claim("c2", "family_name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims, List.of(List.of("c1"), List.of("c1", "c2")))), null);

        assertDoesNotThrow(() -> validator.validate(query));
    }

    // -------------------------------------------------------------------------
    // validateClaimPaths — path element type validation (query creation time)
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenClaimPathContainsOnlyStrings() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList("given_name", "family_name"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldPass_whenClaimPathContainsWildcard() {
        // null element is a valid wildcard step per DCQL spec
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList(null, "city"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldPass_whenClaimPathContainsNonNegativeInteger() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList("addresses", 0), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldFail_whenClaimPathContainsBlankString() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList("  "), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_PATH_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimPathContainsNegativeInteger() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList(-1), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_PATH_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimPathContainsIntegerOverMaxValue() {
        long tooBig = (long) Integer.MAX_VALUE + 1;
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList(tooBig), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_PATH_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimPathContainsBoolean() {
        // boolean is not a valid path element type per DCQL spec
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, Arrays.asList((Object) true), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_PATH_INVALID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // validateClaimPaths — duplicate path check
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenTwoClaimsHaveDifferentPaths() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("c1", List.of("name"), null),
                new ClaimQueryDto("c2", List.of("email"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldFail_whenTwoClaimsHaveIdenticalPaths() {
        // duplicate paths are rejected when claim_sets is absent
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("c1", List.of("name"), null),
                new ClaimQueryDto("c2", List.of("name"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CLAIM_PATH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenTwoClaimsHaveIdenticalNestedPaths() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("c1", List.of("address", "city"), null),
                new ClaimQueryDto("c2", List.of("address", "city"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_DUPLICATE_CLAIM_PATH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSamePathAppearsInDifferentCredentialQueries() {
        // duplicate paths are only disallowed within a single credential query
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto("c1", List.of("name"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(
                credWithClaims("cred1", claims, null),
                credWithClaims("cred2", claims, null)), null);

        assertDoesNotThrow(() -> validator.validate(query));
    }

    @Test
    void shouldPass_whenTwoClaimsHaveIdenticalPaths_andClaimSetsPresent() {
        // duplicate paths are allowed when claim_sets is present — different claim IDs may
        // alias the same path for use in separate options (e.g. first_name vs given_name)
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("first_name", List.of("name"), null),
                new ClaimQueryDto("given_name", List.of("name"), null));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(credWithClaims("c", claims,
                        List.of(List.of("first_name"), List.of("given_name")))), null);

        assertDoesNotThrow(() -> validator.validate(query));
    }

    // -------------------------------------------------------------------------
    // validateClaimValues — values element type validation (DCQL §6.3)
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenClaimValuesContainStringsIntegersBooleans() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("status"), Arrays.asList("active", 1, true, null)));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldPass_whenClaimValuesContainLong() {
        // Long is accepted alongside Integer per DCQL spec (both are integral)
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("score"), Arrays.asList(9999999999L)));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldPass_whenClaimValuesIsNull() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("given_name"), null));
        assertDoesNotThrow(() -> validator.validate(
                new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null)));
    }

    @Test
    void shouldFail_whenClaimValuesContainsDouble() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("score"), Arrays.asList(1.5)));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_VALUES_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimValuesContainsFloat() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("score"), Arrays.asList(3.14f)));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_VALUES_INVALID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenClaimValuesContainsList() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto(null, List.of("tags"), Arrays.asList((Object) List.of("a", "b"))));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("c", claims, null)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validate(query));

        assertEquals(ErrorCode.DCQL_CLAIM_VALUES_INVALID, ex.getErrorCode());
    }
}
