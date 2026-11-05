# mDL (`mso_mdoc`) test samples

These are test ISO/IEC 18013-5 mobile driving licence samples for the Inji Verify API tests and the Postman collection. Each `.txt` file holds one base64url string (no padding). Send that string as `verifiableCredential` to `POST /v1/verify/v2/vc-verification`.

All data is fictional. The Document Signer certificate is self-signed and marked **NOT FOR PRODUCTION**.

## Regenerate

The generator script is not committed. The samples were made once, outside the tests, and checked against verify-service; the tests never create credentials. To change a sample, regenerate the whole set: each run uses a new issuer key, a new certificate and dates relative to the time of the run, and the private keys are never saved, so a single sample cannot be re-signed. Before the set was written, the valid sample was decoded and checked that:

- the COSE signature verifies against the certificate
- every item digest matches `valueDigests`
- the CBOR tags are correct (24, 1004 and 0)
- `issuing_country` equals the certificate's `C`
- the encoding is deterministic

## Where they are used

- `UtilsTest` loads these files from the test classpath for mdoc detection, shapes A/B/C, claim extraction, meta claims, the size limit and empty `documents`. For the size limit it pads `mdl-valid.txt` past 1 MiB in the test. `MdocSmokeTest` verifies `mdl-valid.txt` with vc-verifier.
- The Postman collection (`v2/vc-verification` > `mso_mdoc`) embeds the same strings. Its oversized request builds an over-limit payload in a pre-request script.
- `mdl-expired.txt` and `mdl-not-yet-valid.txt` are not used yet; they are added to the tests and the Postman collection once vc-verifier#300 ships.

## Samples

`validUntil` values are from the run committed here, on 2026-11-05. Results were checked against verify-service built from `injiver-2266-mdl-scan-upload`.

| File | What it is | validUntil | Expected result | Actual today |
|---|---|---|---|---|
| `mdl-valid.txt` | Shape A `{issuerAuth, nameSpaces}`. The 11 mandatory mDL elements, ES256, `validFrom` = now − 1 day | 2031-11-05T05:05:21Z | All checks pass | As expected |
| `mdl-tampered-element.txt` | `family_name` changed to `Smith`. The MSO is unchanged | 2031-11-05T05:05:21Z | `schemaAndSignatureCheck.valid: false` | As expected (digest mismatch) |
| `mdl-bad-signature.txt` | One byte of the COSE signature is flipped | 2031-11-05T05:05:21Z | `schemaAndSignatureCheck.valid: false` | As expected |
| `mdl-shape-b.txt` | `{docType, issuerSigned: {issuerAuth, nameSpaces}}` | 2031-11-05T05:05:21Z | Same as valid | As expected |
| `mdl-shape-c.txt` | `{documents: [{docType, issuerSigned}]}` | 2031-11-05T05:05:21Z | Same as valid | As expected |
| `mdl-empty-documents.txt` | `{documents: []}` | – | HTTP 400 `mdoc contains no documents` | As expected |
| `mdl-corrupt-cbor.txt` | Valid sample cut to half its length. It still contains both detection keys, so it is routed to the mdoc path | – | HTTP 400 `Failed to decode mdoc credential` | As expected |
| `mdl-expired.txt` | Re-signed with `validFrom` = now − 2 years and `validUntil` = now − 1 day | 2026-11-04T05:05:21Z | `expiryCheck.valid: false` | **Differs:** `schemaAndSignatureCheck.valid: false` with `ERR_INVALID_VALID_UNTIL_MSO`, and `expiryCheck: null` |
| `mdl-not-yet-valid.txt` | Re-signed with `validFrom` = now + 1 year | 2031-11-05T05:05:21Z | `expiryCheck.valid: false` | **Differs:** all checks pass, because `validFrom` is not checked |

The two expiry samples will match the expected column once vc-verifier#300 ships.

`mdl-expired.txt` has `signed` = now − 2 years, which is earlier than the certificate's `notBefore`. ISO requires `validFrom` ≥ `signed`, so the two dates cannot both match the specification.

The 400 responses use Spring Boot's default error body, as for the other formats. The messages above appear in the body only when `server.error.include-message=always` is set.

## Expiry

The valid samples expire on **2031-11-05T05:05:21Z** (`validUntil`). The certificate expires on 2032-11-05T05:05:21Z. Regenerate before then, or `mdl-valid.txt` will start failing.

## Certificate

`mdl-test-document-signer.pem` contains the public key only.

- Subject: `C=IN, ST=KA, O=Inji Verify Test, CN=Inji Verify TEST Document Signer - NOT FOR PRODUCTION`
- Key and signature: EC P-256 key, signed with ecdsa-with-SHA256
- Extensions (all critical): BasicConstraints CA=false, KeyUsage digitalSignature, EKU `1.0.18013.5.1.2`
