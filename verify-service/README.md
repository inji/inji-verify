# Inji Verify Backend Service

### Contents
* Features
* Standards
* Setup Guide
* API docs


#### Features
* ###### VC Verification
  It offers an API for verifying VCs on the server side. The API takes a VC as input and carries out validation and proof verification using the [vc-verifier](https://github.com/mosip/vc-verifier/tree/master/vc-verifier/kotlin) module.

* ###### OpenID4VP Sharing
  It is designed to support OpenID4VP specification. The current supported draft is [draft 21](https://openid.net/specs/openid-4-verifiable-presentations-1_0-21.html).

#### Standards
  For OpenID4VP Sharing below are the supported features.
- Cross Device Flow
- Same Device Flow
- `response_type` as `vp_token`
- `response_mode` as `direct_post`
- Verifiable Presentation proofs supported are `ED25519Signature2018`, `ED25519Signature2020` and `RSASignature2018`

Out of scope items are
- `response_type` with `vp_token id_token`
- `response mode` with `direct_post.jwt
##### Setup Guide

The link to set up guide can be found [here](../Readme.md).

##### API docs

The API docs are published in Stoplight, which can be found [here](https://mosip.stoplight.io/docs/inji-verify/branches/main).

#### Using verify-service as a Library Dependency

`verify-service` publishes a plain JAR (without bundled dependencies) that can be consumed by other Maven projects.

Add the following to your `pom.xml`:

**Dependency:**
```xml
<dependency>
    <groupId>io.inji.verify</groupId>
    <artifactId>verify-service</artifactId>
    <version>${verify-service.version}</version>
</dependency>
```

**Repositories:**

Some transitive dependencies are not available on Maven Central. Ensure the following repositories are configured in your `pom.xml` or `settings.xml`:

- **Sonatype (INJI snapshots)** — for `io.inji` artifacts (`vcverifier-jar`, `pixelpass-jar`)
- **Danubetech** — for `ld-signatures-java` and `jsonld-common-java`
- **Google Maven** — for `com.android.identity:identity-credential`

Maven will automatically resolve all transitive dependencies from the published POM.
