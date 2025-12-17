# Revocation Support

`Inji Verify` now supports revocation checking, allowing the verifier to determine whether a credential is valid or revoked. Revocation is a mechanism used by an issuer to express the status of a Claim after issuance. 

# Why is VC Revocation needed?

In a `Verifiable Credential` (VC), once it’s issued, it’s cryptographically signed and can be verified.

But what happens if the credential later becomes invalid? For example:
- A driver’s license is revoked
- A student’s degree is withdrawn
- An employment certificate is revoked

We need a way to check if the VC is still valid — without reissuing it.
That's where the `credentialStatus` property comes in.
It points to a `status list` or registry maintained by the issuer, where verifiers can check whether the credential has been revoked.

## API Documentation

The API documentations can be found in the [Inji Verify API documentation](https://mosip.stoplight.io/docs/inji-verify/branches/main/)

## Functionalities 

## 1. UPLOAD & SCAN

### Verifiable Credential Submission:
- Inji Verify allows users to SCAN & UPLOAD Verifiable Credential.

- Once the verifier scans the QR code, it decodes the QR code using pixel_pass_library and sends the extracted VC to the Inji Verify backend for storage in the database.

- A typical JSON-LD Revoked Verifiable Credential includes a field like this:

    ```json
    "credentialStatus": {
    "id": "https://example.org/status/24#94567",
    "type": "StatusList2021Entry",
    "statusPurpose": "revocation",
    "statusListIndex": "94567",
    "statusListCredential": "https://example.org/status/24"
    }
    ```

- Field-by-Field Explanation

| Field                 | Meaning                                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------------------|
| id                    | A unique URL identifier for this status entry.                                                            |
| type                  | The method used for status checking — e.g., `BitstringStatusListEntry`.                                   |
| statusPurpose         | Describes why this status exists — e.g., "revocation".                                                    |
| statusListIndex       | A numeric index or position of status of credential.                                                      |
| statusListCredential  | The URI of the status list credential.                                                                    |

### Submission Result:

- After a successful `UPLOAD` or `SCAN`, `INJI VERIFY` performs server-side verification of a Verifiable Credential (VC) to validate its integrity and authenticity. It executes checks such as cryptographic signature validation and ensures that the credential has not been altered or tampered with.

### How it checks for revocation

> - **_Issuer_**: assigns each new credential a statusListIndex and puts that index into the VC's credentialStatus object.
>
> - **_Inji Verify Backend (vc verifier)_**: retrieves the `statusListCredential`, decodes its encodedList, checks the bit at that index to see whether it's revoked.

- The verification status returned can be **_SUCCESS_**, **_INVALID_**, **_EXPIRED_** or **_REVOKED_**.

- If the `Inji Verify Backend` encounters any `error` while retrieving the `statusListCredential` or verifying the status, it will respond with an `error description` and `status code` as `500`.

## 2. OPENID4VP

### Verifiable Presentation Submission

Inji Verify allows users to submit a Verifiable Presentation.

- Inji Verify generates QR code with VP request.

- Once the `wallet` scans the QR code, it generates the `VP token` and the `presentation submission`, and then posts them to the Inji Verify backend.

- If the `wallet` encounters any error while generating the `VP token`, it submits the `error` to the `Inji Verify backend` along with an `error description`.

### Submission Result

- Once the `wallet` submits the VP, the status is updated to **_VP_SUBMITTED_**.

- The `Inji Verify Backend` retrieves the `statusListCredential`, decodes its encodedList, and checks the bit at the `statusListIndex` to determine whether the credential is revoked.

- The `Inji Verify UI` can then fetch the result of the submission. The result contains two things:

  1. The overall status of the submission, which is either **_SUCCESS_** or **_FAILED_**. 

  2. A list of VCs with their individual verification statuses, which can be:
  * **_SUCCESS_**
  * **_INVALID_**
  * **_EXPIRED_**
  * **_REVOKED_**
  
During the revocation check, if the Inji Verify Backend encounters any error, it throws an exception with a descriptive error message, which the Verify UI displays to the user.