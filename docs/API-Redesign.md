# API Redesign

Starting with v0.17, `Inji Verify Backend` now returns detailed information for `VC-verification` and `VP-results` endpoints, rather than only a final status.

# Why is API Redesign needed?

## Old Design 

### 1. POST /vc-verification

Sample Request:
```json
{
    "vc": "..."
}
```
Sample Response:
```json
{
  "verificationStatus": "SUCCESS"
}
```

### 2. GET /vp-results/{transactionId}

Sample Response:
```json
{
  "transactionId": "txn_11",
  "vpResultStatus": "SUCCESS",
  "vcResults": [
    {
      "vc": "...",
      "verificationStatus": "SUCCESS"
    }
  ]
}
```

## Limitations

- Only a simple final verification status is returned: `SUCCESS / INVALID / EXPIRED / REVOKED`.

## New Design

- Return additional contextual details explaining why a credential passed or failed verification.

### 1. POST /v2/vc-verification

Sample Request:
```json
{
  "verifiableCredential": "...",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation", "suspension"],
  "includeClaims": true
}
```
Sample Response:
```json
{
  "allChecksSuccessful": true,
  "schemaAndSignatureCheck": { "valid": true, "error": null },
  "expiryCheck": { "valid": true },
  "statusChecks": [
    { "purpose": "revocation", "valid": true, "error": null },
    { "purpose": "suspension", "valid": true, "error": null }
  ],
  "claims": {
    "givenName": "Alice",
    "familyName": "Smith",
    "birthDate": "1992-04-05"
  }
}
```

### 2. POST /v2/vp-results/{transactionId}

Sample Request:
```json
{
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation", "suspension"],
  "includeClaims": true
}
```
Sample Response:
```json
{
  "transactionId": "txn_11",
  "allChecksSuccessful": true,
  "credentialResults": [
    {
      "verifiableCredential": "{...}",
      "allChecksSuccessful": true, 
      "holderProofCheck": { "valid": true, "error": null },
      "schemaAndSignatureCheck": { "valid": true, "error": null },
      "expiryCheck": { "valid": true },
      "statusChecks": [
        { "purpose": "revocation", "valid": true, "error": null },
        { "purpose": "suspension", "valid": true, "error": null },
      ],
      "claims": {
        "givenName": "Alice",
        "familyName": "Smith",
        "birthDate": "1992-04-05"
      }
    },
    {
      "verifiableCredential": "eee...xxx",
      "allChecksSuccessful": false,
      "holderProofCheck": null,
      "schemaAndSignatureCheck": { "valid": true, "error": null },
      "expiryCheck": { "valid": true },
      "statusChecks": [
        { "purpose": "revocation", "valid": true, "error": null },
        { "purpose": "suspension", "valid": false, "error": { "code": "INVALID_INDEX", "message": "Index is invalid" } }
      ],
      "claims": {
        "givenName": "Alice",
        "familyName": "Smith",
        "birthDate": "1992-04-05"
      }
    }
  ]
}
```

## Request Fields Summary

| Field | Description | Default | Behavior |
|-------|------------|---------|----------|
| **verifiableCredential** | The Verifiable Credential (VC) to verify. Its format is determined by the Verify Service and passed to the `vc-verifier`. | — | Required input for verification. |
| **skipStatusChecks** | Controls whether credential status checks should be performed. | `false` | **true**: No status checks are performed. `statusCheckFilters` is ignored. <br> **false**: Status checks supported by the credential are performed. Filters can be applied using `statusCheckFilters`. |
| **statusCheckFilters** | Optional list of specific status checks to perform (e.g., revocation, suspension). | Empty | If **empty** and `skipStatusChecks=false`: perform all status checks supported by the credential. <br> If **non-empty**: only the specified checks are performed, even if the credential supports more. |
| **includeClaims** | Determines whether extracted VC claims should be included in the response along with verification results. | `false` | **true**: Response includes extracted claims. <br> **false**: Only verification and status results are returned. |

