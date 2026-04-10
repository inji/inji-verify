# INJI VERIFY SDK

Inji Verify SDK provides ready-to-use **React components** to integrate [OpenID4VP](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)-based **Verifiable Credential (VC) and Verifiable Presentation (VP) verification** into any React TypeScript web application.

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
https://your-backend.com/v1/verify
```

## Usage Guide

### Step 1: Install the Package

```bash
npm i @injistack/react-inji-verify-sdk
```

### Step 2: Choose Your Verification Method

### Option A: QR Code Verification (Scan & Upload)

The QRCodeVerification component enables end-to-end Verifiable Credential (VC) verification using QR codes in Inji-Verify. It supports both camera-based scanning and file upload for QR code verification.

**Perfect for:** Scanning QR codes from documents, or uploading QR codes (PNG, JPEG, JPG, PDF) within the supported size range of 10 KB to 5 MB.

Steps to integrate:

###  Import & Usage

```javascript
import {QRCodeVerification} from "@injistack/react-inji-verify-sdk";
```

#### 1. Uploading a Verifiable Credential (VC) for verification

a. Client-side handling (onVCProcessed)

```javascript
function MyApp() {
  return (
  <QRCodeVerification 
      triggerElement={triggerElement}
      verifyServiceUrl="https://your-backend.com/v1/verify"
      isEnableScan={false}
      onVCProcessed={(result) => {
        console.log("Verification complete:", result);
        // Handle the verification result here
      }}
      onError={(error) => {
        console.log("Something went wrong:", error);
      }}
      clientId="did"
    />
  );
}
```
b. Server-to-server handling (onVCReceived)
```javascript
function MyApp() {
  return (
  <QRCodeVerification 
      triggerElement={triggerElement}
      verifyServiceUrl="https://your-backend.com/v1/verify"
      isEnableScan={false}
      onVCReceived={(transactionId) => {
          //using the transactionId, one can securely fetch the result from service
          console.log("VC received txnId:", transactionId);
      }}
      onError={(error) => {
        console.log("Something went wrong:", error);
      }}
      clientId="pre_registered"
    />
  );
}
```

####  2. Scanning a Verifiable Credential (VC) Using Device Camera

a. Client-side handling (onVCProcessed)

```javascript
function MyApp() {
  return (
  <QRCodeVerification
      scannerActive={scannerActive}
      verifyServiceUrl="https://your-backend.com/v1/verify"
      isEnableUpload={false}
      onClose={onClose} // invoked when scanner is closed 
      onVCProcessed={(result) => {
        console.log("Verification complete:", result);
        // Handle the verification result here
      }}
      onError={(error) => {
        console.log("Something went wrong:", error);
      }}
      clientId="did"
    />
  );
}
```
b. Server-to-server handling (onVCReceived)

```javascript
function MyApp() {
  return (
  <QRCodeVerification
      scannerActive={scannerActive}
      verifyServiceUrl="https://your-backend.com/v1/verify"
      isEnableUpload={false}
      onClose={onClose} // invoked when scanner is closed 
      onVCReceived={(transactionId) => {
          //using the transactionId, one can securely fetch the result from service
          console.log("VC received txnId:", transactionId);
      }}
      onError={(error) => {
        console.log("Something went wrong:", error);
      }}
      clientId="pre_registered"
    />
  );
}
```

> **NOTE: QRCodeVerification Methods**
>
>- onVCProcessed returns the verification result directly to the client.
>- onVCReceived returns only a transactionId, allowing the backend to securely fetch verification results.
>- Only one of these callbacks should be used at a time.

### Verification Response

Once VCVerification is completed, the response will be based on summariseResults attribute.

If summariseResults=true, then response will be
```javascript
{
    "verificationStatus":"STATUS"
}
```
If summariseResults=false, then response will be
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

| Property                  | Type    | Optional | Description                          |
|---------------------------|---------|----------|--------------------------------------|
| `allChecksSuccessful`     | boolean | ✅        | Final aggregated validation flag   |
| `schemaAndSignatureCheck` | object  | ✅        | Validates schema and signature check |
| `expiryCheck`             | object  | ✅        | If false, the credential is EXPIRED     |
| `statusChecks`            | array   | ❌        | Contains revocation and other status validations           |
| `statusChecks.error`      | object  | ❌        | If present, throws an error instead of returning a status        |
| `statusChecks.purpose`    | string  | ❌        | Identifies purpose (e.g., "revocation")          |
| `statusChecks.valid`      | boolean | ❌        | If false for revocation → credential is revoked            |
| `claims`                  | object  | ❌        |  Include all claims from credentialSubject   |

### Option B: OpenID4VP Verification
OpenID4VPVerification Component verifies Verifiable Presentations securely using OpenID4VP standards for both cross-device and same-device scenarios.

**Perfect for:** Integrating with digital wallets (like mobile ID apps)

Steps to integrate:
### Import & Usage

```javascript
import {OpenID4VPVerification} from "@injistack/react-inji-verify-sdk";
```
#### 1. Same Device Flow (Recommended Default)
```javascript
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";
export default function VerifySameDevice() {
    return (
        <OpenID4VPVerification
            triggerElement={<button>Verify with Wallet</button>}
            verifyServiceUrl="https://verify.example.com/v1/verify"
            clientId="did"
            presentationDefinitionId="drivers-license-check"
            isSameDeviceFlowEnabled={true} //default value
            webWalletBaseUrl="https://wallet.example.com" // required to support web-wallets 
            onVPProcessed={(result) => {
                console.log("VP processed:", result);
            }}
            onError={(error) => {
                console.error("Verification error:", error);
            }}
        />
    );
}
```
> **NOTE**
>
> When webWalletBaseUrl is configured, we use web-wallets to support verification flow.
>In the absence of webWalletBaseUrl, the SDK falls back to a deep link mechanism to launch the native wallet application if any supported mobile wallet is installed.

#### 2. Cross-device flow (QR code scan from another device)
```javascript
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";
export default function VerifyCrossDevice() {
    return (
        <OpenID4VPVerification
            triggerElement={<button>Show QR for Wallet Scan</button>}
            verifyServiceUrl="https://verify.example.com/v1/verify"
            clientId="did"
            presentationDefinitionId="drivers-license-check"
            isSameDeviceFlowEnabled={false} // QR code flow
            onVPProcessed={(result) => {
                console.log("VP processed:", result);
            }}
            onQrCodeExpired={() => {
                console.log(" QR code expired - ask user to retry");
            }}
            onError={(error) => {
                console.error("Verification error:", error);
            }}
           
        />
    );
}
```

#### 3. Server-to-server callback (onVPReceived)
```javascript
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";

