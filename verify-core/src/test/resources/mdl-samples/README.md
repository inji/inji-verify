# mDL (`mso_mdoc`) test samples

These are test ISO/IEC 18013-5 mobile driving licence samples for the Inji Verify API tests and the Postman collection. Each `.txt` file holds one base64url string (no padding). Send that string as `verifiableCredential` to `POST /v1/verify/v2/vc-verification`.

All data is fictional. The Document Signer certificate is self-signed and marked **NOT FOR PRODUCTION**.

## Regenerate

The generator script is not committed; the tests never create credentials. The set was re-signed on 2026-11-16 for vc-verifier 1.11.0, which checks `issue_date` ≤ `validFrom` and that `signed` is within the certificate validity. The data elements are unchanged from the first set. The samples are signed with the vc-verifier test Document Signer key (`vc-verifier/kotlin/vcverifier/src/test/resources/mso_mdoc/test_ds_key_pkcs8.pem`), using vc-verifier's `MdlEdgeCaseTestDataGenerator` from a throwaway test. Regenerate the whole set together. Before committing, check through vc-verifier `CredentialsVerifier.verify` (strict profile on and off) that each sample gives the result in the table below.

## Where they are used

- `UtilsTest` loads these files from the test classpath for mdoc detection, shapes A/B/C, claim extraction, meta claims, the size limit and empty `documents`. For the size limit it pads `mdl-valid.txt` past 1 MiB in the test. `MdocSmokeTest` verifies `mdl-valid.txt` with vc-verifier.
- The Postman collection (`v2/vc-verification` > `mso_mdoc`) embeds the same strings. Its oversized request builds an over-limit payload in a pre-request script.
- `mdl-expired.txt` and `mdl-not-yet-valid.txt` are not used by the tests or the Postman collection yet.

## Samples

Results are the same with `inji.verify.mso-mdoc.strict-profile` `true` or `false`.

| File | What it is | validUntil | Expected result |
|---|---|---|---|
| `mdl-valid.txt` | Shape A `{issuerAuth, nameSpaces}`. The 11 mandatory mDL elements, ES256, `signed` = `validFrom` = 2026-11-05T05:05:21Z | 2031-11-05T05:05:21Z | All checks pass |
| `mdl-tampered-element.txt` | `family_name` changed to `Smith`. The MSO is unchanged | 2031-11-05T05:05:21Z | `schemaAndSignatureCheck.valid: false`, `ERR_LIKELY_TAMPERED` |
| `mdl-bad-signature.txt` | One byte of the COSE signature is flipped | 2031-11-05T05:05:21Z | `schemaAndSignatureCheck.valid: false`, `ERR_SIGNATURE_VERIFICATION_FAILED` |
| `mdl-shape-b.txt` | `{docType, issuerSigned: {issuerAuth, nameSpaces}}` | 2031-11-05T05:05:21Z | Same as valid |
| `mdl-shape-c.txt` | `{documents: [{docType, issuerSigned}]}` | 2031-11-05T05:05:21Z | Same as valid |
| `mdl-empty-documents.txt` | `{documents: []}` | – | HTTP 400 `mdoc contains no documents` |
| `mdl-corrupt-cbor.txt` | Valid sample cut to half its length. It still contains both detection keys, so it is routed to the mdoc path | – | HTTP 400 `Failed to decode mdoc credential` |
| `mdl-expired.txt` | `signed` = `validFrom` = 2024-11-05T05:05:21Z, `issue_date` 2024-11-05 | 2026-11-04T05:05:21Z | `schemaAndSignatureCheck.valid: true`, `expiryCheck.valid: false` |
| `mdl-not-yet-valid.txt` | `validFrom` = 2027-11-05T05:05:21Z, `signed` = 2026-11-05T05:05:21Z | 2031-11-05T05:05:21Z | `schemaAndSignatureCheck.valid: false`, `ERR_VALID_FROM_IS_FUTURE_DATE` (until 2027-11-05) |

The 400 responses use Spring Boot's default error body, as for the other formats. The messages above appear in the body only when `server.error.include-message=always` is set.

## Expiry

The valid samples expire on **2031-11-05T05:05:21Z** (`validUntil`). The certificate expires on 2032-11-05T05:05:21Z. Regenerate before then, or `mdl-valid.txt` will start failing.

## Certificate

`mdl-test-document-signer.pem` contains the public key only.

- Subject: `C=IN, ST=KA, O=Inji Verify Test, CN=Inji Verify TEST Document Signer - NOT FOR PRODUCTION`
- Valid 2024-01-01T00:00:00Z to 2032-11-05T05:05:21Z
- Key and signature: EC P-256 key, signed with ecdsa-with-SHA256
- Extensions (both critical): KeyUsage digitalSignature, EKU `1.0.18013.5.1.2`
