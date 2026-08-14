# Inji Verify Backend Service

`verify-service` is the standalone Spring Boot app: controllers, exception-to-HTTP-response
mapping, and app bootstrapping. The framework-agnostic domain logic (DTOs, validation, DCQL/VP
business logic, persistence) lives in [`verify-core`](../verify-core/README.md), which
`verify-service` depends on and which can also be embedded directly by another Spring app — see
that module's README if you want to consume Inji Verify as a library rather than run it as a
service.

### Contents
* Setup Guide
* API docs

> For features, supported VC formats, OpenID4VP standards, and endpoint details see:
> - [docs/technical_docs/Inji_Verify_API_Overview.md](../docs/technical_docs/Inji_Verify_API_Overview.md)
> - [docs/technical_docs/VC_Verification.md](../docs/technical_docs/VC_Verification.md)
> - [docs/technical_docs/OpenID4VP-1.0.0.md](../docs/technical_docs/OpenID4VP-1.0.0.md)

##### Setup Guide

> **Never build `verify-service` standalone** (i.e. don't `cd verify-service` and run `mvn ...`
> directly). It depends on the sibling `verify-core` module; building it alone resolves
> `verify-core` from your `~/.m2` cache instead of from current source, which will silently use a
> **stale** `verify-core` jar if it's changed since your last install — leading to confusing
> `NoClassDefFoundError`s at runtime that don't reproduce with `mvn test`/`spring-boot:run`. Always
> build from the **repo root**.

```shell
cd inji-verify                        # repo root, not verify-service
mvn clean install -Dgpg.skip          # builds + installs verify-core and verify-service together
```

Run/test individual pieces from the repo root once that's built:

```shell
mvn -pl verify-service -am spring-boot:run                                # HSQLDB in-memory (default, no DB setup needed)
mvn -pl verify-service -am spring-boot:run -Dspring.profiles.active=local  # same, explicitly
mvn -pl verify-service test -Dtest=VPRequestControllerTest                # run a single test class
```

For PostgreSQL (production), apply the scripts in `db_scripts/` manually. `spring.jpa.hibernate.ddl-auto` is set to `none` for production profiles.

##### Docker

```shell
cd inji-verify                    # repo root
mvn clean install -Dgpg.skip
cd verify-service
docker build -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-service-dev <dockerImageName>:<tag>
```

To build with the local HSQLDB profile:

```shell
docker build --build-arg active_profile=local -t <dockerImageName>:<tag> .
```

Stop and delete containers:

```shell
docker stop inji-verify-service-dev
docker rm inji-verify-service-dev
```

##### API docs

The API docs are published on Stoplight: [Inji Verify API documentation](https://mosip.stoplight.io/docs/inji-verify/branches/main).
