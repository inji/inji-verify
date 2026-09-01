package io.inji.verify.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DcqlValidator.validateVpTokenAgainstDcql
 *
 * credential_sets uses OR-of-ANDs logic:
 *   - Each option in options[] is an AND group (all IDs must be present)
 *   - Multiple options represent OR (any one fully satisfied is enough)
 *   - required=true (default): at least one option must be satisfied
 *   - required=false: optional, failure does not invalidate the submission
 */
class DcqlVpTokenValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // SD-JWT strings with correct JWT header typ values.
    // Base64url-decoded header: {"alg":"ES256","typ":"dc+sd-jwt"}
    // _WITH_KB variants include a Key Binding JWT as the last ~-delimited part (a full JWT: header.payload.sig).
    // _NO_KB variants end with a trailing ~ and no KB-JWT (last part is empty).
    // vc+sd-jwt is a valid SD-JWT format (supported alongside dc+sd-jwt).
    // This token is used in ldp_vc format-mismatch tests: when ldp_vc format is expected,
    // any textual string (including a vc+sd-jwt token) fails the isObject() check in DCQL
    // format validation — it is NOT invalid as an SD-JWT type.
    private static final String VC_SD_JWT =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9.payload.signature~disclosure~";
    // dc+sd-jwt, no cnf in payload — used to test missing cnf rejection
    private static final String DC_SD_JWT_NO_KB =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.payload.signature~disclosure~";
    // dc+sd-jwt with cnf claim + KB-JWT — payload = base64url({"cnf":{"kid":"k1"}})
    private static final String DC_SD_JWT_WITH_KB =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOnsia2lkIjoiazEifX0.signature~disclosure~kb-header.kb-payload.kb-sig";
    // dc+sd-jwt with cnf + no KB-JWT — ends with trailing ~ (empty last part)
    private static final String DC_SD_JWT_WITH_CNF_NO_KB =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJjbmYiOnsia2lkIjoiazEifX0.signature~disclosure~";
    // Aliases for clarity
    private static final String DC_SD_JWT_WITH_CNF_AND_KB = DC_SD_JWT_WITH_KB;
    private static final String DC_SD_JWT = DC_SD_JWT_NO_KB;

    private DcqlValidator validator;

    // -------------------------------------------------------------------------
    // Helpers: build CredentialQueryDto entries
    // -------------------------------------------------------------------------

    private static CredentialQueryDto cred(String id) {
        // binding=false: ldpVcToken carries a VerifiableCredential, not a VerifiablePresentation
        return new CredentialQueryDto(id, "ldp_vc", new CredentialMetaDto(null, null), false, false, null, null);
    }

    private static CredentialQueryDto credWithFormat(String id, String format) {
        return new CredentialQueryDto(id, format, new CredentialMetaDto(null, null), true, false, null, null);
    }

    private static CredentialSetQueryDto requiredSet(List<List<String>> options) {
        return new CredentialSetQueryDto(options, true);
    }

    private static CredentialSetQueryDto optionalSet(List<List<String>> options) {
        return new CredentialSetQueryDto(options, false);
    }

    // -------------------------------------------------------------------------
    // Helpers: build vp_token JsonNode
    // -------------------------------------------------------------------------

    /**
     * vp_token where every id carries a valid ldp_vc element:
     * JSON object with type=VerifiableCredential (no holder binding).
     */
    private static JsonNode ldpVcToken(String... ids) {
        ObjectNode node = MAPPER.createObjectNode();
        for (String id : ids) {
            node.putArray(id).addObject().put("type", "VerifiableCredential");
        }
        return node;
    }

    /**
     * vp_token where every id carries a valid vc+sd-jwt element
     * (JWT header typ=vc+sd-jwt, mirrors Utils.isSdJwt).
     */
    private static JsonNode vcSdJwtToken(String... ids) {
        ObjectNode node = MAPPER.createObjectNode();
        for (String id : ids) {
            node.putArray(id).add(VC_SD_JWT);
        }
        return node;
    }

    /**
     * vp_token where every id carries a valid dc+sd-jwt element
     * (JWT header typ=dc+sd-jwt, mirrors Utils.isSdJwt).
     */
    private static JsonNode dcSdJwtToken(String... ids) {
        ObjectNode node = MAPPER.createObjectNode();
        for (String id : ids) {
            node.putArray(id).add(DC_SD_JWT);
        }
        return node;
    }

    /** vp_token with dc+sd-jwt elements that include a Key Binding JWT. */
    private static JsonNode dcSdJwtWithKbToken(String... ids) {
        ObjectNode node = MAPPER.createObjectNode();
        for (String id : ids) {
            node.putArray(id).add(DC_SD_JWT_WITH_KB);
        }
        return node;
    }

    @BeforeEach
    void setUp() {
        validator = new DcqlValidator();
    }

    // -------------------------------------------------------------------------
    // Validation A: unknown credential IDs
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenVpTokenKeysMatchDcqlCredentialIds() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldFail_whenVpTokenContainsUnknownCredentialId() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("unknown_id")));

        assertEquals(ErrorCode.VP_TOKEN_UNKNOWN_CREDENTIAL_ID, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenOneOfMultipleVpTokenKeysIsUnknown() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1"), cred("cred2")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", "unknown_id")));

        assertEquals(ErrorCode.VP_TOKEN_UNKNOWN_CREDENTIAL_ID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B (no credential_sets): all DCQL credentials must be present
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenAllCredentialsPresent_andNoCredentialSets() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1"), cred("cred2")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", "cred2")));
    }

    @Test
    void shouldFail_whenACredentialIsMissing_andNoCredentialSets() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1"), cred("cred2")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_MISSING_CREDENTIAL_ID, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B (with credential_sets): OR logic between options
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenFirstOptionSatisfied_orLogic() {
        // {"credential_sets":[{"options":[["passport_query"],["national_id_query"]]}]}
        // passport_query submitted → first option matches → set satisfied
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(requiredSet(List.of(List.of("passport_query"), List.of("national_id_query"))))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("passport_query")));
    }

    @Test
    void shouldPass_whenSecondOptionSatisfied_orLogic() {
        // {"credential_sets":[{"options":[["passport_query"],["national_id_query"]]}]}
        // national_id_query submitted → second option matches → set satisfied
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(requiredSet(List.of(List.of("passport_query"), List.of("national_id_query"))))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("national_id_query")));
    }

    @Test
    void shouldFail_whenNoOptionSatisfied_orLogic() {
        // {"credential_sets":[{"options":[["passport_query"],["national_id_query"]]}]}
        // neither passport_query nor national_id_query submitted → no option matches → fails
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query"), cred("other_query")),
                List.of(requiredSet(List.of(List.of("passport_query"), List.of("national_id_query"))))
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("other_query")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B (with credential_sets): AND logic within an option
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenAllIdsInAndOptionPresent() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(requiredSet(List.of(List.of("cred1", "cred2"))))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", "cred2")));
    }

    @Test
    void shouldFail_whenOnlyPartialIdsInAndOptionPresent() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(requiredSet(List.of(List.of("cred1", "cred2"))))
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B: OR-of-ANDs
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenThirdOptionSatisfied_orOfAnds() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2"), cred("cred3"), cred("cred4"), cred("cred5")),
                List.of(requiredSet(List.of(
                        List.of("cred1", "cred2"),
                        List.of("cred3"),
                        List.of("cred4", "cred5")
                )))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred3")));
    }

    @Test
    void shouldPass_whenAndOptionFullySatisfied_orOfAnds() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2"), cred("cred3")),
                List.of(requiredSet(List.of(
                        List.of("cred1", "cred2"),
                        List.of("cred3")
                )))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", "cred2")));
    }

    @Test
    void shouldFail_whenAndOptionPartiallyPresent_andNoOtherOptionSatisfied_orOfAnds() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2"), cred("cred3")),
                List.of(requiredSet(List.of(
                        List.of("cred1", "cred2"),
                        List.of("cred3")
                )))
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B: optional credential_sets (required=false)
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenOptionalSetNotSatisfied() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(optionalSet(List.of(List.of("cred2"))))
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldPass_whenRequiredSetSatisfiedAndOptionalSetNot() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2"), cred("cred3")),
                List.of(
                        requiredSet(List.of(List.of("cred1"))),
                        optionalSet(List.of(List.of("cred2")))
                )
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldFail_whenRequiredSetNotSatisfied_evenIfOptionalSetIs() {
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("cred1"), cred("cred2")),
                List.of(
                        requiredSet(List.of(List.of("cred1"))),
                        optionalSet(List.of(List.of("cred2")))
                )
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred2")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation B: AND between multiple credential_sets objects
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenAllMultipleRequiredSetsAreSatisfied() {
        // Both passport and national_id submitted — both required sets satisfied
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(
                        requiredSet(List.of(List.of("passport_query"))),
                        requiredSet(List.of(List.of("national_id_query")))
                )
        );
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("passport_query", "national_id_query")));
    }

    @Test
    void shouldFail_whenFirstRequiredSetMissing() {
        // passport_query is required but not submitted
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(
                        requiredSet(List.of(List.of("passport_query"))),
                        requiredSet(List.of(List.of("national_id_query")))
                )
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("national_id_query")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenSecondRequiredSetMissing() {
        // national_id_query is required but not submitted
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(cred("passport_query"), cred("national_id_query")),
                List.of(
                        requiredSet(List.of(List.of("passport_query"))),
                        requiredSet(List.of(List.of("national_id_query")))
                )
        );

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("passport_query")));

        assertEquals(ErrorCode.VP_TOKEN_DCQL_NOT_SATISFIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation C: credential format must match DCQL declaration
    // Uses same logic as extractDcqlTokens():
    //   ldp_vc    → isLdpVcElement (type field = VerifiablePresentation/VerifiableCredential)
    //   dc+sd-jwt → Utils.isSdJwt (JWT header typ check)
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenLdpVcCredentialHasCorrectType() {
        // cred() uses binding=false so ldpVcToken (VerifiableCredential) passes E
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldFail_whenLdpVcCredentialIsAnSdJwtString() {
        // DCQL declares ldp_vc but wallet submits an SD-JWT string
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("cred1", "ldp_vc")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, vcSdJwtToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenLdpVcCredentialIsJsonObjectWithoutTypeField() {
        // JSON object but no "type" field — not a valid VerifiablePresentation/VerifiableCredential
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("cred1", "ldp_vc")), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").addObject().put("proof", "some-proof");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenDcSdJwtCredentialIsStringWithoutValidJwtHeader() {
        // String present but JWT header does not contain a valid SD-JWT typ
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("cred1", "dc+sd-jwt")), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add("not.a.valid-sdjwt~");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDcSdJwtCredentialHasCorrectJwtHeader() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("cred1", "dc+sd-jwt")), null);
        // binding=true in credWithFormat so use a token that carries a KB-JWT
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, dcSdJwtWithKbToken("cred1")));
    }

    @Test
    void shouldFail_whenDcSdJwtCredentialIsAJsonObject() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithFormat("cred1", "dc+sd-jwt")), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_CREDENTIAL_FORMAT_MISMATCH, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation D: multiple=false (default) → array must have exactly one element
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenMultipleFalseAndSingleCredentialSubmitted() {
        // multiple=false (default): single element is allowed
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldFail_whenMultipleFalseAndMoreThanOneCredentialSubmitted() {
        // multiple=false (default): two elements in the array → rejected
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1")
                .addObject().put("type", "VerifiablePresentation");
        ((com.fasterxml.jackson.databind.node.ArrayNode) token.get("cred1"))
                .addObject().put("type", "VerifiablePresentation");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_MULTIPLE_CREDENTIALS_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenMultipleTrueAndMoreThanOneCredentialSubmitted() {
        // multiple=true: multiple elements in the array are allowed
        CredentialQueryDto credWithMultiple =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), true, true, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithMultiple), null);

        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1")
                .addObject().put("type", "VerifiablePresentation");
        ((com.fasterxml.jackson.databind.node.ArrayNode) token.get("cred1"))
                .addObject().put("type", "VerifiablePresentation");

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, token));
    }

    // -------------------------------------------------------------------------
    // Validation E: require_cryptographic_holder_binding type check for ldp_vc
    //   true  (default) → all elements must be VerifiablePresentation
    //   false           → all elements must be VerifiableCredential
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenBindingRequiredAndAllElementsAreVerifiablePresentation() {
        // require_cryptographic_holder_binding=true: VerifiablePresentation is expected
        CredentialQueryDto credWithBinding =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").addObject().put("type", "VerifiablePresentation");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, token));
    }

    @Test
    void shouldFail_whenBindingRequiredAndElementIsVerifiableCredential() {
        // require_cryptographic_holder_binding=true but wallet submits VerifiableCredential
        CredentialQueryDto credWithBinding =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").addObject().put("type", "VerifiableCredential");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_EXPECTED_VERIFIABLE_PRESENTATION, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenBindingNotRequiredAndAllElementsAreVerifiableCredential() {
        // require_cryptographic_holder_binding=false: VerifiableCredential is expected
        CredentialQueryDto credNoBinding =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), false, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credNoBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").addObject().put("type", "VerifiableCredential");

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, token));
    }

    @Test
    void shouldFail_whenBindingNotRequiredAndElementIsVerifiablePresentation() {
        // require_cryptographic_holder_binding=false but wallet submits VerifiablePresentation
        CredentialQueryDto credNoBinding =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), false, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credNoBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").addObject().put("type", "VerifiablePresentation");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_EXPECTED_VERIFIABLE_CREDENTIAL, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenBindingRequired_andSecondElementInArrayIsVerifiableCredential() {
        // multiple=true, require_cryptographic_holder_binding=true
        // first element is VP, second is VC — second should fail
        CredentialQueryDto credMultiple =
                new CredentialQueryDto("cred1", "ldp_vc", new CredentialMetaDto(null, null), true, true, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credMultiple), null);

        ObjectNode token = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode arr = token.putArray("cred1");
        arr.addObject().put("type", "VerifiablePresentation");
        arr.addObject().put("type", "VerifiableCredential"); // second element is wrong type

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_EXPECTED_VERIFIABLE_PRESENTATION, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation E: SD-JWT — require_cryptographic_holder_binding, cnf claim, and Key Binding JWT
    // Per OpenID4VP: when holderBindingRequired=true, the SD-JWT must have BOTH a cnf claim AND a KB-JWT.
    // SD-JWTs without cnf do not support Holder Binding and are rejected before KB-JWT is checked.
    // -------------------------------------------------------------------------

    @Test
    void shouldPass_whenDcSdJwtBindingRequired_andCnfPresentAndKbJwtPresent() {
        CredentialQueryDto credWithBinding =
                new CredentialQueryDto("cred1", "dc+sd-jwt", new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(DC_SD_JWT_WITH_CNF_AND_KB);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, token));
    }

    @Test
    void shouldFail_whenDcSdJwtBindingRequired_andCnfClaimAbsent() {
        // No cnf in payload — SD-JWT does not support Holder Binding at all
        CredentialQueryDto credWithBinding =
                new CredentialQueryDto("cred1", "dc+sd-jwt", new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, dcSdJwtToken("cred1")));

        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_MISSING_CNF, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenDcSdJwtBindingRequired_andCnfPresentButKbJwtAbsent() {
        // cnf present (supports binding) but KB-JWT not included
        CredentialQueryDto credWithBinding =
                new CredentialQueryDto("cred1", "dc+sd-jwt", new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(DC_SD_JWT_WITH_CNF_NO_KB);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_MISSING_KEY_BINDING, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDcSdJwtBindingNotRequired_andCnfAbsent() {
        // holderBindingRequired=false: cnf and KB-JWT are not checked
        CredentialQueryDto credNoBinding =
                new CredentialQueryDto("cred1", "dc+sd-jwt", new CredentialMetaDto(null, null), false, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credNoBinding), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, dcSdJwtToken("cred1")));
    }

    // -------------------------------------------------------------------------
    // Validation F: meta.type_values — OR-of-ANDs against ldp_vc credential types
    // Each inner array is an AND group (all must be present in credential type field).
    // At least one outer option must match (OR).
    // -------------------------------------------------------------------------

    private static CredentialQueryDto credWithTypeValues(String id, List<List<String>> typeValues) {
        // binding=false: token carries VerifiableCredential, not VerifiablePresentation
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC,
                new CredentialMetaDto(null, typeValues), false, false, null, null);
    }

    private static JsonNode ldpVcTokenWithTypes(String id, String... types) {
        ObjectNode token = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode typeArray =
                token.putObject(id).putArray("type");
        typeArray.add("VerifiableCredential");
        for (String type : types) {
            typeArray.add(type);
        }
        // wrap in array per vp_token structure
        ObjectNode wrapper = MAPPER.createObjectNode();
        wrapper.putArray(id).add(token.get(id));
        return wrapper;
    }

    @Test
    void shouldPass_whenTypeValuesNull_noConstraint() {
        // meta.typeValues=null → no type constraint, any credential passes
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldPass_whenCredentialTypesMatchFirstOption() {
        // type_values has two options; credential matches the first
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("AlumniCredential"),
                List.of("UniversityDegreeCredential")
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "AlumniCredential")));
    }

    @Test
    void shouldPass_whenCredentialTypesMatchSecondOption() {
        // credential does not match first option but matches second
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("AlumniCredential"),
                List.of("UniversityDegreeCredential")
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "UniversityDegreeCredential")));
    }

    @Test
    void shouldPass_whenCredentialHasMoreTypesThanRequired() {
        // credential has extra types beyond what the option requires → still passes
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("AlumniCredential")
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "AlumniCredential", "BachelorDegree")));
    }

    @Test
    void shouldFail_whenCredentialTypesMatchNoOption() {
        // credential has no type matching either option
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("AlumniCredential"),
                List.of("UniversityDegreeCredential")
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVcTokenWithTypes("cred1"))); // only VerifiableCredential, no domain type

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenCredentialTypesMatchFullIriOption() {
        // DCQL uses full IRIs with # separator; credential uses relative type names — fragment match must succeed.
        // e.g. "https://example.org/examples#UniversityDegreeCredential" must match "UniversityDegreeCredential".
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/examples#UniversityDegreeCredential"
                )
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "UniversityDegreeCredential")));
    }

    @Test
    void shouldPass_whenCredentialTypesMatchFullIriWithSlashSeparator() {
        // DCQL uses full IRIs with / path separator; credential uses relative type name — path match must succeed.
        // e.g. "https://example.org/types/DriversLicense" must match "DriversLicense".
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/types/DriversLicense"
                )
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "VerifiableCredential", "DriversLicense")));
    }

    @Test
    void shouldFail_whenCredentialTypeMatchesNeitherHashNorSlashSeparator() {
        // Credential has "Other" but query requires "DriversLicense" via / path — must not match.
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("https://example.org/types/DriversLicense")
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVcTokenWithTypes("cred1", "Other")));

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenCredentialTypesMatchOneOfMultipleFullIriOptions() {
        // matches second option (AlumniCredential + BachelorDegree) via IRI fragments
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/examples#UniversityDegreeCredential"
                ),
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/examples#AlumniCredential",
                        "https://example.org/examples#BachelorDegree"
                )
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcTokenWithTypes("cred1", "AlumniCredential", "BachelorDegree")));
    }

    @Test
    void shouldFail_whenCredentialMatchesOnlyPartOfAnAndOption() {
        // option requires BOTH AlumniCredential AND BachelorDegree; credential only has one
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValues("cred1", List.of(
                List.of("AlumniCredential", "BachelorDegree")
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVcTokenWithTypes("cred1", "AlumniCredential"))); // missing BachelorDegree

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    // ---- holder-binding=true: type_values checked against inner VCs, not outer VP ----

    private static CredentialQueryDto credWithTypeValuesAndBinding(String id, List<List<String>> typeValues) {
        // binding=true (default): token carries VerifiablePresentation wrapping the VC(s)
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC,
                new CredentialMetaDto(null, typeValues), true, false, null, null);
    }

    /**
     * Builds a vp_token where the entry for {@code id} is a VerifiablePresentation
     * wrapping one or more inner VCs. Each vararg entry is a comma-separated list of
     * extra types for that inner VC (beyond "VerifiableCredential").
     * e.g. ldpVpTokenWithMultipleInnerVcs("cred1", "TypeA", "TypeB") → VP with two inner VCs.
     */
    private static JsonNode ldpVpTokenWithMultipleInnerVcs(String id, String... innerVcExtraTypes) {
        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        com.fasterxml.jackson.databind.node.ArrayNode vcArr = vp.putArray("verifiableCredential");
        for (String extraType : innerVcExtraTypes) {
            ObjectNode innerVc = MAPPER.createObjectNode();
            innerVc.putArray("type").add("VerifiableCredential").add(extraType);
            vcArr.add(innerVc);
        }
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray(id).add(vp);
        return token;
    }

    /** Convenience: single inner VC with the given extra types. */
    private static JsonNode ldpVpTokenWithInnerTypes(String id, String... innerTypes) {
        // inner VC
        ObjectNode innerVc = MAPPER.createObjectNode();
        com.fasterxml.jackson.databind.node.ArrayNode vcTypeArr = innerVc.putArray("type");
        vcTypeArr.add("VerifiableCredential");
        for (String t : innerTypes) {
            vcTypeArr.add(t);
        }
        // outer VP
        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        vp.putArray("verifiableCredential").add(innerVc);
        // vp_token wrapper
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray(id).add(vp);
        return token;
    }

    @Test
    void shouldPass_whenVpWrapsVcWithMatchingTypeValues() {
        // holder binding=true: wallet submits VP wrapping VC; type_values must match inner VC types
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/credentials#MOSIPVerifiableCredential"
                )
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVpTokenWithInnerTypes("cred1", "MOSIPVerifiableCredential")));
    }

    @Test
    void shouldFail_whenVpWrapsVcWithNonMatchingTypeValues() {
        // inner VC types don't satisfy type_values — must throw even though outer VP type is present
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of(
                        "https://www.w3.org/2018/credentials#VerifiableCredential",
                        "https://example.org/credentials#MOSIPVerifiableCredential"
                )
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVpTokenWithInnerTypes("cred1", "SomeOtherCredential")));

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenVpWrapsMultipleVcsAndAllMatchTypeValues() {
        // VP has two inner VCs; both match type_values — should pass (all must match)
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVpTokenWithMultipleInnerVcs("cred1", "MOSIPVerifiableCredential", "MOSIPVerifiableCredential")));
    }

    @Test
    void shouldFail_whenVpWrapsMultipleVcsAndOnlyOneMatchesTypeValues() {
        // VP has two inner VCs; only one matches — must fail (all must match)
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVpTokenWithMultipleInnerVcs("cred1", "MOSIPVerifiableCredential", "AgeCredential")));

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVpWrapsMultipleVcsAndNoneMatchTypeValues() {
        // VP has two inner VCs; neither matches type_values
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVpTokenWithMultipleInnerVcs("cred1", "TypeA", "TypeB")));

        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation G: meta.vct_values — SD-JWT vct claim must match an allowed value
    // SD-JWT strings below have a real base64url-encoded payload containing the vct claim.
    //   DC_SD_JWT_VCT_MY    → {"vct":"https://example.com/MyCredential"}
    //   DC_SD_JWT_VCT_OTHER → {"vct":"https://example.com/OtherCredential"}
    //   DC_SD_JWT_NO_VCT    → {"sub":"user123"} (no vct claim)
    //   DC_SD_JWT_VCT_MY    → dc+sd-jwt with {"vct":"https://example.com/MyCredential"}
    // -------------------------------------------------------------------------

    private static final String DC_SD_JWT_VCT_MY =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9NeUNyZWRlbnRpYWwifQ.sig~";
    private static final String DC_SD_JWT_VCT_OTHER =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9PdGhlckNyZWRlbnRpYWwifQ.sig~";
    private static final String DC_SD_JWT_NO_VCT =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9.eyJzdWIiOiAidXNlcjEyMyJ9.sig~";

    private static CredentialQueryDto credWithVctValues(String id, List<String> vctValues) {
        // holder binding off so E doesn't reject test SD-JWTs that lack a KB-JWT
        return new CredentialQueryDto(id, Constants.FORMAT_DC_SD_JWT, new CredentialMetaDto(vctValues, null), false, false, null, null);
    }

    private static JsonNode sdJwtToken(String id, String sdJwt) {
        ObjectNode node = MAPPER.createObjectNode();
        node.putArray(id).add(sdJwt);
        return node;
    }

    @Test
    void shouldPass_whenVctValuesNull_noConstraint() {
        // meta.vctValues=null → no vct constraint, any SD-JWT passes
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1", null)), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_MY)));
    }

    @Test
    void shouldPass_whenVctTypeMatchesAllowedValue() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential", "https://example.com/OtherCredential"))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_MY)));
    }

    @Test
    void shouldPass_whenVctTypeMatchesSecondAllowedValue() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential", "https://example.com/OtherCredential"))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_OTHER)));
    }

    @Test
    void shouldFail_whenVctTypeNotInAllowedValues() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential"))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_OTHER)));

        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_VCT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVctTypeAbsent() {
        // payload has no vct claim → must fail when vct_values is set
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential"))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_NO_VCT)));

        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_VCT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDcSdJwtVctTypeMatchesAllowedValue() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential"))), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_MY)));
    }

    @Test
    void shouldFail_whenDcSdJwtVctTypeNotInAllowedValues() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithVctValues("cred1",
                List.of("https://example.com/MyCredential"))), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, sdJwtToken("cred1", DC_SD_JWT_VCT_OTHER)));

        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_VCT_MISMATCH, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation H: ldp_vc claim paths must be present in credentialSubject
    // -------------------------------------------------------------------------

    private static CredentialQueryDto credWithClaims(String id, boolean holderBinding, List<ClaimQueryDto> claims) {
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC,
                new CredentialMetaDto(null, null), holderBinding, false, claims, null);
    }

    /** Builds a ClaimQueryDto with a path from varargs. Use Arrays.asList directly when path contains null. */
    private static ClaimQueryDto claimPath(Object... path) {
        return new ClaimQueryDto(null, Arrays.asList(path), null);
    }

    /** vp_token where the entry for id is a VerifiableCredential with the given credentialSubject. */
    private static JsonNode ldpVcToken(String id, ObjectNode credentialSubject) {
        ObjectNode vc = MAPPER.createObjectNode();
        vc.putArray("type").add("VerifiableCredential");
        vc.set("credentialSubject", credentialSubject);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray(id).add(vc);
        return token;
    }

    /** vp_token where the entry for id is a VerifiablePresentation wrapping one inner VC with the given credentialSubject. */
    private static JsonNode ldpVpTokenWithVc(String id, ObjectNode credentialSubject) {
        ObjectNode innerVc = MAPPER.createObjectNode();
        innerVc.putArray("type").add("VerifiableCredential");
        innerVc.set("credentialSubject", credentialSubject);
        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        vp.putArray("verifiableCredential").add(innerVc);
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray(id).add(vp);
        return token;
    }

    /** vp_token VP wrapping multiple inner VCs, one per credentialSubject. */
    private static JsonNode ldpVpTokenWithMultipleVcs(String id, ObjectNode... subjects) {
        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        com.fasterxml.jackson.databind.node.ArrayNode vcArr = vp.putArray("verifiableCredential");
        for (ObjectNode credentialSubject : subjects) {
            ObjectNode innerVc = MAPPER.createObjectNode();
            innerVc.putArray("type").add("VerifiableCredential");
            innerVc.set("credentialSubject", credentialSubject);
            vcArr.add(innerVc);
        }
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray(id).add(vp);
        return token;
    }

    @Test
    void shouldPass_whenNoClaims_claimValidationSkipped() {
        DCQLQueryDto query = new DCQLQueryDto(List.of(cred("cred1")), null);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1")));
    }

    @Test
    void shouldPass_whenTopLevelClaimPresentInCredentialSubject() {
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", credentialSubject)));
    }

    @Test
    void shouldFail_whenTopLevelClaimAbsentFromCredentialSubject() {
        List<ClaimQueryDto> claims = List.of(claimPath("email"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode().put("name", "Alice"); // "email" absent

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenNestedClaimPathPresent() {
        List<ClaimQueryDto> claims = List.of(claimPath("address", "city"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putObject("address").put("city", "Springfield");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", credentialSubject)));
    }

    @Test
    void shouldFail_whenNestedClaimPathAbsent() {
        List<ClaimQueryDto> claims = List.of(claimPath("address", "country"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putObject("address").put("city", "Springfield"); // "country" absent

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenWildcardStepAppliedToObject() {
        // Per DCQL §7.1.1: null step requires the current node to be an array; applying to an object is an error.
        // credentialSubject is an object, so [null, "city"] must abort with claim-not-found.
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto(null, Arrays.asList(null, "city"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putObject("address").put("city", "Springfield");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenWildcardStepSelectsFromArray() {
        // null step on an array: path = ["items", null, "city"] selects "city" from every element of the "items" array
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto(null, Arrays.asList("items", null, "city"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putArray("items")
                .addObject().put("city", "Springfield");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", credentialSubject)));
    }

    @Test
    void shouldFail_whenWildcardStepFindsNoMatchingKey() {
        // path = ["items", null, "country"] — array elements have "city" not "country" → empty selection
        List<ClaimQueryDto> claims = List.of(new ClaimQueryDto(null, Arrays.asList("items", null, "country"), null));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putArray("items")
                .addObject().put("city", "Springfield");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenIntegerIndexReachesArrayElement() {
        // path = ["emails", 0] — index 0 of the "emails" array
        List<ClaimQueryDto> claims = List.of(claimPath("emails", 0));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putArray("emails").add("alice@example.com");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", credentialSubject)));
    }

    @Test
    void shouldFail_whenIntegerIndexIsOutOfBounds() {
        // path = ["emails", 5] — array has only one element, index 5 is out of bounds
        List<ClaimQueryDto> claims = List.of(claimPath("emails", 5));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode();
        credentialSubject.putArray("emails").add("alice@example.com");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    // ---- holder-binding=true: paths checked against inner VC credentialSubject ----

    @Test
    void shouldPass_whenClaimPresentInInnerVcSubject_holderBindingRequired() {
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", true, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVpTokenWithVc("cred1", credentialSubject)));
    }

    @Test
    void shouldFail_whenClaimAbsentFromInnerVcSubject_holderBindingRequired() {
        List<ClaimQueryDto> claims = List.of(claimPath("email"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", true, claims)), null);

        ObjectNode credentialSubject = MAPPER.createObjectNode().put("name", "Alice"); // "email" absent

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVpTokenWithVc("cred1", credentialSubject)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenAllInnerVcsContainRequiredClaim() {
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", true, claims)), null);

        ObjectNode sub1 = MAPPER.createObjectNode().put("name", "Alice");
        ObjectNode sub2 = MAPPER.createObjectNode().put("name", "Bob");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVpTokenWithMultipleVcs("cred1", sub1, sub2)));
    }

    @Test
    void shouldFail_whenSecondInnerVcMissingRequiredClaim() {
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", true, claims)), null);

        ObjectNode sub1 = MAPPER.createObjectNode().put("name", "Alice");
        ObjectNode sub2 = MAPPER.createObjectNode().put("email", "bob@example.com"); // "name" absent

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        ldpVpTokenWithMultipleVcs("cred1", sub1, sub2)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVpMissingVerifiableCredentialArray_holderBindingRequired() {
        // VP has no "verifiableCredential" array — claim validation must not be silently skipped.
        // Even when typeValues is absent (so validateTypeValues returns early without catching this),
        // validateLdpClaims must throw VP_TOKEN_MISSING_VERIFIABLE_CREDENTIAL.
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", true, claims)), null);

        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        // intentionally no "verifiableCredential" array
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(vp);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));

        assertEquals(ErrorCode.VP_TOKEN_MISSING_VERIFIABLE_CREDENTIAL, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation H: claim value matching
    // -------------------------------------------------------------------------

    private static ClaimQueryDto claimPathWithValues(List<Object> values, Object... path) {
        return new ClaimQueryDto(null, Arrays.asList(path), values);
    }

    @Test
    void shouldPass_whenClaimValuesNull_noValueConstraint() {
        // values=null → only path presence is checked
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldPass_whenClaimValueMatchesDeclaredString() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenClaimValueDoesNotMatchDeclaredString() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Bob");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenClaimValueMatchesOneOfMultipleDeclaredValues() {
        // OR logic — "Bob" is in the declared list
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice", "Bob", "Carol"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Bob");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenClaimValueMatchesNoneOfDeclaredValues() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice", "Carol"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Bob");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenClaimValueMatchesDeclaredBoolean() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(true), "verified"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("verified", true);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldPass_whenClaimValueMatchesDeclaredInteger() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(42), "score"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("score", 42);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenDeclaredStringDoesNotMatchBooleanType() {
        // Per DCQL spec, type must match: declared "true" (String) must NOT match JSON boolean true
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("true"), "verified"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("verified", true);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenDeclaredStringDoesNotMatchIntegerType() {
        // declared "42" (String) must NOT match JSON integer 42
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("42"), "score"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("score", 42);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDeclaredBooleanMatchesBooleanType() {
        // Boolean true declared → JSON boolean true matches
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(true), "verified"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("verified", true);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenDeclaredBooleanTrueDoesNotMatchBooleanFalse() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(true), "verified"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("verified", false);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenClaimValueIsNull_andDeclaredValueIsNull() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(Arrays.asList((Object) null), "middleName"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode();
        subject.putNull("middleName");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenLdpVcClaimAbsent_andValuesConstraintSpecified() {
        // Claims listed in claims[] are always required. If the claim is absent, it is VP_TOKEN_CLAIM_NOT_FOUND
        // regardless of whether values is specified — the presentation doesn't satisfy the DCQL query.
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("age", 30);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // Validation I: SD-JWT claim paths resolved against decoded disclosed claims
    // -------------------------------------------------------------------------

    // SD-JWT strings with proper selective disclosures.
    // Each payload contains _sd_alg + _sd array (SHA-256 hashes of the disclosures).
    // Claims are in the ~disclosure~ parts, not baked into the JWT payload.
    // Only the selectively disclosed claims are checked per DCQL §6.4.1.
    //
    // Disclosure format: BASE64URL(["salt", "claim_name", claim_value])
    // _sd hash:          BASE64URL(SHA-256(disclosure_bytes))

    // {"name": "Alice"}  disclosure=WyJzYWx0MSIsIm5hbWUiLCJBbGljZSJd
    private static final String SD_JWT_NAME_ALICE =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJfd1JuYm9uTU11cktlME5Ud2Y0ZXBJaXB0dVF5VFlBTldiSHBCNmVJYlFFIl19" +
            ".sig~WyJzYWx0MSIsIm5hbWUiLCJBbGljZSJd~";

    // {"name": "Bob"}
    private static final String SD_JWT_NAME_BOB =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJ4TzgxMERLT21KZkdxQXZuWkpfcVA0OFY4VHpERDZrOHZ1RDVhaXhzUEJVIl19" +
            ".sig~WyJzYWx0MiIsIm5hbWUiLCJCb2IiXQ~";

    // {"address": {"city": "Springfield"}}
    private static final String SD_JWT_ADDRESS_CITY =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJTb190cDJ4S1BUZ25HWVI5X3FTUzBwMDJVU1hueDlFLWRuOUlwV0ZVXzBjIl19" +
            ".sig~WyJzYWx0MyIsImFkZHJlc3MiLHsiY2l0eSI6IlNwcmluZ2ZpZWxkIn1d~";

    // Flat dotted disclosure used by Multipaz EU PID: claim name "age_equal_or_over.18" = true
    // Disclosure: WyJzYWx0QWdlMTgiLCJhZ2VfZXF1YWxfb3Jfb3Zlci4xOCIsdHJ1ZV0
    private static final String SD_JWT_AGE_OVER_18_DOTTED =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJ2TmtZY3ZvU0k0NTI4Q0Q4VWJNT0pYdGhtU09IWXk1V09aR1hBbjNXZ1BNIl19" +
            ".sig~WyJzYWx0QWdlMTgiLCJhZ2VfZXF1YWxfb3Jfb3Zlci4xOCIsdHJ1ZV0~";

    // Nested Multipaz-style age claim: {"age_equal_or_over":{"18":true}}
    // Disclosure: WyJzYWx0QWdlT2JqIiwiYWdlX2VxdWFsX29yX292ZXIiLHsiMTgiOnRydWV9XQ
    private static final String SD_JWT_AGE_OVER_18_NESTED =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJlVllWZ3BZc0pCSFR4X1FmS0FMYmtEZFIxRndobDdkaFhmWlRtZDFHVXEwIl19" +
            ".sig~WyJzYWx0QWdlT2JqIiwiYWdlX2VxdWFsX29yX292ZXIiLHsiMTgiOnRydWV9XQ~";

    // {"emails": ["alice@example.com", "bob@example.com"]}
    private static final String SD_JWT_EMAILS_ARRAY =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJCSWp5SDJjNVM0azRjVWFEZjdKdlNoS1BCUm5xN3ZtQXczUkFVX2puanA0Il19" +
            ".sig~WyJzYWx0NCIsImVtYWlscyIsWyJhbGljZUBleGFtcGxlLmNvbSIsImJvYkBleGFtcGxlLmNvbSJdXQ~";

    // {"score": 42}
    private static final String SD_JWT_SCORE_42 =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJxQ2NEbUZOZmpCSTZDRnhpbFhPY3VKQkV6RXA3OUdBWWdsODNrRHNfU3E0Il19" +
            ".sig~WyJzYWx0NSIsInNjb3JlIiw0Ml0~";

    // No disclosures — payload is {"_sd_alg":"sha-256","_sd":[]}, no regular claims and no selectively disclosed claims
    private static final String SD_JWT_NO_DISCLOSURES =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6W119" +
            ".sig~";

    // Non-SD payload claim — payload is {"iss":"https://issuer.example","_sd_alg":"sha-256","_sd":[]}, no disclosures
    // "iss" is a regular (non-selectively-disclosed) JWT payload claim
    private static final String SD_JWT_ISS_ONLY =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJpc3MiOiJodHRwczovL2lzc3Vlci5leGFtcGxlIiwiX3NkX2FsZyI6InNoYS0yNTYiLCJfc2QiOltdfQ" +
            ".sig~";

    // dc+sd-jwt with {"name": "Alice"} disclosure
    private static final String DC_SD_JWT_NAME_ALICE =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJfd1JuYm9uTU11cktlME5Ud2Y0ZXBJaXB0dVF5VFlBTldiSHBCNmVJYlFFIl19" +
            ".sig~WyJzYWx0MSIsIm5hbWUiLCJBbGljZSJd~";

    private static CredentialQueryDto sdJwtCredentialQueryWithClaims(String id, List<ClaimQueryDto> claims) {
        // holder binding off: no KB-JWT required
        return new CredentialQueryDto(id, Constants.FORMAT_DC_SD_JWT, new CredentialMetaDto(null, null), false, false, claims, null);
    }

    @Test
    void shouldPass_whenSdJwtTopLevelClaimPresent() {
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldFail_whenSdJwtTopLevelClaimAbsent() {
        List<ClaimQueryDto> claims = List.of(claimPath("email")); // "email" not in payload
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NAME_ALICE)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtNestedClaimPathPresent() {
        List<ClaimQueryDto> claims = List.of(claimPath("address", "city"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_ADDRESS_CITY)));
    }

    @Test
    void shouldPass_whenSdJwtDottedClaimNameMatchesNestedDcqlPath() {
        // Multipaz discloses age as claim name "age_equal_or_over.18"; DCQL path is ["age_equal_or_over","18"]
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("age_over_18", List.of("age_equal_or_over", "18"), List.of(true)));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_AGE_OVER_18_DOTTED)));
    }

    @Test
    void shouldPass_whenSdJwtNestedAgeClaimPresent() {
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("age_over_18", List.of("age_equal_or_over", "18"), List.of(true)));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_AGE_OVER_18_NESTED)));
    }

    @Test
    void shouldPass_whenSdJwtNestedAgeClaimPathUsesIntegerStep() {
        // Nested object {"age_equal_or_over":{"18":true}} with path step Integer 18
        // (Jackson deserializes unquoted 18 in DCQL JSON as Integer → array-index branch).
        List<ClaimQueryDto> claims = List.of(
                new ClaimQueryDto("age_over_18", List.of("age_equal_or_over", 18), List.of(true)));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_AGE_OVER_18_NESTED)));
    }

    @Test
    void shouldFail_whenSdJwtNestedClaimPathAbsent() {
        List<ClaimQueryDto> claims = List.of(claimPath("address", "country")); // "country" not in address
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_ADDRESS_CITY)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtArrayIndexClaimPresent() {
        List<ClaimQueryDto> claims = List.of(claimPath("emails", 0));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_EMAILS_ARRAY)));
    }

    @Test
    void shouldFail_whenSdJwtArrayIndexOutOfBounds() {
        List<ClaimQueryDto> claims = List.of(claimPath("emails", 5)); // only 2 elements
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_EMAILS_ARRAY)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtClaimValueMatchesDeclaredString() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Alice"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldFail_whenSdJwtClaimValueDoesNotMatchDeclaredString() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("Carol"), "name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenSdJwtClaimAbsent_andValuesConstraintSpecified() {
        // Claims listed in claims[] are always required — absence is VP_TOKEN_CLAIM_NOT_FOUND
        // regardless of whether values is specified.
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("carol@example.com"), "email"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        // SD_JWT_NAME_ALICE has "name" disclosed but no "email" disclosure
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtClaimValueMatchesDeclaredInteger() {
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(42), "score"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_SCORE_42)));
    }

    @Test
    void shouldPass_whenSdJwtNoClaims_andNoDisclosures() {
        // Per DCQL spec: claims absent → wallet must return only mandatory claims (no disclosures).
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", null)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NO_DISCLOSURES)));
    }

    @Test
    void shouldPass_whenSdJwtNoClaimsButDisclosuresPresent() {
        // No claims declared — extra disclosures from the wallet are ignored.
        // The DCQL spec places a MUST on the wallet not to over-disclose, but per OpenID4VP §6.4
        // the verifier MUST NOT rely on the wallet to enforce constraints. We use what we need
        // and ignore the rest, consistent with how we handle extra disclosures when claims are declared.
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", null)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldFail_whenClaimAbsentFromBothPayloadAndDisclosures() {
        // SD_JWT_NO_DISCLOSURES payload is {"_sd_alg":"sha-256","_sd":[]} — "name" is absent
        // from both the JWT payload and the selective disclosures, so validation must fail.
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NO_DISCLOSURES)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenNonSdPayloadClaimMatchesQuery() {
        // SD_JWT_ISS_ONLY has "iss" as a regular (non-selectively-disclosed) JWT payload claim.
        // Claim matching now covers both SD disclosures and regular payload claims, so "iss" must be found.
        List<ClaimQueryDto> claims = List.of(claimPath("iss"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_ISS_ONLY)));
    }

    @Test
    void shouldPass_whenNonSdPayloadClaimValueMatchesQuery() {
        // "iss" is a regular payload claim with value "https://issuer.example" — value match must succeed.
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("https://issuer.example"), "iss"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_ISS_ONLY)));
    }

    @Test
    void shouldFail_whenNonSdPayloadClaimValueMismatch() {
        // "iss" is present but value doesn't match the declared constraint.
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of("https://other.example"), "iss"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_ISS_ONLY)));

        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenDcSdJwtTopLevelClaimPresent() {
        // same path validation applies to dc+sd-jwt format
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredentialQueryWithClaims("cred1", claims)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", DC_SD_JWT_NAME_ALICE)));
    }

    // -------------------------------------------------------------------------
    // Validation J: claim_sets — OR-of-ANDs over claim IDs within a single credential
    // claim_sets references claim IDs from claims[]; at least one option (inner array)
    // must be fully satisfied (all claims present + values matching).
    // Tests cover both ldp_vc and dc+sd-jwt formats.
    // -------------------------------------------------------------------------

    /** Builds a ClaimQueryDto with an ID, path, and optional values. */
    private static ClaimQueryDto namedClaim(String id, List<Object> values, Object... path) {
        return new ClaimQueryDto(id, Arrays.asList(path), values);
    }

    private static CredentialQueryDto ldpVcCredWithClaimSets(String id,
                                                               List<ClaimQueryDto> claims,
                                                               List<List<String>> claimSets) {
        return new CredentialQueryDto(id, Constants.FORMAT_LDP_VC,
                new CredentialMetaDto(null, null), false, false, claims, claimSets);
    }

    private static CredentialQueryDto sdJwtCredWithClaimSets(String id,
                                                               List<ClaimQueryDto> claims,
                                                               List<List<String>> claimSets) {
        return new CredentialQueryDto(id, Constants.FORMAT_DC_SD_JWT,
                new CredentialMetaDto(null, null), false, false, claims, claimSets);
    }

    // ---- ldp_vc ----

    @Test
    void shouldPass_whenLdpVcSatisfiesFirstClaimSetOption() {
        // claim_sets: [[a(name)], [b(score)]]  — credential has name → option 1 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldPass_whenLdpVcSatisfiesSecondClaimSetOption() {
        // claim_sets: [[a(name)], [b(score)]]  — credential has score → option 2 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("score", 42);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenLdpVcSatisfiesNoClaimSetOption() {
        // credential has neither name nor score → no option satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("email", "alice@example.com");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenLdpVcSatisfiesAndOptionWithMultipleClaims() {
        // claim_sets: [[a(name), b(score)], [c(email)]]  — credential has name+score → option 1 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"),
                namedClaim("c", null, "email"));
        List<List<String>> claimSets = List.of(List.of("a", "b"), List.of("c"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice").put("score", 42);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenLdpVcPartiallyMatchesAndOption() {
        // claim_sets: [[a(name), b(score)], [c(email)]]  — credential has only name (not score) and no email
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"),
                namedClaim("c", null, "email"));
        List<List<String>> claimSets = List.of(List.of("a", "b"), List.of("c"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice"); // score absent

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenLdpVcClaimSetOptionSatisfiedWithValueMatch() {
        // claim_sets: [[a(name="Alice")], [b(score=42)]]  — name=Alice → option 1 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Alice"), "name"),
                namedClaim("b", List.of(42), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice");
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenLdpVcClaimPresentButValueMismatchInAllOptions() {
        // claim_sets: [[a(name="Alice")], [b(score=42)]]
        // name=Bob (wrong value) and score absent → no option satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Alice"), "name"),
                namedClaim("b", List.of(42), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Bob");

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenLdpVcFirstOptionValueMismatch_butSecondOptionSatisfied() {
        // claim_sets: [[a(name="Alice")], [b(score=42)]]
        // name=Bob (option 1 fails value) but score=42 (option 2 passes)
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Alice"), "name"),
                namedClaim("b", List.of(42), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Bob").put("score", 42);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    // ---- dc+sd-jwt ----

    @Test
    void shouldPass_whenSdJwtSatisfiesFirstClaimSetOption() {
        // claim_sets: [[a(name)], [b(score)]]  — SD-JWT discloses name → option 1 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldPass_whenSdJwtSatisfiesSecondClaimSetOption() {
        // SD-JWT discloses score=42 → option 2 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_SCORE_42)));
    }

    @Test
    void shouldFail_whenSdJwtSatisfiesNoClaimSetOption() {
        // SD-JWT has no disclosures → neither name nor score present → no option satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name"),
                namedClaim("b", null, "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NO_DISCLOSURES)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtClaimSetOptionSatisfiedWithValueMatch() {
        // claim_sets: [[a(name="Alice")], [b(score=42)]]  — name=Alice → option 1 satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Alice"), "name"),
                namedClaim("b", List.of(42), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldFail_whenSdJwtClaimPresentButValueMismatchInAllOptions() {
        // claim_sets: [[a(name="Carol")], [b(score=99)]]
        // name=Alice (wrong value), score=42 (wrong value) → no option satisfied
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Carol"), "name"),
                namedClaim("b", List.of(99), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        // SD_JWT_NAME_ALICE discloses name=Alice (not Carol) → option 1 fails value
        // SD_JWT_SCORE_42 would be needed for option 2 but we only have one SD-JWT here;
        // use SD_JWT_NAME_ALICE which has no score disclosure → option 2 also fails
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", SD_JWT_NAME_ALICE)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    @Test
    void shouldPass_whenSdJwtFirstOptionValueMismatch_butSecondOptionSatisfied() {
        // claim_sets: [[a(name="Carol")], [b(score=42)]]
        // SD-JWT discloses score=42 → option 2 satisfied even though option 1 would fail value
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", List.of("Carol"), "name"),
                namedClaim("b", List.of(42), "score"));
        List<List<String>> claimSets = List.of(List.of("a"), List.of("b"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(sdJwtCredWithClaimSets("cred1", claims, claimSets)), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", SD_JWT_SCORE_42)));
    }

    // -------------------------------------------------------------------------
    // Additional branch-coverage tests
    // -------------------------------------------------------------------------

    // vc+sd-jwt format — same as dc+sd-jwt but different header typ
    // Header: {"alg":"ES256","typ":"vc+sd-jwt"} = eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9
    // Same payload+disclosures as SD_JWT_NAME_ALICE but with vc+sd-jwt typ
    private static final String VC_SD_JWT_NAME_ALICE =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9" +
            ".eyJfc2RfYWxnIjoic2hhLTI1NiIsIl9zZCI6WyJfd1JuYm9uTU11cktlME5Ud2Y0ZXBJaXB0dVF5VFlBTldiSHBCNmVJYlFFIl19" +
            ".sig~WyJzYWx0MSIsIm5hbWUiLCJBbGljZSJd~";
    // vc+sd-jwt token with vct="https://example.com/MyCredential"
    // Header: {"alg":"ES256","typ":"vc+sd-jwt"} = eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9
    // Payload: {"vct": "https://example.com/MyCredential"} = eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9NeUNyZWRlbnRpYWwifQ
    private static final String VC_SD_JWT_VCT_MY =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6InZjK3NkLWp3dCJ9" +
            ".eyJ2Y3QiOiAiaHR0cHM6Ly9leGFtcGxlLmNvbS9NeUNyZWRlbnRpYWwifQ" +
            ".sig~";

    // Validation F (isTypeValuesSatisfied): branches where vcArray is null / not-array / empty
    // These require holderBindingRequired=true AND typeValues set so the code enters isTypeValuesSatisfied.

    @Test
    void shouldFail_whenVpHasNoVerifiableCredentialArray_andTypeValuesSet() {
        // VP passes holder-binding check (type=VerifiablePresentation) but has no verifiableCredential field.
        // isTypeValuesSatisfied: vcArray == null → return false → VP_TOKEN_META_TYPE_VALUES_MISMATCH
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);

        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation"); // no verifiableCredential field
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(vp);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));
        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVpVerifiableCredentialIsNotArray_andTypeValuesSet() {
        // VP has verifiableCredential as a string (not array).
        // isTypeValuesSatisfied: !vcArray.isArray() → return false → VP_TOKEN_META_TYPE_VALUES_MISMATCH
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);

        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        vp.put("verifiableCredential", "not-an-array"); // string, not array
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(vp);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));
        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVpHasEmptyVerifiableCredentialArray_andTypeValuesSet() {
        // VP has verifiableCredential as an empty array.
        // isTypeValuesSatisfied: vcArray.isEmpty() → return false → VP_TOKEN_META_TYPE_VALUES_MISMATCH
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithTypeValuesAndBinding("cred1", List.of(
                List.of("MOSIPVerifiableCredential")
        ))), null);

        ObjectNode vp = MAPPER.createObjectNode();
        vp.putArray("type").add("VerifiablePresentation");
        vp.putArray("verifiableCredential"); // empty array
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(vp);

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));
        assertEquals(ErrorCode.VP_TOKEN_META_TYPE_VALUES_MISMATCH, ex.getErrorCode());
    }

    // claimValueMatches: Long branch (value instanceof Long && node.isIntegralNumber())

    @Test
    void shouldPass_whenClaimValueMatchesDeclaredLong() {
        // Long declared value must match an integral JSON number (node.asLong() comparison)
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(9999999999L), "bigScore"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("bigScore", 9999999999L);
        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                ldpVcToken("cred1", subject)));
    }

    @Test
    void shouldFail_whenClaimValueDeclaredLongDoesNotMatch() {
        // Long declared value that doesn't equal the JSON number
        List<ClaimQueryDto> claims = List.of(claimPathWithValues(List.of(9999999999L), "bigScore"));
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithClaims("cred1", false, claims)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("bigScore", 1111111111L);
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_VALUE_MISMATCH, ex.getErrorCode());
    }

    // vc+sd-jwt format: hits FORMAT_VC_SD_JWT branch in isSdJwtFormat (used by validateSdJwtClaims / validateVctValues)

    @Test
    void shouldPass_whenVcSdJwtTopLevelClaimPresent() {
        // FORMAT_VC_SD_JWT: same claim-path resolution as dc+sd-jwt (via isSdJwtFormat)
        List<ClaimQueryDto> claims = List.of(claimPath("name"));
        CredentialQueryDto credVcSdJwt = new CredentialQueryDto("cred1", Constants.FORMAT_VC_SD_JWT,
                new CredentialMetaDto(null, null), false, false, claims, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credVcSdJwt), null);

        assertDoesNotThrow(() -> validator.validateVpTokenAgainstDcql(query,
                sdJwtToken("cred1", VC_SD_JWT_NAME_ALICE)));
    }

    @Test
    void shouldFail_whenVcSdJwtVctValuesMismatch() {
        // FORMAT_VC_SD_JWT: validateVctValues path — vct must match one of vct_values
        CredentialQueryDto credVcSdJwt = new CredentialQueryDto("cred1", Constants.FORMAT_VC_SD_JWT,
                new CredentialMetaDto(List.of("https://example.com/WrongCredential"), null), false, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credVcSdJwt), null);

        // VC_SD_JWT_VCT_MY has typ=vc+sd-jwt and vct="https://example.com/MyCredential"
        // — a real vct claim that is not in the allowed list, exercising the vct mismatch path
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query,
                        sdJwtToken("cred1", VC_SD_JWT_VCT_MY)));
        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_VCT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void shouldFail_whenVcSdJwtBindingRequired_andCnfAbsent() {
        // FORMAT_VC_SD_JWT: validateHolderBinding with holderBindingRequired=true and no cnf claim
        CredentialQueryDto credWithBinding = new CredentialQueryDto("cred1", Constants.FORMAT_VC_SD_JWT,
                new CredentialMetaDto(null, null), true, false, null, null);
        DCQLQueryDto query = new DCQLQueryDto(List.of(credWithBinding), null);

        // VC_SD_JWT has vc+sd-jwt typ but no cnf in payload → hasSdJwtCnfClaim returns false
        ObjectNode token = MAPPER.createObjectNode();
        token.putArray("cred1").add(VC_SD_JWT);
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, token));
        assertEquals(ErrorCode.VP_TOKEN_SD_JWT_MISSING_CNF, ex.getErrorCode());
    }

    // describeOptionFailure: catch block for VPRequestValidationException (path type mismatch)

    @Test
    void shouldFail_whenPathTypeMismatch_inClaimSetOption() {
        // Path ["name", "first"] where credentialSubject.name is a String (not object).
        // resolvePath throws VPRequestValidationException (string step on non-object) →
        // describeOptionFailure catches it and returns "path type mismatch" description →
        // all options fail → VP_TOKEN_CLAIM_SETS_NOT_SATISFIED
        List<ClaimQueryDto> claims = List.of(
                namedClaim("a", null, "name", "first")); // "name" is a string, can't navigate into it
        List<List<String>> claimSets = List.of(List.of("a"));
        DCQLQueryDto query = new DCQLQueryDto(
                List.of(ldpVcCredWithClaimSets("cred1", claims, claimSets)), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice"); // "name" is a plain string

        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }

    // describeOptionFailure: "unknown claim id" branch (claimById.get(claimId) returns null)

    @Test
    void shouldFail_whenClaimSetReferencesNonExistentClaimId_atSubmissionTime() {
        // claimSets references claim id "z" which is not in claims — describeOptionFailure returns
        // "unknown claim id 'z'" for every option → VP_TOKEN_CLAIM_SETS_NOT_SATISFIED
        // Note: this bypasses query-time validation since DCQL spec allows such checks at submission
        List<ClaimQueryDto> claims = List.of(namedClaim("a", null, "name"));
        // Directly use the validator bypassing query-level validateClaimSets
        // by building a raw CredentialQueryDto with mismatched claimSets
        CredentialQueryDto credMismatch = new CredentialQueryDto("cred1", Constants.FORMAT_LDP_VC,
                new CredentialMetaDto(null, null), false, false, claims,
                List.of(List.of("z"))); // "z" is not in claims
        DCQLQueryDto query = new DCQLQueryDto(List.of(credMismatch), null);

        ObjectNode subject = MAPPER.createObjectNode().put("name", "Alice");
        VPRequestValidationException ex = assertThrows(VPRequestValidationException.class,
                () -> validator.validateVpTokenAgainstDcql(query, ldpVcToken("cred1", subject)));
        assertEquals(ErrorCode.VP_TOKEN_CLAIM_SETS_NOT_SATISFIED, ex.getErrorCode());
    }
}
