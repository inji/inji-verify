# VC Format: IETF SD-JWT VC

Inji Verify supports **Selective Disclosure JSON Web Tokens (SD-JWT VC)** in both current and legacy variants.

| Format identifier | Specification | Status |
|---|---|---|
| `dc+sd-jwt` | [SD-JWT VC draft-10](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-sd-jwt-vc-10) | Current |
| `vc+sd-jwt` | [SD-JWT VC draft-04](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-sd-jwt-vc-04) | Legacy — accepted for backward compatibility |

Both formats are processed identically by the backend.

---

## Index

1. [What is SD-JWT?](#what-is-sd-jwt)
2. [Issuer Signature](#issuer-signature)
   - [Supported Algorithms](#supported-algorithms)
   - [Key Resolution](#key-resolution)
3. [VC Verification API](#vc-verification-api)
4. [Integration with Verification Flows](#integration-with-verification-flows)
5. [DCQL Query for SD-JWT](#dcql-query-for-sd-jwt)
6. [Holder Binding (KB-JWT)](#holder-binding-kb-jwt)
7. [UI Decoding](#ui-decoding)

---

## What is SD-JWT?

An SD-JWT is a JWT-based credential where claims can be **selectively disclosed**. The holder includes only the disclosures they choose to share, while the issuer's signature covers all claims. This enables privacy-preserving presentations without revealing undisclosed data.

A compact SD-JWT string has the form:
```
{issuer-signed JWT}~{disclosure 1}~{disclosure 2}~...~{KB-JWT}
```

- The **issuer-signed JWT** contains hashes of all claims in `_sd` arrays.
- **Disclosures** are base64url-encoded `[salt, claim_name, value]` tuples that reveal individual claims.
- The **KB-JWT** (Key Binding JWT) proves holder possession — the backend validates its `aud` and `nonce` against the VP request, and its `iat` and `sd_hash` against the presented disclosures.

---

## Issuer Signature

SD-JWT VCs use standard **JSON Web Signatures (JWS)**. The issuer signs the JWT using one of the supported algorithms and provides the public key via an X.509 certificate embedded in the JWT header.

### Supported Algorithms

Supported `alg` values: `RS256`, `PS256`, `ES256`, `ES256K`, `EdDSA`.

### Key Resolution

The verifier resolves the issuer's public key from the JWT header. The vc-verifier library resolves the key via the **X.509 certificate chain** provided in the header:

The vc-verifier reads the public key from the `x5c` JWT header claim — the first certificate in the chain is used as the leaf certificate.

| JWT header claim | Description |
|---|---|
| `x5c` | Base64-encoded X.509 certificate chain (leaf cert first) — **required** |

> **Note:** `x5u` (URL-based certificate fetch) and JWT VC Issuer Metadata (`.well-known/jwt-vc-issuer`) are **not currently supported**. Issuers must embed the public key via `x5c` in the JWT header.

### Issuer-Signed JWT Header Example

```json
{
  "alg": "ES256",
  "typ": "dc+sd-jwt",
  "x5c": [
    "<base64-encoded-leaf-certificate>",
    "<base64-encoded-intermediate-certificate>"
  ]
}
```

---

## VC Verification API

Submit an SD-JWT string for server-side verification:

```http
POST /v2/vc-verification
Content-Type: application/json

{
  "verifiableCredential": "<compact SD-JWT string>",
  "includeClaims": true
}
```

The backend verifies the issuer signature and expiry. Credential status (revocation) checks are **not supported** for SD-JWT. It returns:

```json
{
  "allChecksSuccessful": true,
  "schemaAndSignatureCheck": { "valid": true, "error": null },
  "expiryCheck": { "valid": true },
  "claims": { "age": 30 }
}
```

`claims` is populated only when `includeClaims: true` and contains the disclosed claims after SD-JWT decoding.

> **Note:** KB-JWT holder binding is **not** validated at this standalone endpoint (`validateKeyBindingJwt=false`). Holder proof is only checked during OpenID4VP VP submission (`POST /v2/vp-submission/direct-post`), where the VP request `nonce` and `clientId` are available.

---

## Integration with Verification Flows

SD-JWT credentials are used in both scan/upload and OpenID4VP flows. The format-specific handling is transparent to integrators — the SDK and backend detect the format automatically.

**Scan/Upload:** The `QRCodeVerification` component decodes the QR, detects an SD-JWT string, and submits it to `POST /v2/vc-verification`.

**OpenID4VP:** The wallet submits an SD-JWT string as the value in `vp_token` keyed by the DCQL `query_id`. The backend performs full KB-JWT validation (nonce, aud, iat, sd_hash, signature) as part of VP submission. See [OpenID4VP-1.0.0.md](./OpenID4VP-1.0.0.md) for the full flow.

---

## DCQL Query for SD-JWT

When requesting SD-JWT credentials via OpenID4VP, the `dcql_query` uses `dc+sd-jwt` or `vc+sd-jwt` as the format and `vct_values` in `meta` to constrain the credential type:

```json
{
  "credentials": [
    {
      "id": "age_credential",
      "format": "dc+sd-jwt",
      "meta": {
        "vct_values": ["https://credentials.example.com/AgeCredential"]
      },
      "claims": [
        { "path": ["age"] }
      ]
    }
  ]
}
```

---

## Holder Binding (KB-JWT)

When an SD-JWT is submitted via OpenID4VP with `require_cryptographic_holder_binding=true`, the backend performs a full KB-JWT validation.

**Issuer JWT must contain:**

| Claim | Requirement |
|---|---|
| `cnf` | Required. Must contain either `jwk` (inline JWK) or `kid` (DID URI) — not both. |

**KB-JWT header must have:**

| Header | Requirement |
|---|---|
| `typ` | Must be exactly `"kb+jwt"` |
| `alg` | Must be a supported JWS algorithm (RS256, PS256, ES256, ES256K, EdDSA) |

**KB-JWT payload — all four fields are required:**

| Claim | Requirement |
|---|---|
| `aud` | Must match the verifier `clientId` from the VP request |
| `nonce` | Must match the `nonce` from the VP request |
| `iat` | Must be a positive integer (Unix timestamp) |
| `sd_hash` | Base64url-encoded hash of `{credentialJwt}~{disclosure1}~{disclosure2}~...~` using the `_sd_alg` from the issuer JWT (default: `sha-256`). Binds the KB-JWT to the exact set of disclosures presented. |

**KB-JWT signature** is verified against the holder's public key from the `cnf` claim: either directly from `cnf.jwk`, or resolved via DID resolution when `cnf.kid` is a `did:` URI.

---

## UI Decoding

After the backend returns a verification result containing an SD-JWT string, the UI decodes it using `@sd-jwt/decode` to extract and display the **public and disclosed claims**. Undisclosed claims are not shown.

```bash
npm install @sd-jwt/decode@0.8.1-next.0
```

