# API Overview

Full API reference: [Inji Verify API documentation](https://mosip.stoplight.io/docs/inji-verify)

---

## Index

1. [Supported VC Formats](#supported-vc-formats)
2. [VC Verification](#vc-verification)
3. [VC Submission (Server-to-Server)](#vc-submission-server-to-server)
4. [OpenID4VP — VP Request Creation](#openid4vp--vp-request-creation)
5. [VP Result Retrieval](#vp-result-retrieval)
6. [DID](#did)
7. [Session Cookie](#session-cookie)

---

## Supported VC Formats

The `/v2/vc-verification` endpoint auto-detects format from the credential content. The following formats are supported:

| VC Format | Issuer Signature Mechanism | Supported Algorithms | Signature Suites / Proof Types |
|---|---|---|---|
| `ldp_vc` | Linked Data Proof | PS256, RS256, EdDSA (Ed25519), ES256, ES256K | `RsaSignature2018`, `Ed25519Signature2018`, `Ed25519Signature2020`, `EcdsaSecp256k1Signature2019`, `EcdsaSecp256r1Signature2019` |
| `vc+sd-jwt` | X.509 Certificate (`x5c` JWT header) | PS256, RS256, EdDSA (Ed25519), ES256, ES256K | — |
| `dc+sd-jwt` | X.509 Certificate (`x5c` JWT header) | PS256, RS256, EdDSA (Ed25519), ES256, ES256K | — |
| `cwt_vc` (Claim 169) | COSE_Sign1 (CWT — RFC 8392) | ES256, EdDSA | `COSE_Sign1` |

---

## VC Verification

There are two endpoints. Use **v2** for all new integrations.

| | `POST /v2/vc-verification` | `POST /vc-verification` |
|---|---|---|
| Version | v2 (current) | v1 (legacy) |
| Request body | JSON `{ "verifiableCredential": ... }` | Raw credential string or JSON object as body |
| Format detection | Auto-detected from credential content | Determined by `Content-Type` header |
| Response | Detailed per-check result + claims | Simple `verificationStatus` string only |
| Status checks | Controllable via `skipStatusChecks` / `statusCheckFilters` | Not configurable |

### POST /v2/vc-verification (recommended)

Verify a single Verifiable Credential server-side. Accepts JSON-LD, SD-JWT, or CWT credentials. Format is auto-detected.

**Request**
```json
{
  "verifiableCredential": "<credential — JSON object or compact string>",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation"],
  "includeClaims": false
}
```

**Response**
```json
{
  "allChecksSuccessful": true,
  "schemaAndSignatureCheck": { "valid": true, "error": null },
  "expiryCheck": { "valid": true },
  "statusCheck": [{ "purpose": "revocation", "valid": true, "error": null }],
  "claims": { "givenName": "Alice" }
}
```

`claims` is included only when `includeClaims: true`.

### POST /vc-verification (legacy v1)

Accepts the raw credential as the request body. Format is determined by the `Content-Type` header:

| `Content-Type` | Format |
|---|---|
| `application/dc+sd-jwt` | `dc+sd-jwt` |
| `application/vc+sd-jwt` | `vc+sd-jwt` |
| `application/vc+cwt` | `cwt_vc` (Claim 169) |
| anything else | `ldp_vc` |

The `ldp_vc` fallback for unrecognized content types is intentional — it preserves compatibility with older clients that submitted JSON-LD credentials without an explicit `Content-Type` header. New integrations should always set an explicit content type.

**Request**
```http
POST /vc-verification
Content-Type: application/ld+json

{ "@context": [...], "type": ["VerifiableCredential", "..."], ... }
```

**Response** — simplified status only, no per-check detail:
```json
{ "verificationStatus": "SUCCESS" }
```

`verificationStatus` values: `SUCCESS`, `INVALID`, `EXPIRED`, `REVOKED`.

---

## VC Submission (Server-to-Server)

### POST /vc-submission

Submit a VC for server-side storage. Returns a `transactionId` for the Relying Party backend to fetch results later.

**Request**
```json
{
  "vc": "<credential>",
  "transactionId": "optional-existing-txn"
}
```

**Response**
```json
{ "transactionId": "txn_abc123" }
```

**Recommendation:** Use the `GET /vp-result/{transactionId}` to fetch results of VP submission

---

## OpenID4VP — VP Request Creation

There are two endpoints for creating a VP request. They accept the same request body but differ in whether they manage the browser session via a cookie.

| Endpoint | Sets Cookie | Use When |
|---|---|---|
| `POST /v2/vp-session-request` | **Yes** — sets HttpOnly `transaction_id` cookie | Browser-based flows (QR scan, same-device). The Inji Verify SDK and UI use this. Results are fetched with `/vp-session-results` using the cookie. |
| `POST /v2/vp-request` | **No** | Pure server-to-server integrations where the Relying Party backend manages `transactionId` directly and fetches results via `/v2/vp-results/{transactionId}`. |

**Recommendation:** use `/v2/vp-session-request` for all browser-based flows. Use `/v2/vp-request` only when building a backend-to-backend integration that never involves a browser session.

### POST /v2/vp-session-request

Create a VP request and establish a browser session. Used by the Inji Verify SDK for all QR-scan and same-device flows.

**Request**
```json
{
  "clientId": "client123",
  "nonce": "optional-min-16-ascii-chars",
  "transactionId": "optional-reuse-existing",
  "dcqlQuery": {
    "credentials": [{ "id": "age_cred", "format": "ldp_vc", "meta": {} }]
  },
  "responseCodeValidationRequired": false
}
```

`nonce` is optional — if omitted, the backend generates a cryptographically random one. When provided, must be URL-safe ASCII and at least 16 characters.

**`responseCodeValidationRequired`** — set to `true` for same-device web wallet flows. When enabled, the backend generates a single-use `responseCode` on VP submission by Wallet and appends it to to the `redirectUri` as `#response_code=<uuid>`. The SDK reads it from the URL hash and passes it to `/vp-session-results`. Leave `false` (default) for QR scan and deep-link flows.

**Response** — `201 Created`
```json
{
  "transactionId": "txn_abc",
  "requestId": "req_xyz",
  "expiresAt": 1761814010329,
  "authorizationDetails": {
    "responseType": "vp_token",
    "responseMode": "direct_post",
    "issuedAt": 1761810410329,
    "clientId": "client123",
    "dcqlQuery": { "credentials": [{ "...": "..." }] },
    "nonce": "abc123",
    "responseUri": "https://verify.example.com/v1/verify/v2/vp-submission/direct-post",
    "responseCodeValidationRequired": false,
    "acceptVPWithoutHolderProof": false // deprecated, always false
  }
}
```

For DID-based `clientId` (prefix `decentralized_identifier:`) or certificate-based `clientId` (prefix `x509_san_dns:`): `authorizationDetails` is `null` and `requestUri` is populated with the URL the wallet fetches to get the signed JWT. Both prefixes use this same by-reference flow. `decentralized_identifier:` clientIds are accepted as-is (validity is established later, when the wallet resolves the DID document). `x509_san_dns:` clientIds additionally require the DNS name after the prefix to match this deployment's configured identity (`inji.verify.x509-san-dns.host`) — see below for the specific error responses. See `GET /v2/vp-request/{requestId}` below for how the two prefixes differ in the JWT that's ultimately served.

**`x509_san_dns` clientId — request creation errors** (`400 Bad Request`, `{ "errorCode": "invalid_request", "errorMessage": "..." }`). The wire-level `errorCode` is `invalid_request` for both causes below — distinguish them by `errorMessage` text, or by the internal enum constant name if you're grepping server code/logs:

| Cause | Internal enum constant (not the wire `errorCode`) |
|---|---|
| DNS name in `clientId` doesn't match this deployment's configured `inji.verify.x509-san-dns.host` | `CLIENT_ID_HOST_MISMATCH` |
| `inji.vp-submission.base-url` isn't `https` (and isn't a loopback host) | `REQUEST_URI_INSECURE` |
| Keystore has no certificate chain configured (or an empty one) for the signing key | `CLIENT_ID_CERTIFICATE_CHAIN_MISSING` |

All three checks run at request-creation time (`POST /v2/vp-session-request` / `POST /v2/vp-request`), before a `requestUri` is ever issued — they don't apply to `decentralized_identifier` clientIds.

**Response** — `201 Created`
```json
{
    "transactionId": "txn_abc",
    "requestId": "req_xyz",
    "expiresAt": 1782459750046,
    "requestUri": "https://injiverify.dev.mosip.net/v1/verify/v2/vp-request/req_2616beb0-88b0-42d1-89d6-9aa5a2772716"
}
```

Sets `Set-Cookie: transaction_id=<base64>; HttpOnly; Secure; SameSite=None`.

### POST /v2/vp-request

Same request body and response shape as `/v2/vp-session-request` but does **not** set a session cookie. For server-to-server integrations only. Pair with `/v2/vp-results/{transactionId}` to fetch results.

---

### GET /vp-request/{requestId}/status

Long-poll for VP request status. Times out after ~55 seconds (Configurable via `INJI_VP_REQUEST_LONG_POLLING_TIMEOUT`).

**Response**
```json
{ "status": "ACTIVE" }
```

| Status | Meaning |
|---|---|
| `ACTIVE` | Awaiting wallet submission |
| `VP_SUBMITTED` | Wallet has submitted the VP token |
| `EXPIRED` | Request window elapsed |

**Deployment note:** Status notifications rely on an in-memory listener map (`vpRequestStatusListeners` in `VerifiablePresentationRequestServiceImpl`) scoped to a single application instance. If `verify-service` runs with multiple replicas behind a load balancer, a wallet's submission may land on a different instance than the one holding the long-poll connection. The status still resolves correctly (via a DB fallback check), but only after the long-poll timeout elapses instead of immediately. For multi-instance deployments, use sticky sessions or an external pub/sub (e.g. Redis) to restore instant notification.

---

### GET /v2/vp-request/{requestId}

Fetch the signed Authorization Request JWT for by-reference flows (`decentralized_identifier` or `x509_san_dns` client IDs). Called by the wallet, not the verifier UI.

**Response** — `Content-Type: application/oauth-authz-req+jwt`

A signed Ed25519 JWT containing all authorization request parameters. Which header the wallet uses to establish trust depends on the `clientId` prefix used when the request was created:

| `clientId` prefix | JWT header | Wallet trust mechanism |
|---|---|---|
| `decentralized_identifier:` | `kid` — a DID URL fragment | Wallet resolves the DID document (`GET /did.json`) and matches `kid` to a `verificationMethod.id` |
| `x509_san_dns:` | `x5c` — the full leaf-first certificate chain (base64-DER), no `kid` | Wallet validates the embedded certificate chain directly (trust chain + signature) and checks the DNS name in `clientId` against the leaf certificate's Subject Alternative Name |

A deployment can serve both prefixes side by side — which header a given JWT gets is decided per-request from that request's own `clientId`, not a deployment-wide toggle. See [`OpenID4VP-1.0.0.md`](./OpenID4VP-1.0.0.md#authorization-request-embedded-vs-by-reference) for the full request/response shapes, and [`verify-core/README.md`](../../verify-core/README.md) for keystore/config requirements (`inji.keystore.file.path`, `inji.verify.x509-san-dns.host`).

**`x509_san_dns` clientId — JWT signing failures.** These are checked at fetch time (when the wallet calls this endpoint), not at request-creation time, and currently return a generic `500 Internal Server Error` (no structured error body — there's no dedicated exception handler for this failure class yet):

| Cause |
|---|
| Signing certificate has expired or is not yet valid (`notBefore`/`notAfter`) |
| Signing certificate's Subject Alternative Name doesn't include the DNS name from `clientId` |

A missing/empty certificate chain is *not* in this table — it's now caught earlier, at request-creation time, as `CLIENT_ID_CERTIFICATE_CHAIN_MISSING` (see the `400`-level table above). It's still possible in theory for the keystore to lose its certificate chain between request creation and the wallet's fetch, so `getVPRequestJwt` re-checks for a missing/empty chain too and fails the same way as the two causes above (`500`, no structured body) if that happens.

These fetch-time failures are distinct from the `400`-level checks above — those validate the *request* before any `requestUri` is issued, these validate the deployment's *keystore* against what the request already claimed, at the moment the wallet actually asks for the JWT.

---

### POST /v2/vp-submission/direct-post

Wallet submits the Verifiable Presentation. Accepts `application/x-www-form-urlencoded`. Called by the wallet, not the verifier UI.

**Request fields**

| Field | Required | Description |
|---|---|---|
| `state` | Yes | The `requestId` from the VP session |
| `vp_token` | One of | URL-encoded JSON object keyed by DCQL `query_id` |
| `error` | One of | Wallet error code (mutually exclusive with `vp_token`) |
| `error_description` | No | Human-readable error detail |

**Response — cross-device / mobile wallet** (`responseCodeValidationRequired=false`)
```
200 OK
{}
```

**Response — web wallet** (`responseCodeValidationRequired=true`)
```json
{ "redirect_uri": "https://verifier.example.com/#response_code=abc123" }
```

The `response_code` is short-lived, single-use, and cryptographically secure.

---

## VP Result Retrieval

There are three result endpoints. Choose based on how the VP request was created and whether a browser session exists.

| Endpoint | Auth | Use When |
|---|---|---|
| `POST /vp-session-results` | HttpOnly cookie (`transaction_id`) | Browser-based flows that used `/v2/vp-session-request`. The cookie identifies the session. Supports web-wallet `responseCode`. |
| `POST /v2/vp-results/{transactionId}` | None (transactionId in path) | Server-to-server flows that used `/v2/vp-request`. Allows specifying verification options (`skipStatusChecks`, `includeClaims`, etc.). |
| `GET /vp-result/{transactionId}` | None (transactionId in path) | Same as above but simpler — no request body, uses default verification options. |

### POST /vp-session-results

Fetch the VP verification result using the session cookie. Clears the cookie on success.

**Request headers**
```
Cookie: transaction_id=<base64>
```

**Request body**
```json
{
  "responseCode": "abc123",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation"],
  "includeClaims": false
}
```

`responseCode` required only for web-wallet flows (`responseCodeValidationRequired=true`).

**Response**
```json
{
  "transactionId": "txn_abc",
  "allChecksSuccessful": true,
  "credentialResults": [
    {
      "verifiableCredential": "...",
      "allChecksSuccessful": true,
      "schemaAndSignatureCheck": { "valid": true, "error": null },
      "holderProofCheck": { "valid": true, "error": null },
      "expiryCheck": { "valid": true },
      "statusCheck": [{ "purpose": "revocation", "valid": true, "error": null }],
      "claims": { "givenName": "Alice" }
    }
  ]
}
```

### POST /v2/vp-results/{transactionId}

Fetch VP result by transaction ID with verification options. For server-to-server flows (no cookie).

**Request**
```json
{
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation"],
  "includeClaims": true
}
```

**Response** — same shape as `/vp-session-results`.

### GET /vp-result/{transactionId}

Fetch VP result by transaction ID with default verification options. No request body. For simple server-to-server lookups.

Unlike the other result endpoints, this endpoint also handles VCs submitted via `POST /vc-submission` — if no VP request is found for the `transactionId`, it falls back to checking VC submissions and returns the result for that instead.

**Response**
```json
{
  "transactionId": "txn_abc",
  "vpResultStatus": "SUCCESS",
  "vcResults": [
    {
      "vc": "<credential string>",
      "verificationStatus": "SUCCESS"
    }
  ]
}
```

`vpResultStatus`: `SUCCESS` or `FAILED`. `verificationStatus` per VC: `SUCCESS`, `INVALID`, `EXPIRED`, or `REVOKED`.

---

## DID

### GET /did.json

Returns the verifier's DID Web document. The full URL is `{baseUrl}/v1/verify/did.json` (relative to the context path). Wallets resolve this when verifying a `did:web:` client ID.

```json
{
  "id": "did:web:verify.example.com:v1:verify",
  "verificationMethod": [
    {
      "id": "did:web:verify.example.com:v1:verify#key-1",
      "type": "Ed25519VerificationKey2020",
      "publicKeyMultibase": "z..."
    }
  ],
  "authentication": ["did:web:verify.example.com:v1:verify#key-1"],
  "service": [
    {
      "id": "#linkeddomains",
      "type": "LinkedDomains",
      "serviceEndpoint": "https://verify.example.com"
    }
  ]
}
```

> **⚠️ Keystore:** The Ed25519 signing key behind this document comes from
> `inji.keystore.file.path` (`INJI_KEYSTORE_FILE_PATH`) / `inji.keystore.file.pass`
> (`INJI_KEYSTORE_FILE_PASS`). By default this points at the sample keystore bundled inside
> `verify-core` (`classpath:sample-keystore/test.p12`) — a throwaway dev/test key whose private
> key is public (it ships in the published jar). **Any real deployment must override both
> properties with its own privately-held keystore.** Leaving the default in place means anyone can
> forge validly-signed `did:web` VP requests appearing to come from your deployment.

> The keystore must hold an **Ed25519** key — RSA and EC keys aren't supported today. This only
> applies to the key used to *sign* these requests; it's unrelated to the algorithm table above,
> which is about *verifying* incoming VCs/VPs. See
> [`verify-core/README.md`](../../verify-core/README.md) for more.

---

## Session Cookie

| Property | Production | Local |
|---|---|---|
| Name | `transaction_id` | `transaction_id` |
| `HttpOnly` | Yes | Yes |
| `Secure` | Yes | No |
| `SameSite` | `None` | `Lax` |

The cookie is cleared (`Max-Age=0`) after `/vp-session-results` responds successfully.
