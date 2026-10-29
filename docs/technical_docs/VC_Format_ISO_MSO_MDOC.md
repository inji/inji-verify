# VC Format: ISO mdoc / mDL (`mso_mdoc`)

Inji Verify supports ISO/IEC 18013-5 mobile documents (mdocs), including the mobile driving licence (mDL), on the **scan and upload** path.

| Format identifier | Specification | Status |
|---|---|---|
| `mso_mdoc` | [ISO/IEC 18013-5](https://www.iso.org/standard/69084.html) | Supported for scan, upload, and `POST /v2/vc-verification` |

> **Out of scope:** OpenID4VP presentation of `mso_mdoc` is not supported in this release. Do not request this format in a DCQL query.

---

## Index

1. [What is an mdoc?](#what-is-an-mdoc)
2. [Detection](#detection)
3. [Issuer Signature and Expiry](#issuer-signature-and-expiry)
4. [VC Verification API](#vc-verification-api)
5. [Integration with Scan and Upload](#integration-with-scan-and-upload)
6. [UI Decoding](#ui-decoding)
7. [Limitations](#limitations)

---

## What is an mdoc?

An mdoc is a CBOR credential. Claims are grouped into named namespaces, and integrity is split across:

- a COSE_Sign1 `issuerAuth` over the Mobile Security Object (MSO)
- a digest in that MSO for every disclosed data element

For an mDL, `docType` is typically `org.iso.18013.5.1.mDL` and the standard namespace is `org.iso.18013.5.1`.

Inji Verify accepts the credential as a **base64url-encoded CBOR** string. That string may encode a bare document (`issuerSigned` / `issuerAuth` + `nameSpaces`) or a presentation with a `documents` array. Only the first document is read today.

---

## Detection

`POST /v2/vc-verification` auto-detects format from the credential content. An mdoc is recognised **before** CWT and JSON-LD:

1. the string is not empty, not JSON (`{` / `[`), and contains no `.` (so it is not an SD-JWT)
2. base64url decoding succeeds
3. the first CBOR item is a map that contains `documents`, `issuerSigned`, or both `issuerAuth` and `nameSpaces`

Strings longer than 1 MiB are rejected as mdocs.

---

## Issuer Signature and Expiry

| Check | Source | Notes |
|---|---|---|
| Signature | COSE_Sign1 `issuerAuth` inside `issuerSigned` | Algorithm comes from the `issuerAuth` COSE `alg` header, subject to verifier support |
| Data-element integrity | MSO digests | Performed by the verification library |
| Expiry | MSO `validityInfo` | Reported in `expiryCheck` |
| Revocation | — | Not supported; `statusCheck` is empty |
| Holder / device binding | — | Not applicable on scan and upload |

---

## VC Verification API

```http
POST /v2/vc-verification
Content-Type: application/json

{
  "verifiableCredential": "<base64url CBOR mdoc>",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation"],
  "includeClaims": true
}
```

Example success response:

```json
{
  "allChecksSuccessful": true,
  "schemaAndSignatureCheck": { "valid": true, "error": null },
  "expiryCheck": { "valid": true },
  "statusCheck": [],
  "claims": {
    "docType": "org.iso.18013.5.1.mDL",
    "family_name": "Cooper",
    "given_name": "Gorge",
    "birth_date": "1985-04-29",
    "document_number": "DL2154189532"
  }
}
```

When `includeClaims` is `true`, `claims` contains:

- `docType` from the document or the MSO
- issuer-signed data elements keyed by `elementIdentifier`

If the same identifier appears in more than one namespace, both values are kept under namespace-qualified keys (for example `org.iso.18013.5.1.family_name`). An element literally named `docType` is always stored under a namespace-qualified key so it cannot overwrite the document type.

---

## Integration with Scan and Upload

1. The user scans or uploads a QR that embeds the base64url mdoc.
2. The SDK posts the credential string to `POST /v2/vc-verification`.
3. The backend verifies it as `mso_mdoc` and returns the check outcomes.
4. With the SDK default `summariseResults=true`, `onVCProcessed` receives `{ verificationStatus }` plus the original credential in `vc`.
5. The result screen decodes `vc` locally and displays the claims.

See [VC_Verification.md](./VC_Verification.md) for the shared scan and upload flow.

---

## UI Decoding

The UI mirrors the backend claim shape. It reads `docType` and flattens issuer-signed items from `nameSpaces`, `issuerSigned.nameSpaces`, or `documents[0].issuerSigned.nameSpaces`. Tag-24 byte strings are unwrapped. CBOR date values render as ISO strings.

This is the same pattern used for SD-JWT (`decodeSdJwtToken`) and CWT (`extractMappedClaim`): claims for display come from the credential string, not from the summarised verification response.

---

## Limitations

| Topic | Behaviour |
|---|---|
| OpenID4VP | Not supported for `mso_mdoc` |
| Trust anchors / certificate chain | Not enforced by Inji Verify in this release; the library chain check remains a stub |
| Multi-document submissions | Only the first document is verified and displayed |
| Revocation / status | Not checked; `statusCheck` is `[]` |
| Device / holder binding | Not checked on scan and upload |
| Oversized input | Base64url strings longer than 1 MiB are rejected |
