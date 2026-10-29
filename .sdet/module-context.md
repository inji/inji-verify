# inji-verify: API test context

Context for SDET work on `api-test/` (API TestRig). Produced by module onboarding with the AIDLC Engineering Agent and reviewed by
the module owner; **verified** on `upstream/develop` @ `110c0529` (2026-10-29). The validator profile for this module
(`validators/profiles/inji-verify.yaml`) lives in the AIDLC Engineering Agent repository, not here.

| Item | Value |
|---|---|
| Suites (with ymlFile tests) | `api-test/testNgXmlFiles/injiverifySuite.xml` (`injiverifyMasterTestSuite.xml` has no ymlFile tests) |
| YAML files / cases | 77 / 354 (354 with uniqueIdentifier) |
| Case-name families | `Injiverify_` (278), `DC_` (74) of 354; deviations: `InjiVerify_` (2, KI-IV-001) |
| uniqueIdentifier pattern | `^TC_(?i:InjiVerify)_[A-Za-z0-9_]+_\d+$` (354/354 match) |
| Duplicate uniqueIdentifiers | 0 |
| Script classes | SimplePostForAutoGenId (23), SimplePostForAutoGenIdForUrlEncoded (15), GetWithParam (12), PostWithBodyAndPathParams (8), SimplePost (7), GetWithParamAndHeader (4), SimplePostForAutoGenIdWithHeaders (4), PostDcApiSubmission (2), SimplePostWithContentType (1), GetDcApiRequestJwt (1) |
| idKeyName values | id, nonce, redirect_uri, requestId, requestUri, response_code, transactionId |
| Unwired YAML | 0 |
| YAML that does not parse | 0 |
| Coverage matrix | none |
| Baseline (all cases, reviewed profile) | 2 blocking (KI-IV-001), 117 warnings (KI-IV-002) |

Existing debt in the baseline is recorded in `known-issues.md`, not fixed: later changes are validated only for what they change.

ID capture, found in this module's own script classes:
- `SimplePostForAutoGenIdWithHeaders` also stores `requestId` and `transactionId`, and stores IDs without a `_sid` check.
- Script classes from apitest-commons were not scanned.

## Rules for test changes

- A test's `uniqueIdentifier` is permanent: never renumber, reuse or recycle one.
- Never weaken a test to make it pass (changed expected output, removed assertion, disabled or deleted test) without the
  reviewer's explicit approval.
