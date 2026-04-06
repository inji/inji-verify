# INJI VERIFY SDK

Inji Verify SDK provides ready-to-use **React components** to integrate [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)-based **Verifiable Credential (VC) and Verifiable Presentation (VP) verification** into any React TypeScript web application.

## Usage Guide

### Step 1: Install the Package

```bash
npm i @injistack/react-inji-verify-sdk
```

### Step 2: Choose Your Verification Method

#### Option A: QR Code Verification (Scan & Upload)

### Step 3 : Import & Use

```javascript
import {QRCodeVerification} from "@injistack/react-inji-verify-sdk";
```

#### QRCodeVerification Component

```javascript
function MyApp() {
  return (
    <QRCodeVerification
      verifyServiceUrl="https://your-backend.com/verify"
      onVCProcessed={(result) => {
        console.log("Verification complete:", result);
        // Handle the verification result here
      }}
      onError={(error) => {
        console.log("Something went wrong:", error);
      }}
      triggerElement={<button>📷 Scan ID Document</button>}
      clientId="CLIENT_ID"
    />
  );
}
```
###  Request Body Example
###### vcVerificationV2Request

This prop allows us to control how Verifiable Credential (VC) verification is performed by passing request parameters to the verification API.
```javascript
{
    "verifiableCredential": "...",
        "skipStatusChecks": false,
        "statusCheckFilters": ["revocation"],
        "includeClaims": true
}
```
#### Usage 
```javascript

    <QRCodeVerification
        vcVerificationV2Request={vcVerificationV2Request}
    />
```

#### Request Fields Summary

| Property               | Type    | Required | Description                          |
|------------------------|---------|----------|--------------------------------------|
| `verifiableCredential` | string  | ✅      | The VC to verify. Its format is determined in the Verify Service and passed to the vc-verifier.|
| `responseCode`         | string  | ❌      | Response code generated during /vp-submission when validation is required|
| `skipStatusChecks`     | boolean | ❌       | If true, skips all status checks and ignores statusCheckFilters    |
| `statusCheckFilters`   | array   | ❌      | array of status checks to perform          |
| `includeClaims`        | boolean | ❌      |If true, the response includes extracted VC claims in addition to verification and status check results.       |

### Response Received

When verification is completed, the response received is based on summariseResults attribute which will decide the format of the response from SDK.

If summariseResults=true, then response should be
```javascript
{
    "verificationStatus":"STATUS"
}
```
If summariseResults=false, then response should be
```javascript
{
    "allChecksSuccessful": true,
        "schemaAndSignatureCheck": { "valid": true, "error": null },
    "expiryCheck": { "valid": true },
    "statusChecks": [
        { "purpose": "revocation", "valid": true, "error": null }
    ],
        "claims": {...}
}
```
#### Response Fields Summary

| Property                  | Type    | Required | Description                          |
|---------------------------|---------|-----------|--------------------------------------|
| `schemaAndSignatureCheck` | object  | ✅       | Validates schema and signature check |
| `expiryCheck`             | object  | ✅       | If false, the credential is EXPIRED     |
| `statusChecks`            | array   | ❌       | Contains revocation and other status validations           |
| `statusChecks.error`      | object  | ❌       | If present, throws an error instead of returning a status        |
| `statusChecks.purpose`    | object  | ❌       | Identifies purpose (e.g., "revocation")          |
| `statusChecks.valid`      | boolean | ❌       | If false for revocation → credential is revoked            |
| `allChecksSuccessful`     | boolean | ✅       | Final aggregated validation flag   |

    

### Option B: OpenID4VP Verification

### Import & Use

```javascript
import {OpenID4VPVerification} from "@injistack/react-inji-verify-sdk";
```
### OpenID4VPVerification Component

```javascript
function MyApp() {
  return (
    <OpenID4VPVerification
      verifyServiceUrl="https://your-backend.com/v1/verify"
      presentationDefinitionId="your-definition-id"
      onVpProcessed={(result) => {
        console.log("Wallet verification complete:", result);
        // Handle the verification result here
      }}
      onQrCodeExpired={() => alert("QR code expired, please try again")}
      onError={(error) => console.log("Error:", error)}
      triggerElement={<button>📱 Verify with Digital Wallet</button>}
      clientId="CLIENT_ID"
    />
  );
}
```
### Request Body Example
###### vpVerificationRequest

This prop allows us to control how VPVerification is performed by passing request parameters to the verification API.
```javascript
{
    "response_code": "optional-response-code",
        "skipStatusChecks": false,
        "statusCheckFilters": ["revocation"],
        "includeClaims": true
}
```

