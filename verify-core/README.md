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

> **⚠️ Security: override the default keystore before deploying anything.** `verify-core` ships
> with a sample keystore (`src/main/resources/sample-keystore/test.p12`, alias `test`) that
> `inji.keystore.file.path`/`inji.keystore.file.pass` point at by default. Its
> private key is bundled in the published jar, so it's **public to anyone who depends on
> `verify-core`** — it's only there so the module works out of the box for local development.
> Any real deployment **must** override both properties with its own privately-held keystore, or
> anyone can forge valid `did:web` VP requests appearing to come from you.
>
> The keystore must hold an **Ed25519** key — RSA and EC keys aren't supported today. Adding
> support for other algorithms would need code changes.
>
> **If you're deploying with `client_id` using the `x509_san_dns` scheme**, two things must agree
> with each other: your own keystore's certificate must carry a Subject Alternative Name
> (`dNSName`) equal to your deployment's declared identity, and the `inji.verify.x509-san-dns.host`
> property must be set to that same value. Neither is derived from the other automatically — the
> DNS name is baked into the certificate itself, so a different identity needs a
> differently-issued certificate *and* an updated property. The bundled sample keystore's cert has
> SAN `test.example.com`, matching the property's default; deploying to a real domain without
> regenerating the keystore *and* updating the property will make every `x509_san_dns` request
> fail validation (by design — a real wallet would reject the mismatch too). This only applies if
> you use `x509_san_dns`; `decentralized_identifier` (kid) mode doesn't touch the keystore's cert
> at all and works with just an env var change to `inji.did.verify.uri`.
>
> **Certificate expiry is not silently ignored.** Every time a request JWT is signed for an
> `x509_san_dns` client_id, the signing certificate's validity window is checked
> (`X509Certificate.checkValidity()`); an expired or not-yet-valid certificate fails the request
> with a clear log entry instead of quietly issuing a JWT no compliant wallet would trust. There's
> no automatic rotation — when the configured certificate is approaching (or has passed) its
> `notAfter` date, the Relying Party must issue a new certificate for the same key (or a new key
> pair, updating the keystore) before it expires, and update the keystore file the deployment
> points at. Watch the leaf cert's expiry date as part of normal deployment monitoring; there is
> currently no built-in expiry warning ahead of the hard failure.
>
> **HTTPS is enforced for `request_uri` outside local/dev.** Since `request_uri` serves a signed
> JWT the wallet trusts implicitly (no separate DID-resolution step to catch tampering in x5c
> mode), `inji.vp-submission.base-url` must use `https` for any `x509_san_dns` request unless its
> host is a loopback address (`localhost`, `127.0.0.1`, `::1`, `0.0.0.0`) — so local development
> keeps working over plain HTTP. This check does not apply to `decentralized_identifier` (kid)
> requests, to avoid changing behavior for existing deployments of that scheme.
