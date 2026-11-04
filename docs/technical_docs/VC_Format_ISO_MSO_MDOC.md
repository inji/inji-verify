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

Inji Verify accepts the credential as a **base64url-encoded CBOR** string. That string may encode a bare document (`issuerSigned` / `issuerAuth` + `nameSpaces`) or a presentation with a `documents` array. For a `documents` array, only `documents[0]` is verified and used for claims; all subsequent documents are ignored, matching the verification library's behavior.

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
| Signature | COSE_Sign1 `issuerAuth` inside `issuerSigned` | Algorithm comes from the `issuerAuth` COSE `alg` header, subject to verifier support. The certificate chain is not validated; a valid signature from an untrusted or self-signed certificate can still show as verified. |
| Data-element integrity | MSO digests | Performed by the verification library |
| MSO validity | `validFrom` and `validUntil` | An expired `validUntil` is reported in `expiryCheck`; a future `validFrom` is currently not rejected (see [Limitations](#limitations)) |
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
    "org.iso.18013.5.1": {
      "family_name": "Cooper",
      "given_name": "Gorge",
      "birth_date": "1985-04-29",
      "document_number": "DL2154189532"
    }
  }
}
```

When `includeClaims` is `true`, `claims` contains:

- issuer-signed data elements grouped by namespace and keyed by `elementIdentifier`

The service returns namespace data elements only; it does not read or return `docType` from the document or signed MSO. Each namespace has a nested claim object, so elements with the same identifier remain separate without renaming.
Claims are extracted by decoding the submitted credential string again (`Utils.extractClaims`), separately from the `vc-verifier` verification call.

---

## Integration with Scan and Upload

1. The user scans or uploads a QR that embeds the base64url mdoc.
2. The SDK posts the credential string to `POST /v2/vc-verification`.
3. The backend verifies it as `mso_mdoc` and returns the check outcomes.
4. The UI requests the full verification response, including server-extracted claims.
5. The result screen shows a fixed **Mobile Document** title and the namespace-grouped claims decoded for display (same shape as the backend `claims` map).

See [VC_Verification.md](./VC_Verification.md) for the shared scan and upload flow.

---

## UI Decoding

The UI decodes the credential into a namespace-grouped claim map matching the backend (for example `{ "org.iso.18013.5.1": { "family_name": "..." } }`). It does not read `docType` from the credential; the result screen shows **Mobile Document** as the displayed `docType`. Shape preference matches the library: `documents`, then `issuerSigned`, then `issuerAuth` + `nameSpaces`. When `documents` is present, only the first document is used; later documents are ignored. The result card and expanded view reuse the existing details grid, displaying element identifiers qualified by their namespace.

---

## Limitations

| Topic | Behaviour |
|---|---|
| Signature algorithm | ES256 only |
| OpenID4VP | Not supported for `mso_mdoc` |
| Issuer trust | Not enforced; certificate-chain validation is out of scope until IACA trust-anchor support ships. Even an mdoc signed by a self-signed certificate can show as verified. |
| Multi-document submissions | Only `documents[0]` is verified and used for displayed claims, matching the library; all remaining documents are ignored. |
| Revocation | Not checked; `statusCheck` is `[]` |
| Device check | Not performed on scan and upload |
| Not-yet-valid documents | The verifier checks `validUntil` but currently does not reject an mdoc before its `validFrom` date. |
| Oversized input | Base64url strings longer than 1 MiB are rejected |
