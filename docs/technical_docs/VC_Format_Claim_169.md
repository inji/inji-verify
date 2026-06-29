# VC Format: Claim 169 (CWT)

Claim 169 is a MOSIP-defined, IANA-registered credential format for offline identity verification. Personal identity fields are mapped to numeric claim IDs (CBOR tag 169), CBOR-encoded, and signed as a **COSE Web Token (CWT)**. The entire token is compressed and Base45-encoded for QR code embedding.

- **IANA registration:** [CWT claim key 169 — "identity-data"](https://www.iana.org/assignments/cwt/cwt.xhtml)
- **Specification:** [MOSIP 169 QR Code Specification v1.2.1](https://docs.mosip.io/1.2.0/readme/standards-and-specifications/mosip-standards/169-qr-code-specification)

---

## Index

1. [Why Claim 169?](#why-claim-169)
2. [Claim Attributes](#claim-attributes)
3. [CWT Structure](#cwt-structure)
4. [Issuer Signature](#issuer-signature)
5. [QR Code Encoding Pipeline](#qr-code-encoding-pipeline)
6. [VC Verification API](#vc-verification-api)
7. [Verification Flow](#verification-flow)
8. [Security Considerations](#security-considerations)

---

## Why Claim 169?

Standard JWT/JSON credentials are too large for QR codes. Claim 169 addresses this by:

- **Numeric claim keys** — fields are addressed by integer (e.g., `4` = Full Name) instead of strings, shrinking CBOR payload size significantly.
- **Selective disclosure by issuer** — the issuer encodes only the claims needed for a given use case into each QR code. A single physical card can carry multiple QR codes for different contexts.
- **COSE signatures** — Ed25519/ECC signatures are much smaller than RSA, helping keep the QR scannable.
- **Compact and QR-friendly** — the entire signed token (claims + signature) fits in a single QR code that can be printed on a physical card.

| Use case | Claims shared |
|---|---|
| Age verification (e.g. alcohol purchase) | Age only |
| Address verification (e.g. delivery) | Address only |
| KYC | Name + photo (biometric) |

---

## Claim Attributes

The Claim 169 CBOR map carries two kinds of data: standard **CWT metadata** (processed by the token layer) and **identity attributes** under tag 169 (the Claim 169 payload).

### Standard CWT Attributes

These are part of the CWT envelope, not the Claim 169 map itself. They follow [RFC 8392](https://www.rfc-editor.org/rfc/rfc8392) and the [IANA CWT registry](https://www.iana.org/assignments/cwt/cwt.xhtml).

| Claim key | Type | Name | Description |
|---|---|---|---|
| `1` | `tstr` | Issuer (`iss`) | Fully qualified URI of the issuing authority (e.g. `https://mosip.io`) |
| `2` | `tstr` | Subject (`sub`) | Identifier for the credential subject |
| `4` | `int` | Expiration Time (`exp`) | Unix timestamp — credential must be rejected after this time |
| `5` | `int` | Not Before (`nbf`) | Unix timestamp — credential must not be accepted before this time |
| `6` | `int` | Issued At (`iat`) | Unix timestamp — time the credential was issued |

### Identity Attributes (under tag 169)

All fields are optional. The issuer includes only the subset needed for each QR code. For the full attribute list, see the [MOSIP spec](https://docs.mosip.io/1.2.0/readme/standards-and-specifications/mosip-standards/169-qr-code-specification#id-3.-semantics).

Some key attributes:

| Key | Field name | Example value |
|---|---|---|
| `1` | ID | `"3918592438"` |
| `4` | Full Name | `"Janardhan BS"` |
| `8` | Date of Birth | `"19880102"` (YYYYMMDD) |
| `9` | Gender | `1` = Male, `2` = Female, `3` = Other |
| `10` | Address | `"New House, Near Metro Line\nBengaluru, KA"` |
| `12` | Phone Number | `"+919876543210"` (E.123 format) |
| `13` | Nationality | `"IN"` (ISO 3166-2) |
| `62` | Face (biometric) | `{ 0: <binary>, 1: 0 (Image), 2: 4 (WEBP) }` |

Keys `16` and `17` (Binary Image / Binary Image Format) are **deprecated** — use biometric object `#62` (Face) instead. Keys `50–65` cover other biometrics (fingerprints, iris, palm, voice).

---

## CWT Structure

The signed token follows the `COSE_Sign1` structure ([RFC 8152](https://www.rfc-editor.org/rfc/rfc8152)):

```
COSE_Sign1 = [
  protected_header,      ← algorithm (alg), key ID (kid)
  unprotected_header,    ← optional: kid, x5chain
  CBOR_encoded_payload,  ← CWT claims + tag 169 identity map
  signature              ← COSE signature bytes
]
```

The payload is a CBOR map containing:
- Standard CWT claims (`iss`, `exp`, `nbf`, `iat`)
- The Claim 169 identity map under key `169`

### CBOR Map Example (simplified)

```
iss: "https://mosip.io"
exp: 1787912445
nbf: 1756376445
iat: 1756376445
169:
  1: "3918592438"       # ID
  4: "Janardhan BS"     # Full Name
  8: "19880102"         # Date of Birth
  9: 1                  # Gender: Male
  10: "New House, Near Metro Line, Bengaluru, KA"
  13: "IN"              # Nationality
  62:                   # Face biometric
    - 0: <binary>       # Image data
      1: 0              # Format: Image
      2: 4              # Sub-format: WEBP
```

---

## Issuer Signature

### Supported Algorithms

Claim 169 tokens use **COSE signatures** (not JWT). The issuer signs using one of:

Supported algorithms: `EdDSA` (Ed25519) and `ES256`. Ed25519 is recommended — it produces the smallest signatures, keeping QR codes compact.

### Key Resolution

The verifier resolves the issuer's public key using the **`iss` claim** from the CWT payload and the **`kid`** from the COSE header:

1. Read `iss` (claim key `1`) from the CWT payload
2. Resolve the public key based on the `iss` URI scheme:

| `iss` scheme | Resolution |
|---|---|
| `https://` / `http://` | Fetch `{iss}/.well-known/jwks.json` and select the key matching `kid` |

3. `kid` is read from the COSE protected header first, then the unprotected header

---

## QR Code Encoding Pipeline

The issuer encodes a Claim 169 credential into a QR code through these steps:

```
Identity data (JSON)
       ↓
Map to Claim 169 numeric keys
       ↓
CBOR-encode payload
       ↓
Wrap in CWT (add iss, exp, iat)
       ↓
Sign with COSE_Sign1
       ↓
Compress (zlib or Brotli)
       ↓
Base45-encode
       ↓
Generate QR code
```

**Compression:** Both zlib and Brotli are supported (as of spec v1.2.1). Verifiers detect the format by checking for the zlib magic number (`0x78`) at the start of the compressed bytes. If present → zlib; otherwise → Brotli.

---

## VC Verification API

Submit a decoded CWT for server-side verification. The v2 endpoint accepts JSON and auto-detects the format from the credential content:

```http
POST /v2/vc-verification
Content-Type: application/json

{
  "verifiableCredential": "<CWT hex string>",
  "skipStatusChecks": false,
  "includeClaims": true
}
```

The backend hex-decodes the string, CBOR-decodes it, and passes the CWT to the `vc-verifier` library. It returns:

```json
{
  "allChecksSuccessful": true,
  "schemaAndSignatureCheck": { "valid": true, "error": null },
  "expiryCheck": { "valid": true }
}
```

> Revocation via `credentialStatus` is **not applicable** to Claim 169 credentials. The spec does not define a status mechanism — expiry (`exp`) is the only time-based validity check.

---

## Verification Flow

### 1. Decode QR

The `QRCodeVerification` SDK component decodes the scanned/uploaded QR via [PixelPass](https://github.com/inji/pixelpass):

1. Base45-decode
2. Detect compression format (zlib magic number → zlib; else → Brotli)
3. Decompress to get raw CWT bytes
4. Extract as hex string for API submission

### 2. Verify CWT

The backend runs these steps via `vc-verifier`:

| Step | Operation |
|---|---|
| 1 | `decodeCose()` — parse `COSE_Sign1` structure |
| 2 | `extractCwtClaims()` — extract CBOR-encoded claims |
| 3 | `extractIssuer(iss)` — read issuer from CWT payload |
| 4 | `resolveIssuerMetadata()` — fetch issuer public key metadata |
| 5 | `fetchPublicKeys()` — retrieve issuer public keys |
| 6 | `selectKeyByKid()` — match key by `kid` in COSE header |
| 7 | `verifyCoseSignature()` — validate COSE signature against payload |

### 3. Display Claims

After verification, the UI:
1. Decodes the CWT to extract the raw CBOR payload
2. Extracts the Claim 169 numeric map under tag `169`
3. Passes it to [`PixelPass.decodeMappedData()`](https://github.com/inji/pixelpass) which reverse-maps numeric keys to human-readable field names
4. Displays the mapped claims alongside the verification status

---

## Security Considerations

- The Claim 169 payload is **plain-text CBOR** (not encrypted). Anyone who scans the QR code can read all claims. The COSE signature prevents tampering but does not protect disclosure. Only include claims that are legally permissible to share in the QR context.
- The token **must** be a tagged `COSE_Sign1` (CBOR tag 61, 4-element array). The verifier enforces this — missing tag, wrong array size, or incorrect element types all result in a verification failure.
- The issuer's public key is resolved **dynamically at verification time** from the `iss` claim in the CWT. The `iss` must be an `https://` or `http://` URL — the verifier fetches `{iss}/.well-known/jwks.json` and selects the key by `kid` from the COSE header. The issuer's JWKS endpoint must be reachable at verification time.

---
