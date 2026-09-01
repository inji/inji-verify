# verify-core

Framework-agnostic domain layer for Inji Verify: DTOs, validation, DCQL/VP business logic and
persistence, with no servlet/web dependency.

`verify-core` holds everything `verify-service` (the standalone Spring Boot app) needs except the
HTTP layer — controllers, exception-to-HTTP-response mapping, and app bootstrapping stay in
`verify-service`. This split lets `verify-core` be embedded directly by another Spring application
that wants OpenID4VP verification without running `verify-service` as a separate process.

### Contents

- Package layout
- Build & test
- Using verify-core as a library dependency

##### Package layout

```
io.inji.verify
├── config       # non-web Spring configuration (bean wiring)
├── dto          # request/response and internal data objects
├── enums        # shared enumerations
├── exception    # domain exceptions
├── key          # keystore/key-extraction abstractions
├── models       # JPA entities
├── repository   # Spring Data repositories
├── serialization
├── services     # business logic (VP/VC verification, DID generation, etc.)
├── shared
├── utils
└── validator
```

##### Build & test

```shell
mvn -pl verify-core -am install -Dgpg.skip     # build verify-core (and its dependency modules)
mvn -pl verify-core test                       # run verify-core's tests only
```

`verify-core` is a plain `jar` module — it has no `spring-boot-maven-plugin` repackage step and
isn't independently runnable; it's always consumed by an application module (`verify-service`, or
your own).

##### Using verify-core as a library dependency

`verify-core` publishes a plain JAR that can be consumed by other Maven projects wanting to embed
OpenID4VP verification.

**Dependency:**

```xml
<dependency>
    <groupId>io.inji.verify</groupId>
    <artifactId>verify-core</artifactId>
    <version>${verify-core.version}</version>
</dependency>
```

**Repositories:**

Some transitive dependencies are not available on Maven Central. Ensure the following repositories
are configured in your `pom.xml` or `settings.xml`:

- **Sonatype (INJI snapshots)** — for `io.inji` artifacts (`vcverifier-jar`, `pixelpass-jar`)
- **Danubetech** — for `ld-signatures-java` and `jsonld-common-java`
- **Google Maven** — for `com.android.identity:identity-credential`

Maven will automatically resolve all transitive dependencies from the published POM.

A consuming Spring application needs its own `@ComponentScan`/`@EntityScan` to cover the
`io.inji.verify` base package (or an equivalent explicit scan) for `verify-core`'s beans and JPA
entities to be picked up.

> **Note:** The consuming application **must be a Spring MVC app** (i.e. have `DispatcherServlet`
> active). `VerifiablePresentationRequestService`'s long-polling VP status endpoint uses Spring's
> `DeferredResult`, which only resolves/times out correctly when driven by `DispatcherServlet`/
> `WebAsyncManager` inside an active HTTP request. Outside that context — a non-web app, a
> scheduled job, or a plain `ApplicationContext` — `DeferredResult#onTimeout()` never fires.
> `DeferredResult` has no timer of its own, so the call will simply hang instead of timing out.

### ⚠️ Production deployment

**Replace the default keystore before deploying to any real environment.**

`verify-core` includes a sample keystore (`src/main/resources/sample-keystore/test.p12`) for local development. Its private key is included in the published JAR and is therefore **not secure for production use**.

Configure `inji.keystore.file.path` and `inji.keystore.file.pass` to point to your own privately-held keystore. Otherwise, attackers could forge valid VP requests that appear to come from your deployment.

The keystore must contain an **Ed25519 key**. RSA and other EC keys are not currently supported.

If using `x509_san_dns`:

- The certificate must contain a `dNSName` SAN matching `inji.verify.x509-san-dns.host`.
- The certificate must be valid (not expired or not-yet-valid).
- `inji.vp-submission.base-url` must use **HTTPS** in non-local environments.
- If the wallet does not already trust your certificate, use the same host for `inji.vp-submission.base-url` and `inji.verify.x509-san-dns.host`.
- If the keystore has no certificate chain configured (or an empty one), request creation fails immediately with `CLIENT_ID_CERTIFICATE_CHAIN_MISSING` — you won't get a `requestUri` a wallet could later fail to fetch.

Certificates are **not rotated automatically**, so renew the certificate and update the configured keystore before it expires.