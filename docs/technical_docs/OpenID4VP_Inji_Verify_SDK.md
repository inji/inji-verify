# Inji Verify SDK

The SDK is published as `@injistack/react-inji-verify-sdk` and provides two independent React components for embedding credential verification into a Relying Party UI.

| Component | Use case |
|---|---|
| `QRCodeVerification` | QR scan / file upload VC verification |
| `OpenID4VPVerification` | Online sharing via OpenID4VP v1.0 (QR code or wallet redirect) |

---

## Index

1. [Installation](#installation)
2. [QRCodeVerification](#qrcodeVerification)
   - [Props](#props)
   - [QR Code Types Handled](#qr-code-types-handled)
   - [Data-Share Flow](#data-share-online-vc-flow)
   - [Server-to-Server Flow](#server-to-server-flow)
   - [Result Shapes](#result-shapes)
   - [Basic Usage Example](#basic-usage-example)
3. [OpenID4VPVerification](#openid4vpverification)
   - [Props](#props-1)
   - [Supported flows](#supported-flows)
   - [Flow Selection Logic](#flow-selection-logic)
   - [Result Shapes](#result-shapes-1)
   - [Basic Usage Example](#basic-usage-example-1)
4. [`require_cryptographic_holder_binding`](#require_cryptographic_holder_binding)

---

## Installation

```bash
npm install @injistack/react-inji-verify-sdk
```

---

## QRCodeVerification

Handles VC verification via camera scan or file upload. Decodes QR codes using the [PixelPass](https://github.com/inji/pixelpass) library, then calls the Verify Backend to verify the credential.

Also handles **data-share (online VC) QR codes** — QR codes that contain a redirect URL to an Online VC Provider rather than an embedded credential.

### Import

```tsx
import { QRCodeVerification } from "@injistack/react-inji-verify-sdk";
```

### Props

#### Required

| Prop | Type | Description |
|---|---|---|
| `verifyServiceUrl` | `string` | Base URL of the Verify Backend |
| `clientId` | `string` | Client identifier, used in data-share redirect flows |
| `onError` | `(error: Error) => void` | Called on any error |

#### Result callbacks (exactly one required)

| Prop | Type | Description |
|---|---|---|
| `onVCProcessed` | `(results: VerificationResults) => void` | Client-side verification. SDK calls `/v2/vc-verification` and returns result |
| `onVCReceived` | `(txnId: string) => void` | Server-to-server flow. SDK submits VC to `/vc-submission` and returns the `transactionId` |

#### Optional

| Prop | Type | Default | Description |
|---|---|---|---|
| `isEnableScan` | `boolean` | `true` | Enable camera scanning |
| `isEnableUpload` | `boolean` | `true` | Enable file upload |
| `isEnableZoom` | `boolean` | `true` | Enable camera zoom slider (mobile only) |
| `isVPSubmissionSupported` | `boolean` | `false` | Enable VP-based data-share flow for online VC sharing QR codes |
| `triggerElement` | `ReactNode` | — | Element that opens the scanner/uploader |
| `transactionId` | `string` | — | Optional transaction ID to reuse |
| `scannerActive` | `boolean` | `true` | Enable/disable the scanner |
| `onClose` | `() => void` | — | Called when the scanner is closed |
| `uploadButtonId` | `string` | — | Custom ID for the upload button |
| `uploadButtonStyle` | `string` | — | Custom CSS class for the upload button |
| `vcVerificationV2Request` | `VCVerificationV2Request` | — | Controls verification checks |
| `summariseResults` | `boolean` | `true` | When `true`, returns simplified `verificationStatus`. When `false`, returns full per-check breakdown |

**`VCVerificationV2Request` options:**

| Key | Type | Default | Description |
|---|---|---|---|
| `skipStatusChecks` | `boolean` | `false` | Skip revocation/suspension checks |
| `statusCheckFilters` | `string[]` | `[]` | Run only selected checks |
| `includeClaims` | `boolean` | `false` | Include extracted claims in result |

**Supported upload formats:** PNG, JPEG, JPG, PDF  
**File size limits:** 10 KB minimum, 5 MB maximum

### QR Code Types Handled

| QR content | Behaviour |
|---|---|
| Embedded VC (JSON-LD, SD-JWT, CWT) | Decoded with [PixelPass](https://github.com/inji/pixelpass), verified via `/v2/vc-verification` |
| Data-share URL (starts with `INJI_OVP://`) | See data-share flow below |

### Sequence — Standard Scan/Upload Flow

![Scan/upload flow](images/sdk-qr-scan-upload.svg)

### Data-Share (Online VC) Flow

When the QR code contains a data-share URL (prefixed with `INJI_OVP://`):

**`isVPSubmissionSupported=false` (default):** Redirects directly to the Online VC Provider with `client_id` and `redirect_uri`. The provider returns an OpenID4VP 1.0 DCQL `vp_token` in the redirect hash (no `presentation_submission`). The SDK extracts the VC and verifies it via `/v2/vc-verification`. No backend VP session involved.

**`isVPSubmissionSupported=true`:** Full VP flow — creates a VP request on the backend, redirects to the Online VC Provider with full authorization parameters, receives `response_code` on redirect back, and fetches results via `/vp-session-results`.

![Data-share flow](images/sdk-qr-datashare.svg)

### Server-to-Server Flow

Use `onVCReceived` when the Relying Party backend handles verification:

![Server-to-server flow](images/sdk-qr-s2s.svg)

### Result Shapes

**`summariseResults=true` (default):**
```json
[
  {
    "vc": { "...": "..." },
    "verificationResponse": {
      "verificationStatus": "SUCCESS"
    }
  }
]
```

**`summariseResults=false`:**
```json
[
  {
    "vc": { "...": "..." },
    "verificationResponse": {
      "allChecksSuccessful": true,
      "schemaAndSignatureCheck": { "valid": true, "error": null },
      "expiryCheck": { "valid": true },
      "statusCheck": [{ "purpose": "revocation", "valid": true, "error": null }],
      "claims": { "givenName": "Alice" }
    }
  }
]
```

#### Response Fields Summary

| Property | Type | Description |
|---|---|---|
| `vc` | object | The VC that has been verified |
| `allChecksSuccessful` | boolean | Final aggregated validation flag |
| `schemaAndSignatureCheck` | object | Schema and signature validation result |
| `schemaAndSignatureCheck.valid` | boolean | If false, credential signature or schema is invalid |
| `schemaAndSignatureCheck.error` | object | Non-null if the check could not be performed |
| `expiryCheck` | object | Expiry validation result |
| `expiryCheck.valid` | boolean | If false, the credential is EXPIRED |
| `statusCheck` | array | Contains revocation and other status validations |
| `statusCheck[].purpose` | string | Identifies purpose (e.g., "revocation") |
| `statusCheck[].valid` | boolean | If false for revocation and `error` is null → credential is revoked |
| `statusCheck[].error` | object | Non-null if the status check could not be performed (e.g. status list unreachable) |
| `claims` | object | Includes all claims from credentialSubject |

### Basic Usage Example

```tsx
import { QRCodeVerification } from "@injistack/react-inji-verify-sdk";

<QRCodeVerification
  verifyServiceUrl="https://verify.example.com/v1/verify"
  clientId="client123"
  isEnableScan={true}
  isEnableUpload={true}
  triggerElement={<button>Scan / Upload</button>}
  onVCProcessed={(results) => console.log(results)}
  onError={(err) => console.error(err)}
  vcVerificationV2Request={{ skipStatusChecks: false, statusCheckFilters: ["revocation"], includeClaims: true }}
/>
```

---

## OpenID4VPVerification

Handles the full OpenID4VP v1.0 flow — creates the VP request, displays a QR code or redirects to a wallet (or uses Digital Credentials API), polls for status, and returns verification results. Supports three flow families across four presentation paths: **Generate QR Code**, **Open Web Wallet**, **Open Wallet with DC API**, and **Open Wallet in Mobile** (without DC API).

For a detailed description of each flow see [OpenID4VP-1.0.0.md](./OpenID4VP-1.0.0.md). The configuration matrix is under [Supported flows](#supported-flows).

### Import

```tsx
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";
```

### Props

#### Required

| Prop | Type | Description |
|---|---|---|
| `verifyServiceUrl` | `string` | Base URL of the Verify Backend (e.g. `https://verify.example.com/v1/verify`) |
| `clientId` | `string` | Verifier client identifier. Use the pre-registered string for by-value flows or `decentralized_identifier:did:...` or `x509_san_dns:...` for signed-request by-reference flows |
| `dcqlQuery` | `DcqlQuery` | DCQL query describing which credentials to request. Replaces `presentationDefinition`. Note: `trusted_authorities` is not supported — queries containing it will be rejected. |
| `onQrCodeExpired` | `() => void` | Called when the QR code / authorization request expires before submission |
| `onError` | `(error: AppError) => void` | Called on any error during the flow |

#### Result callbacks (exactly one required)

| Prop | Type | Description |
|---|---|---|
| `onVPProcessed` | `(results: VerificationResults) => void` | Client-side result retrieval. SDK fetches and returns verification results. Use this for most integrations |
| `onVPReceived` | `(transactionId: string) => void` | Server-to-server flow. SDK only signals VP receipt; the Relying Party backend fetches results using `transactionId` |

#### Optional

| Prop | Type | Default | Description |
|---|---|---|---|
| `isSameDeviceFlowEnabled` | `boolean` | `true` | When `true`, triggers same-device flow on click. When `false`, always shows QR code (cross-device) |
| `enableDcApi` | `boolean` | `false` | Same-device only: use W3C Digital Credentials API (`response_mode=dc_api`). Mutually exclusive with `webWalletBaseUrl`. Requires signed-request `clientId` (`decentralized_identifier:` or `x509_san_dns:`). If unsupported at runtime, falls back to deep-link without surfacing an error |
| `dcApiTimeoutMs` | `number` | `300000` | Timeout (ms) for DC API JWT fetch and `navigator.credentials.get`. Invalid values fall back to 5 minutes |
| `webWalletBaseUrl` | `string` | — | Base URL of a web wallet. When set, redirects to `{webWalletBaseUrl}/authorize?...` and enables `responseCodeValidationRequired` |
| `triggerElement` | `ReactNode` | — | UI element that starts the flow on click. If omitted, the flow starts automatically on mount |
| `transactionId` | `string` | — | Optional. Reuse an existing transaction instead of generating one |
| `protocol` | `string` | `openid4vp://` | URI scheme prefix for QR code / deep link construction |
| `qrCodeStyles` | `object` | See below | QR code appearance options |
| `vpVerificationRequest` | `VPVerificationRequest` | — | Controls which verification checks to run when fetching results |
| `summariseResults` | `boolean` | `true` | When `true`, returns simplified `vcStatus` per credential. When `false`, returns full per-check breakdown |

**`qrCodeStyles` options:**

| Key | Default | Description |
|---|---|---|
| `size` | `200` | QR code size in pixels |
| `level` | `"L"` | Error correction level (`L`, `M`, `Q`, `H`) |
| `bgColor` | `"#ffffff"` | Background colour |
| `fgColor` | `"#000000"` | Foreground colour |
| `margin` | `10` | Margin in pixels |
| `borderRadius` | `10` | Border radius in pixels |

**`VPVerificationRequest` options:**

| Key | Type | Default | Description |
|---|---|---|---|
| `skipStatusChecks` | `boolean` | `false` | Skip revocation/suspension checks |
| `statusCheckFilters` | `string[]` | `[]` | Run only selected checks (e.g. `["revocation"]`) |
| `includeClaims` | `boolean` | `false` | Include extracted claims in result |

### Supported flows

`OpenID4VPVerification` supports four presentation paths. Configure props as below (plus required `verifyServiceUrl`, `clientId`, `dcqlQuery`, and result/error callbacks).

| Flow | When to use | Key props | `response_mode` | What the user sees |
|---|---|---|---|---|
| **Generate QR Code** | Cross-device: wallet on another phone scans the verifier screen | `isSameDeviceFlowEnabled={false}` | `direct_post` | QR (`openid4vp://authorize?…`); SDK long-polls status |
| **Open Web Wallet** | Same-device browser redirect to a web wallet | `isSameDeviceFlowEnabled={true}` (default), `webWalletBaseUrl="https://…"`, `enableDcApi` unset/`false` | `direct_post` + `responseCodeValidationRequired=true` | Redirect to `{webWalletBaseUrl}/authorize?…`; return via `#response_code=` |
| **Open Wallet with DC API** | Same-device mobile or desktop browser that supports Digital Credentials API (`processDcAPIFlow()`) | `isSameDeviceFlowEnabled={true}`, `enableDcApi={true}`, **no** `webWalletBaseUrl`; `clientId` must be `decentralized_identifier:` or `x509_san_dns:` | `dc_api` | Browser `navigator.credentials.get`; SDK submits to `/vp-submission/dc-api` |
| **Open Wallet in Mobile** (without DC API) | Same-device mobile deep link to a native wallet | `isSameDeviceFlowEnabled={true}`, `enableDcApi` unset/`false` (or DC API unsupported), **no** `webWalletBaseUrl`, mobile UA | `direct_post` | Redirect to `openid4vp://authorize?…` (or `protocol` override) |

**Shared notes**
- `enableDcApi` and `webWalletBaseUrl` are **mutually exclusive** (throws if both are set).
- **Open Wallet with DC API** runs on any supported browser (desktop or mobile) when `isDcApiSupported(clientId)` is true — it is not mobile-only.
- If DC API is not used or not supported, desktop same-device fallback requires `webWalletBaseUrl` (**Open Web Wallet**). Without it, the flow returns `MISSING_WEB_WALLET_BASE_URL`.
- If `enableDcApi={true}` but the browser/`clientId` does not support DC API, the SDK **silently falls back** to the deep-link / native-wallet path (no `DC_API_NOT_SUPPORTED` error); on desktop that fallback still needs `webWalletBaseUrl`.
- Verify UI maps `ENABLE_DC_API=true` to `enableDcApi`, and disables DC API when a web wallet is selected.

#### How DC API relates to QR codes

DC API in this SDK is **same-device browser mediation** (`response_mode=dc_api`). It does **not** render a QR for the wallet; the browser mediator receives the signed request JWT and returns credential data to the SDK.

QR codes are the **Generate QR Code** flow (`isSameDeviceFlowEnabled={false}`, always `direct_post`). That path is independent of `enableDcApi`.

What *is* shared with DC API is the **by-reference request JWT** when `clientId` uses `decentralized_identifier:` or `x509_san_dns:`:

- Both the DC API mediator and a wallet that scanned a QR/`request_uri` deep link call `GET /v2/vp-request/{requestId}`.
- The JWT header is the same trust model either way: `kid` for DID, `x5c` for `x509_san_dns` (see Stoplight [Get VP Request JWT](https://mosip.stoplight.io/docs/inji-verify/branches/main/11d908cda74f6-get-vp-request-jwt-v2-open-id-4-vp-1-0)).
- So DC API does not “encode a QR”, but it reuses the same signed authorization request artifact that QR/deep-link by-reference flows already use. Cross-device QR remains `direct_post`; same-device DC API remains `dc_api`.

### Flow Selection Logic

The component picks a path at trigger time:

```text
isSameDeviceFlowEnabled = false
  → Generate QR Code (response_mode=direct_post)

isSameDeviceFlowEnabled = true (default)
  → enableDcApi = true AND !webWalletBaseUrl AND isDcApiSupported(clientId)
      → Open Wallet with DC API (response_mode=dc_api; desktop or mobile)
  → enableDcApi = true but unsupported at runtime
      → Fall back to deep-link / native-wallet path (no DC_API_NOT_SUPPORTED error);
        on desktop without webWalletBaseUrl, report MISSING_WEB_WALLET_BASE_URL
  → webWalletBaseUrl provided
      → Open Web Wallet
  → Mobile device (detected via user agent)
      → Open Wallet in Mobile without DC API (openid4vp://…)
  → Desktop without webWalletBaseUrl / DC API
      → Error: MISSING_WEB_WALLET_BASE_URL
```

For sequence diagrams (cross-device, same-device mobile, web wallet, DC API, server-to-server), see [OpenID4VP-1.0.0.md](./OpenID4VP-1.0.0.md).

### Server-to-Server Flow

When the Relying Party has its own backend and wants to process results there, use `onVPReceived` instead of `onVPProcessed`.

### Result Shapes

**`summariseResults=true` (default):**

The outer array has one element per credential requested. Each element's `verificationResponse` contains the full `vcResults` list (all credentials) and the overall `vpResultStatus`. For a two-credential VP request:

```json
[
  {
    "vc": { "...": "credential-1 data..." },
    "verificationResponse": {
      "vcResults": [
        { "vc": { "...": "credential-1 data..." }, "vcStatus": "SUCCESS" },
        { "vc": { "...": "credential-2 data..." }, "vcStatus": "SUCCESS" }
      ],
      "vpResultStatus": "SUCCESS"
    }
  },
  {
    "vc": { "...": "credential-2 data..." },
    "verificationResponse": {
      "vcResults": [
        { "vc": { "...": "credential-1 data..." }, "vcStatus": "SUCCESS" },
        { "vc": { "...": "credential-2 data..." }, "vcStatus": "SUCCESS" }
      ],
      "vpResultStatus": "SUCCESS"
    }
  }
]
```

`vpResultStatus` values: `"SUCCESS"` or `"INVALID"`. `vcStatus` values: `"SUCCESS"`, `"INVALID"`, `"EXPIRED"`, `"REVOKED"`.

**`summariseResults=false`:**
```json
[
  {
    "vc": { "...": "..." },
    "verificationResponse": {
      "verifiableCredential": "...",
      "allChecksSuccessful": true,
      "holderProofCheck": { "valid": true, "error": null },
      "schemaAndSignatureCheck": { "valid": true, "error": null },
      "expiryCheck": { "valid": true },
      "statusCheck": [{ "purpose": "revocation", "valid": true, "error": null }],
      "claims": { "givenName": "Alice" }
    }
  }
]
```

#### Response Fields Summary

| Property | Type | Description |
|---|---|---|
| `allChecksSuccessful` | boolean | Final aggregated validation flag |
| `verifiableCredential` | string | The VC that has been verified |
| `holderProofCheck` | object | Holder binding result. `null` when `require_cryptographic_holder_binding=false` |
| `holderProofCheck.valid` | boolean | If false, presenter does not own the credential |
| `holderProofCheck.error` | object | Non-null if the check could not be performed |
| `schemaAndSignatureCheck` | object | Schema and signature validation result |
| `schemaAndSignatureCheck.valid` | boolean | If false, credential signature or schema is invalid |
| `schemaAndSignatureCheck.error` | object | Non-null if the check could not be performed |
| `expiryCheck` | object | Expiry validation result |
| `expiryCheck.valid` | boolean | If false, the credential is EXPIRED |
| `statusCheck` | array | Contains revocation and other status validations |
| `statusCheck[].purpose` | string | Identifies purpose (e.g., "revocation") |
| `statusCheck[].valid` | boolean | If false for revocation and `error` is null → credential is revoked |
| `statusCheck[].error` | object | Non-null if the status check could not be performed (e.g. status list unreachable) |
| `claims` | object | Includes all claims from credentialSubject |

### Error Object

```ts
type AppError = {
  errorMessage: string;
  errorCode?: string;
  transactionId?: string | null;
}
```

### Basic Usage Example

```tsx
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";

<OpenID4VPVerification
  verifyServiceUrl="https://verify.example.com/v1/verify"
  clientId="client123"
  dcqlQuery={{
    credentials: [
      {
        id: "age_credential",
        format: "ldp_vc",
        meta: { type_values: [["VerifiableCredential", "AgeCredential"]] }
      }
    ]
  }}
  isSameDeviceFlowEnabled={false}
  triggerElement={<button>Verify</button>}
  onVPProcessed={(results) => console.log(results)}
  onQrCodeExpired={() => console.log("Expired")}
  onError={(err) => console.error(err)}
  vpVerificationRequest={{ skipStatusChecks: false, statusCheckFilters: ["revocation"], includeClaims: true }}
/>
```

---

## `require_cryptographic_holder_binding`

Each credential entry in `dcqlQuery` accepts a `require_cryptographic_holder_binding` flag (default `true`) that controls whether the wallet must prove cryptographic ownership of the credential.

| Value | Behavior | `holderProofCheck` in result |
|---|---|---|
| `true` (default) | Wallet must wrap the VC in a signed VP. The verifier checks that the presenter owns the credential. | Populated — `valid: true` if holder proof passes |
| `false` | Wallet may submit the VC without a VP wrapper (bare VC). No holder binding check is performed. | `null` |

**Format-specific behavior:**

- **`ldp_vc`**: when `true`, the wallet submits a JSON-LD VP with a `proof` field. When `false`, a bare VC is accepted.
- **`dc+sd-jwt` / `vc+sd-jwt`**: when `true`, a Key Binding JWT (KB-JWT) is required. The KB-JWT payload must contain all four fields: `aud`, `nonce`, `iat`, `sd_hash`. When `false`, KB-JWT validation is skipped.

**Example — disable holder binding for a specific credential:**

```tsx
dcqlQuery={{
  credentials: [{
    id: "age_credential",
    format: "ldp_vc",
    meta: { type_values: [["VerifiableCredential", "AgeCredential"]] },
    require_cryptographic_holder_binding: false  // accept bare VC, holderProofCheck will be null
  }]
}}
