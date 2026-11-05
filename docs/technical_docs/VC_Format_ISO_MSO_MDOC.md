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

An mdoc whose decoded size is larger than `inji.verify.mso-mdoc.max-size` (bytes, default `1048576`; env `INJI_VERIFY_MSO_MDOC_MAX_SIZE`) is still detected as an mdoc, then refused with `Credential exceeds the maximum supported size`. The size is the decoded byte length, not the number of base64url characters.

### Configuration

| Property | Env variable | Default | Meaning |
|---|---|---|---|
| `inji.verify.mso-mdoc.max-size` | `INJI_VERIFY_MSO_MDOC_MAX_SIZE` | `1048576` | Maximum decoded mdoc size in bytes; must be greater than zero, otherwise the service fails to start |
| `inji.verify.mso-mdoc.claims-with-meta-data` | `INJI_VERIFY_MSO_MDOC_CLAIMS_WITH_META_DATA` | empty | Comma-separated `<namespace>.<element>` entries removed from `claims`; each entry removes that element from that namespace only. Namespaces and element identifiers may contain dots, so every split of an entry is tried against the namespaces in the credential. If more than one split matches (for example namespaces `org.iso` and `org.iso.18013.5.1` with elements `18013.5.1.age` and `age`), every matching element is removed. |

---

## Issuer Signature and Expiry

| Check | Source | Notes |
|---|---|---|
| Signature | COSE_Sign1 `issuerAuth` inside `issuerSigned` | ES256 (COSE `alg` -7, P-256), as given by the `issuerAuth` COSE `alg` header. ES256 is the only algorithm tested against real mDLs. The certificate chain is not validated; a valid signature from an untrusted or self-signed certificate can still show as verified. |
| Data-element integrity | MSO digests | Performed by the verification library |
| MSO validity | `validFrom` and `validUntil` | Checked by the verification library, with gaps. `validFrom` is not enforced, so an mdoc with a future `validFrom` passes. An expired `validUntil` currently returns `schemaAndSignatureCheck.valid: false` with `ERR_INVALID_VALID_UNTIL_MSO` and no `expiryCheck`. Both cases await [inji/vc-verifier#300](https://github.com/inji/vc-verifier/issues/300). See [Limitations](#limitations). |
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
- dates as text: `tdate` (tag 0) exactly as sent, keeping its offset (for example `2024-05-03T23:30:00-05:00`; it is not converted to UTC, which could change the date); epoch dates (tag 1) as ISO-8601 UTC instants (for example `2024-05-03T08:00:00Z`); `full-date` (tag 1004) unchanged (for example `1985-04-29`)
- CBOR `null` and `undefined` as `null`
- binary values, including `portrait`, as standard Base64
- decimal fractions (tag 4) as numbers

A namespace that is not an item array, an item without a text `elementIdentifier` or without `elementValue`, a repeated `elementIdentifier` within one namespace, or two namespace keys with the same name (for example `1` and `"1"`) is rejected as an invalid credential.

The service returns namespace data elements only; it does not read or return `docType` from the document or signed MSO. Each namespace has a nested claim object, so elements with the same identifier remain separate without renaming.
Claims are extracted by decoding the submitted credential string again (`Utils.extractClaims`), separately from the `vc-verifier` verification call.

A credential that cannot be decoded is rejected with HTTP 400 and the reason, for example `{ "errorCode": "INVALID_CREDENTIAL", "errorMessage": "mdoc contains no documents" }`. The other messages are `Failed to decode mdoc credential` (corrupt CBOR) and `Credential exceeds the maximum supported size`.

Fixed test samples (valid, shapes B and C, tampered, bad signature, empty `documents`, corrupt CBOR, oversized, expired, not yet valid) are in [`docs/postman_collection/mdl-samples`](../postman_collection/mdl-samples/README.md), with their `validUntil` dates. The Postman collection also has an Inji Certify-issued mDL for interop (valid until 2028-10-14).

---

## Integration with Scan and Upload

1. The user scans or uploads a QR that embeds the base64url mdoc.
2. The SDK posts the credential string to `POST /v2/vc-verification`.
3. The backend verifies it as `mso_mdoc` and returns the check outcomes.
4. The UI requests the verification response with `includeClaims: true`. The result screen does not read its `claims` field.
5. The result screen shows a translated **Mobile Document** title in the result header and namespace-grouped claims decoded locally from the credential. The `inji.verify.mso-mdoc.claims-with-meta-data` setting does not remove entries from this display.

See [VC_Verification.md](./VC_Verification.md) for the shared scan and upload flow.

---

## UI Decoding

The UI decodes the credential into a namespace-grouped claim map matching the backend (for example `{ "org.iso.18013.5.1": { "family_name": "..." } }`). It does not read `docType` from the credential; the result screen shows **Mobile Document** as the displayed `docType`. Shape preference matches the library: `documents`, then `issuerSigned`, then `issuerAuth` + `nameSpaces`. When `documents` is present, only the first document is used; later documents are ignored. The result card and expanded view reuse the existing details grid, displaying element identifiers qualified by their namespace. Dates read the same as the backend: a `tdate` (tag 0) is shown exactly as sent, and an epoch date (tag 1) as an ISO-8601 UTC instant (to millisecond precision). JPEG and PNG `portrait` / `signature_usual_mark` values are shown as images; other image encodings (such as JPEG 2000) are shown as `image (N bytes)`.

---

## Limitations

| Topic | Behaviour |
|---|---|
| Signature algorithm | ES256 only (from the COSE `alg` header); other algorithms are not tested or supported |
| OpenID4VP | Not supported for `mso_mdoc` |
| Issuer trust | Not enforced; certificate-chain validation is out of scope until IACA trust-anchor support ships. Even an mdoc signed by a self-signed certificate can show as verified. |
| Multi-document submissions | Only `documents[0]` is verified and used for displayed claims, matching the library; all remaining documents are ignored. |
| MSO validity | `validFrom` is not enforced. An expired `validUntil` is reported as a signature/schema error, not as `EXPIRED`. Both await vc-verifier#300. |
| Revocation | Not checked; `statusCheck` is `[]` |
| Device check | Not performed on scan and upload |
| Oversized input | Documents larger than `inji.verify.mso-mdoc.max-size` decoded bytes (default 1 MiB) are rejected |
