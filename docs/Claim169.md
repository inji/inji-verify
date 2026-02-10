# Claim 169

Inji introduces a privacy-first method by issuing a VC that contains one or more embedded QR codes:

- Each QR code reveals only the necessary claims (e.g., just age).
- Users can choose which QR code to share.
- Sensitive identity data is protected.

This method ensures convenience, security, and privacy for residents.

# What is in the QR Code?
In a VC, attributes like name, gender, DOB, or age are called Claims.

A VC may include multiple QR codes, each containing only a specific subset of claims—ideal for privacy-preserving verification.

Example use cases:

- Show only age for alcohol purchase
- Show only address for delivery verification
- Show name + photo for KYC

For each QR code, the issuer can select a specific subset of claims, map them to Claim 169, CBOR-encode and sign them as a CWT, and then zlib-compress and Base45-encode the result.

# Example CWT internal structure:

```cbor
COSE_Sign1 = [
  protected_header,
  unprotected_header,
  CBOR_encoded_claims,
  COSE_signature
]
```

# What is Claim 169?
Claim 169 is an approach where personal identity fields are mapped to numeric claim IDs.

Refer here for full list of mappings: [Claim 169 - QR Code Specification](https://docs.mosip.io/1.2.0/readme/standards-and-specifications/mosip-standards/169-qr-code-specification#id-3.-semantics)

# Functionalities

## 1. Verifiable Credential Submission:
- Inji Verify allows users to SCAN & UPLOAD Verifiable Credential.

- Once the verifier scans the QR code, it decodes the QR code using PixelPass.
This will:
    - Base45-decode the CWT data
    - Zlib-decompress the decoded data
    - Extract the actual CWT.

## 2. Verifiable Credential Verification:

- The Verify UI calls /vc-verification endpoint and sends:
    - credential as a CWT hex string

    - Content-Type: `application/vc+cwt`

- The Verify Service calls `vc-verifier` with the CWT.

- The `vc-verifier` verifies the CWT (`cwtHex`) by performing below steps:
    - decodeCose()
    - extractCwtClaims()
    - extractIssuer(iss)
    - resolveIssuerMetadata()
    - fetchPublicKeys()
    - selectKeyByKid()
    - verifyCoseSignature()
    - Return status

- Once the `CWT VC` is verified, the `Verify UI` displays the claims.

## 3. Display Result:

- The `Verify UI` decodes the `CWT` to extract the payload. The `Verify UI` extracts `claim 169` from the payload.

- The `Verify UI` sends `claim 169` to `decodeMappedData()` of `PixelPass`, which:

    - Reverse-maps the keys and values in claim 169
    - Returns the mapped JSON

- The `Verify UI` displays the claims with verification status.