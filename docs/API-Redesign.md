# API Redesign (v0.18)

Starting with v0.18, `Inji Verify Backend` returns detailed verification information for VC and VP flows, along with improved session handling and security.

---

## Why is API Redesign Needed?

### Limitations in Existing Design

#### 1. Limited Response Visibility
- Only final status returned:  
  `SUCCESS / INVALID / EXPIRED / REVOKED`
- No explanation is provided when verification fails

#### 2. Session Handling Issues
- `transactionId` stored in `sessionStorage`
- Lost during redirection in Same-Device flow (Web Wallet → Verify UI)

#### 3. Security Concerns

| Mechanism          | Issue                       |
|--------------------|-----------------------------|
| sessionStorage     | Lost on redirect            |
| localStorage       | Vulnerable to XSS           |
| No session binding | Risk of unauthorised access |

---

## Key Improvements

- Detailed verification breakdown
- HttpOnly cookie-based session binding
- Optional `response_code` validation
- Replay attack protection
- Unified result retrieval API

---

## Old Design

### 1. POST /vc-verification

#### Request Body
```json
{
  "vc": "..."
}
```

#### Response Body
```json
{
  "verificationStatus": "SUCCESS"
}
```

---

### 2. GET /vp-results/{transactionId}

#### Response Body
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

---

### Limitations
- Only a simple final verification status is returned:  
  `SUCCESS / INVALID / EXPIRED / REVOKED`

---

## New Design

- Returns detailed contextual verification results explaining success/failure.

---

### 1. POST /v2/vc-verification

#### Request Body
```json
{
  "verifiableCredential": "...",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation", "suspension"],
  "includeClaims": true
}
```

#### Response Body
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

---

### 2. POST /v2/vp-results/{transactionId}

#### Request Body
```json
{
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation", "suspension"],
  "includeClaims": true
}
```

#### Response Body
```json
{
  "transactionId": "txn_11",
  "allChecksSuccessful": true,
  "credentialResults": []
}
```

---

## Session Storage Handling

### Problem

- Session loss during redirect
- `transactionId` is stored in sessionStorage, but it gets lost after the Wallet → Verify UI redirect.
- Mobile wallets do not reliably support redirect-based flows

---

### Solution

- HttpOnly cookie-based session binding
- Optional `response_code` validation
- Unified result endpoint

---

### 1. POST /vp-session-request

#### Behaviour

- Generates:
    - `transaction_id`
    - `request_id`
- Returns Authorization Request
- Sets cookie:

```http
Set-Cookie: transaction_id=<uuid>; HttpOnly; Secure; SameSite=None
```

#### Cookie Configuration

| Environment | Secure | SameSite |
|------------|--------|----------|
| Production | true   | None     |
| Local      | false  | Lax      |

---

#### Flow Mapping

| Flow Type                    | response_code_validation_required |
|-----------------------------|----------------------------------|
| Cross Device (QR)           | false                            |
| Same Device – Web Wallet    | true                             |
| Same Device – Mobile Wallet | false                            |

---

### 2. POST /vp-submission/direct-post

#### Case 1: Validation Required

- Generates `response_code`

**Properties:**
- Short-lived (1–5 minutes)
- Single-use
- Cryptographically secure

#### Response

```json
{

  "redirect_uri": "https://example.com#response_code=<response_code>"

}
```

---

#### Case 2: Validation Not Required

- No `response_code`
- No `redirect_uri`
- Returns `200 OK`

---

### 3. POST /vp-session-results (Public API)

#### Headers

```http
Cookie: transaction_id=<transaction_id>
```

---

#### Request

```json
{
  "response_code": "optional",
  "skipStatusChecks": false,
  "statusCheckFilters": ["revocation", "suspension"],
  "includeClaims": true
}
```

---

#### Validation Logic

**Step 1: Validate Cookie**
- Missing → `401 Unauthorized`

---

**Case 2: Validation Not Required**
- Return results directly

---

**Case 3: Validation Required**

With `response_code`:

- Validate:
    - Match
    - Not expired
    - Not used
- Mark as used
- Return results

Without `response_code`:
- Return error

---

#### Cookie Cleanup

```http
Set-Cookie: transaction_id=; Max-Age=0
```

---

#### SDK Behavior

| summariseResults | Behavior             |
|------------------|---------------------|
| true             | Minimal response    |
| false            | Full detailed response |

---

#### API Classification

**Public Endpoint**
- POST /vp-session-results

**Internal Endpoints**
- POST /v2/vp-results/{transactionId}
- GET /vp-result/{transactionId}

---

#### Security Improvements

- HttpOnly cookie
- No localStorage usage
- Single-use `response_code`
- Replay attack protection
- Secure session binding

---

## Summary

| Feature           | Old Design       | New Design        |
|-------------------|-----------------|------------------|
| Session Handling  | sessionStorage  | HttpOnly Cookie  |
| Redirect Safe     | No              | Yes              |
| Detailed Results  | No              | Yes              |
| Replay Protection | No              | Yes              |
| Unified API       | No              | Yes              |