export default function VerifyServerToServer() {
    return (
        <OpenID4VPVerification
            triggerElement={<button>Start Verification</button>}
            verifyServiceUrl="https://verify.example.com/v1/verify"
            clientId="pre_registered"
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
            isSameDeviceFlowEnabled={false}
            onVPReceived={(transactionId) => {
                //using the transactionId one can securely fetch the result from service
                console.log("VP received txnId:", transactionId);
            }}
            onQrCodeExpired={() => {
                console.log("QR code expired");
            }}
            onError={(error) => {
                console.error("Verification error:", error);
            }}
        />
    );
}
```

### Verification Response

Once VPVerification is completed, the response will be based on summariseResults attribute.

If summariseResults=true, then response will be

```javascript
 {
        vcResults: [
            {
                vc: { /* verified credential data */ },
                vcStatus: "SUCCESS" // or  "INVALID", "EXPIRED","REVOKED"
            }
        ],
            vpResultStatus: "SUCCESS" //  or "INVALID" Overall verification status
    }
```

If summariseResults=false, then response will be

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

| Property                  | Type    | Optional | Description                                               |
|---------------------------|---------|----------|-----------------------------------------------------------|
| `allChecksSuccessful`     | boolean | ✅        | Final aggregated validation flag                          |
| `verifiableCredential`    | string  | ✅        | The VC which needs to be verified                         |
| `holderProofCheck`        | object  | ✅        | Validates if presenter owns the credential                |
| `schemaAndSignatureCheck` | object  | ✅        | Validates schema and signature check                      |
| `expiryCheck`             | object  | ✅        | If false, the credential is EXPIRED                       |
| `statusChecks`            | array   | ❌        | Contains revocation and other status validations          |
| `statusChecks.error`      | object  | ❌        | If present, throws an error instead of returning a status |
| `statusChecks.purpose`    | string  | ❌        | Identifies purpose (e.g., "revocation")                   |
| `statusChecks.valid`      | boolean | ❌        | If false for revocation → credential is revoked           |
| `claims`                  | object  | ❌        | Include all claims from credentialSubject                 |

> **Security Recommendation**
>
> Avoid consuming results directly from VPProcessed or VCProcessed.
> Instead, use VPReceived or VCReceived events to capture the transactionId, then retrieve the verification results securely from verification service.
> This ensures data integrity and prevents reliance on client-side verification data for final decisions.

### Presentation Definition:

#### Define What to Verify:

**Option 1: Use a predefined template ID**

```javascript
presentationDefinitionId = "drivers-license-check";
```

**Option 2: Define Presentation Definition**

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
> **NOTE**
>
> Only one of presentationDefinitionId or presentationDefinition should be provided at a time.
> It is recommended to use:
>- presentationDefinitionId when leveraging predefined templates.
>- presentationDefinition when custom verification requirements are needed.

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

| Property                  | Type     | Default | Description                                |
|---------------------------|----------|---------|--------------------------------------------|
| `onVCProcessed`           | function | -       | Get full results immediately               |
| `onVCReceived`            | function | -       | Get transaction ID only                    |
| `isEnableUpload`          | boolean  | true    | Allow file uploads                         |
| `isEnableScan`            | boolean  | true    | Allow camera scanning                      |
| `isEnableZoom`            | boolean  | true    | Allow camera zoom (for mobile and tablets) |
| `uploadButtonStyle`       | string   | -       | Custom upload button styling               |
| `isVPSubmissionSupported` | boolean  | false   | Toggle VP submission support               |
| `vcVerificationV2Request` | object   | -       | contains request body for vc verification  |

### OpenID4VPVerification Specific

| Property                 | Type     | Default        | Description                               |
|--------------------------| -------- |----------------|-------------------------------------------|
| `protocol`               | string   | "openid4vp://" | Protocol for QR codes (optional)          |
| `presentationDefinitionId` | string   | -              | Predefined verification template          |
| `presentationDefinition` | object   | -              | Custom verification rules                 |
| `onVPProcessed`          | function | -              | Get full results immediately              |
| `onVPReceived`           | function | -              | Get transaction ID only                   |
| `onQrCodeExpired`        | function | -              | Handle QR code expiration                 |
| `isSameDeviceFlowEnabled` | boolean  | true           | Enable same-device flow (optional)        |
| `qrCodeStyles`           | object   | -              | Customize QR code appearance              |
| `vpVerificationRequest`  | object   | -              | contains request body for vp verification |

## ⚠️ Important Limitations

- **React Only:** Won't work with Angular, Vue, or React Native
- **Backend Required:** You must have a verification service running
