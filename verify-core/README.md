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