#### Usage 
```javascript

    <OpenID4VPVerification
        vpVerificationRequest={vpVerificationRequest}
    />
```
### Request Fields Summary

| Property               | Type    | Required | Description                          |
|------------------------|---------|----------|--------------------------------------|
| `responseCode`         | string  | ❌      | Response code generated during /vp-submission when validation is required|
| `skipStatusChecks`     | boolean | ❌       | If true, skips all status checks and ignores statusCheckFilters    |
| `statusCheckFilters`   | array   | ❌      | array of status checks to perform          |
| `includeClaims`        | boolean | ❌      |If true, the response includes extracted VC claims in addition to verification and status check results.       |

### Response Received

When verification is completed, the response received is based on summariseResults attribute which will decide the format of the response from SDK.

If summariseResults=true, then response should be

```javascript
 {
        vcResults: [
            {
                vc: { /* Your verified credential data */ },
                vcStatus: "SUCCESS" // or  "INVALID", "EXPIRED","REVOKED"
            }
        ],
            vpResultStatus: "SUCCESS" //  or "INVALID" Overall verification status
    }
```

If summariseResults=false, then response should be

```javascript
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
                { "purpose": "revocation", "valid": true, "error": null }
            ],
            "claims": {..}
        }
    ]
}  
```

#### Response Fields Summary

| Property                  | Type    | Required | Description                                               |
|---------------------------|---------|-----------|-----------------------------------------------------------|
| `holderProofCheck`        | object  | ✅       | Validates if presenter owns the credential                |
| `schemaAndSignatureCheck` | object  | ✅       | Validates schema and signature check                      |
| `expiryCheck`             | object  | ✅       | If false, the credential is EXPIRED                       |
| `statusChecks`            | array   | ❌       | Contains revocation and other status validations          |
| `statusChecks.error`      | object  | ❌       | If present, throws an error instead of returning a status |
| `statusChecks.purpose`    | object  | ❌       | Identifies purpose (e.g., "revocation")                   |
| `statusChecks.valid`      | boolean | ❌       | If false for revocation → credential is revoked           |
| `allChecksSuccessful`     | boolean | ✅       | Final aggregated validation flag                          |

> **Security Recommendation**
>
> Avoid consuming results directly from VPProcessed or VCProcessed.
> Instead, use VPReceived or VCReceived events to capture the txnId, then retrieve the verification results securely from your backend's verification service endpoint.
> This ensures data integrity and prevents reliance on client-side verification data for final decisions.

## Pre-requisites

### What You Need:

1. **A React project** (TypeScript recommended)
2. **A verification backend** - You need a server that can verify credentials
3. **Camera permissions** - For QR scanning features

### Backend Requirements:

Your backend must support the OpenID4VP protocol. You can either:

- Use the official `inji-verify-service`
- Build your own following [this specification](https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID3.html)

**Important:** Your backend URL should look like:

```
https://your-backend.com
```

## 📖 Detailed Component Guide

### QRCodeVerification Component

**Perfect for:** Scanning QR codes from documents or uploading QR codes (PNG, JPEG, JPG, PDF)

#### Basic Setup:

```javascript
<QRCodeVerification
  verifyServiceUrl="https://your-backend.com"
  onVCProcessed={(result) => handleResult(result)}
  onError={(error) => handleError(error)}
  triggerElement={<button>Start Verification</button>}
  clientId="CLIENT_ID"
/>
```

#### All Available Options:

```javascript
<QRCodeVerification
  // Required
  verifyServiceUrl="https://your-backend.com"
  onVCProcessed={(result) => console.log(result)} // OR use onVCReceived
  onError={(error) => console.log(error)}
  clientId="CLIENT_ID"
  // Optional
  triggerElement={<button>Custom Trigger</button>}
  transactionId="your-tracking-id" //Optional
  uploadButtonId="my-upload-btn"
  uploadButtonStyle={{ backgroundColor: "blue" }}
  isEnableUpload={true} // Allow file uploads
  isEnableScan={true} // Allow camera scanning
  isEnableZoom={true} // Allow camera zoom
  isVPSubmissionSupported={false} // This attribute indicates whether VP submission is supported in Inji OVP VC sharing flow. By default, it is false which means that VP token will be directly sent in response. If set to true, then VP token will be submitted to the VP_SUBMISSION_ URL.
  acceptVPWithoutHolderProof={false} // This attribute controls whether unsigned Verifiable Presentations (VPs without proof) are allowed in the Inji OVP VC sharing flow. By default, it is set to false, meaning unsigned VP tokens are not supported and an error is thrown if an unsigned VP is received. If set to true, VP tokens without a signature (proof) are allowed and can be verified. For data-share it is set to true by default.
  vcVerificationV2Request={vcVerificationV2Request}
  summariseResults={true} // This attribute will decide the format of the response from SDK
/>
```

