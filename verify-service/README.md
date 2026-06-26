# Inji Verify Backend Service

### Contents
* Setup Guide
* API docs

> For features, supported VC formats, OpenID4VP standards, and endpoint details see:
> - [docs/technical_docs/Inji_Verify_API_Overview.md](../docs/technical_docs/Inji_Verify_API_Overview.md)
> - [docs/technical_docs/VC_Verification.md](../docs/technical_docs/VC_Verification.md)
> - [docs/technical_docs/OpenID4VP-1.0.0.md](../docs/technical_docs/OpenID4VP-1.0.0.md)

##### Setup Guide

```shell
cd verify-service
mvn spring-boot:run                                      # HSQLDB in-memory (default, no DB setup needed)
mvn spring-boot:run -Dspring.profiles.active=local       # same, explicitly
mvn test                                                 # run all tests
mvn test -Dtest=VPRequestControllerTest                  # run a single test class
mvn -U -B package                                        # build jar
```

For PostgreSQL (production), apply the scripts in `db_scripts/` manually. `spring.jpa.hibernate.ddl-auto` is set to `none` for production profiles.

##### Docker

```shell
mvn -U -B package
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
