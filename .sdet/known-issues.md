# inji-verify: known issues and baseline debt

Recorded at module onboarding (2026-10-29) so later changes are validated only for what they change. Baseline: all API test cases
checked with the reviewed inji-verify validator profile (AIDLC Engineering Agent) on `upstream/develop` @ `110c0529`:
2 blocking (case-name family), 117 warnings (uniqueIdentifier subject), 0 ID-dependency findings.

| ID | Issue | Status | Evidence | Impact / action |
|---|---|---|---|---|
| KI-IV-001 | 2 cases named `InjiVerify_...` instead of `Injiverify_...` | Verified | `api-test/src/main/resources/injiverify/CreateVpRequestWithSDJWT/CreateVpRequestWithSDJWT.yml:2`, `api-test/src/main/resources/injiverify/VpSubmission/VpSubmission.yml:662` | Style debt. Renaming changes the `$ID` keys derived from the name, so check consumers first. Not fixed during onboarding |
| KI-IV-002 | 117 uniqueIdentifier subjects differ from their YAML root key (63 in EdgeTestCases) | Verified | baseline warnings | Style only |
| KI-IV-003 | `inji-verify-sdk` Jest reports coverage as `html` only | Verified | `inji-verify-sdk/jest.config.js:25` | Diff coverage for SDK changes needs `--coverageReporters=lcov` on the test command |
| KI-IV-004 | Node version not pinned: `AGENTS.md` says Node 18; no `engines` / `.nvmrc`; onboarding ran verify-ui on Node 20 | Verified / Inferred risk | `AGENTS.md` (verify-ui section), `verify-ui/package.json` | Results may differ by Node version; owner to confirm the CI Node version |
| KI-IV-005 | `verify-ui` `prestart` / `prebuild` run the whole Jest suite; `npm test` uses `--updateSnapshot` | Verified | `verify-ui/package.json` scripts | Use `npm run test:ci`, never `npm test` (it rewrites snapshots and would hide a regression) |
| KI-IV-006 | `ui-test/` pom declares Java 1.8 while the services use Java 21 | Verified | `ui-test/pom.xml` | UI automation project with its own build; informational |