**Choose One Callback:**

- `onVCProcessed`: Get full verification results immediately
- `onVCReceived`: Get just a transaction ID (your backend handles the rest)

### OpenID4VPVerification Component

**Perfect for:** Integrating with digital wallets (like mobile ID apps)

#### Basic Setup:

```javascript
<OpenID4VPVerification
  verifyServiceUrl="https://your-backend.com"
  presentationDefinitionId="what-you-want-to-verify"
  onVpProcessed={(result) => handleResult(result)}
  onQrCodeExpired={() => alert("Please try again")}
  onError={(error) => handleError(error)}
  clientId="CLIENT_ID"
/>
```

#### With Presentation Definition:

```javascript
<OpenID4VPVerification
  verifyServiceUrl="https://your-backend.com"
  presentationDefinition={"Refer Option 2 below"}
  onVpProcessed={(result) => console.log(result)}
  onQrCodeExpired={() => alert("QR expired")}
  onError={(error) => console.error(error)}
  triggerElement={<button>🔐 Verify Credentials</button>}
  clientId="CLIENT_ID"
/>
```

#### Define What to Verify:

**Option 1: Use a predefined template**

```javascript
presentationDefinitionId = "drivers-license-check";
```

**Option 2: Define exactly what you want**

```javascript
presentationDefinition={{
  id: "custom-verification",
  purpose: "We need to verify your identity",
  format: {
    ldp_vc: {
      proof_type: ["Ed25519Signature2020"],
    },
  },
  input_descriptors: [
    {
      id: "id-card-check",
      constraints: {
        fields: [
          {
            path: ["$.type"],
            filter: {
              type: "object",
              pattern: "DriverLicenseCredential",
            },
          },
        ],
      },
    },
  ],
}}
```

## 🎛️ Component Options Reference

### Common Props (Both Components)

| Property                     | Type          | Required | Description                                 |
|------------------------------|---------------| ----- |---------------------------------------------|
| `verifyServiceUrl`           | string        | ✅     | Backend verification URL                    |
| `onError`                    | function      | ✅     | Callback invoked when an error occurs       |
| `triggerElement`             | React element | ❌     | Custom button/element to start verification |
| `transactionId`              | string        | ❌     | Optional client-side tracking ID            |
| `clientId`                   | string        | ✅     | Client identifier                           |
| `acceptVPWithoutHolderProof` | boolean       | ❌     | Allow unsigned Verifiable Presentations     |
| `summariseResults`           | boolean       | ❌     | Decides format of SDK Response              |

### QRCodeVerification Specific

| Property                  | Type     | Default | Description                               |
|---------------------------|----------|---------|-------------------------------------------|
| `onVCProcessed`           | function | -       | Get full results immediately              |
| `onVCReceived`            | function | -       | Get transaction ID only                   |
| `isEnableUpload`          | boolean  | true    | Allow file uploads                        |
| `isEnableScan`            | boolean  | true    | Allow camera scanning                     |
| `isEnableZoom`            | boolean  | true    | Allow camera zoom                         |
| `uploadButtonStyle`       | object   | -       | Custom upload button styling              |
| `isVPSubmissionSupported` | Boolean  | false   | Toggle VP submission support              |
| `vcVerificationV2Request` | object   | -       | contains request body for vc verification |

### OpenID4VPVerification Specific

| Property                 | Type     | Default        | Description                               |
|--------------------------| -------- | -------------- |-------------------------------------------|
| `protocol`               | string   | "openid4vp://" | Protocol for QR codes (optional)          |
| `presentationDefinitionId` | string   | -              | Predefined verification template          |
| `presentationDefinition` | object   | -              | Custom verification rules                 |
| `onVpProcessed`          | function | -              | Get full results immediately              |
| `onVpReceived`           | function | -              | Get transaction ID only                   |
| `onQrCodeExpired`        | function | -              | Handle QR code expiration                 |
| `isSameDeviceFlowEnabled` | boolean  | true           | Enable same-device flow (optional)        |
| `qrCodeStyles`           | object   | -              | Customize QR code appearance              |
| `vpVerificationRequest`  | object   | -       | contains request body for vp verification |

## ⚠️ Important Limitations

- **React Only:** Won't work with Angular, Vue, or React Native
- **Backend Required:** You must have a verification service running