## Response Fields Summary

## Common Fields for /v2/vc-verification & /v2/vp-results/{transactionId}

| Field | Description | Behavior / Notes |
|-------|------------|------------------|
| **allChecksSuccessful** | Overall verification result. | `true` only if: <br> • `schemaAndSignatureCheck.valid = true` <br> • `expiryCheck.valid = true` <br> • All `statusChecks[].valid = true` <br><br> `false` if any check fails. |
| **schemaAndSignatureCheck** | Result of schema validation and signature verification. | Indicates whether the credential structure and signature are valid. Contains: <br> • `valid` – `true` if all validations succeed <br> • `error` – `null` if no error, otherwise `{ code, message }` |
| **expiryCheck** | Result of credential expiry validation. | • `valid = false` if credential is expired. |
| **statusChecks** | Array of individual credential status check results. | Each item contains: <br> • `purpose` <br> • `valid` <br> • `error` – `null` or `{ code, message }` |
| **claims** | Extracted credential claims. | Included **only if** `includeClaims = true` in request. <br><br> Format-specific behavior: <br> • **JSON-LD**: All fields from `credentialSubject` <br> • **CWT**: All payload claims (excluding metadata via configuration) <br> • **SD-JWT**: Plain + disclosed claims (excluding metadata via configuration) |

## Fields for /v2/vp-results/{transactionId}

| Field | Description | Behavior / Notes |
|-------|------------|------------------|
| **transactionId** | Transaction ID for the VP flow. | Used to correlate the VP verification with the request/flow. |
| **allChecksSuccessful** | Overall result for the VP. | `true` only if **all checks pass for all credentials**. <br> `false` if any credential fails any check. |
| **credentialResults** | List of verification results for all credentials in the VP. | Contains individual verification details for each credential. |

### Fields inside `credentialResults[]`

| Field | Description | Behavior / Notes |
|-------|------------|------------------|
| **verifiableCredential** | Original credential. | The credential as received in the VP. |
| **allChecksSuccessful** | Overall result for the credential. | `true` only if schema/signature, status checks, and holder proof (if applicable) pass. |
| **schemaAndSignatureCheck** | Schema and signature validation result. | Same structure and behavior as `/v2/vc-verification`. |
| **statusChecks** | Credential status check results. | Same structure and behavior as `/v2/vc-verification`. |
| **holderProofCheck** | Result of holder binding / presentation proof validation. | Behavior depends on credential format (see below). If `valid=false`, then `allChecksSuccessful=false`. If `null`, this check is ignored in overall result. |

### `holderProofCheck` Behavior by Format

| Format | Behavior |
|--------|----------|
| **JSON-LD** | Validates Verifiable Presentation signature. <br> If `acceptVPWithoutHolderProof=true` in `/vp-request` → `holderProofCheck = null`. <br> If verification fails → `{ "valid": false, "error": { code, message } }`. |
| **SD-JWT** | Validates Key Binding JWT (KB-JWT). <br> If validation fails → `{ "valid": false, "error": { code, message } }`. <br> Possible error codes include: <br> `ERR_INVALID_KB_JWT_FORMAT`, `ERR_INVALID_KB_JWT_HEADER`, `ERR_INVALID_KB_JWT_ALG`, `ERR_INVALID_KB_JWT_TYP`, `ERR_INVALID_CNF`, `ERR_INVALID_CNF_TYPE`, `ERR_INVALID_CNF_KID`, `ERR_INVALID_KB_SIGNATURE`, `ERR_INVALID_AUD`, `ERR_INVALID_NONCE`, `ERR_INVALID_KB_JWT_IAT`, `ERR_INVALID_SD_HASH`, `ERR_CODE_MISSING_AUD`, `ERR_CODE_MISSING_NONCE`, `ERR_CODE_MISSING_IAT`, `ERR_CODE_MISSING_SD_HASH`. |
| **CWT** | Holder proof not supported currently. <br> Always returned as `holderProofCheck = null`. |
