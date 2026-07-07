[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.0.x&project=inji_inji-verify-verify-service&id=inji_inji-verify-verify-service&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.0.x&id=inji_inji-verify-verify-service)
# Inji Verify

Inji Verify is a web application for verifying Verifiable Credentials (VCs) via QR code scan/upload and the OpenID4VP protocol.

The repository contains three independently deployable components:

- **`verify-ui/`** — React/TypeScript frontend (Node 18)
- **`verify-service/`** — Spring Boot backend (Java 21, Maven)
- **`inji-verify-sdk/`** — React component library (`@injistack/react-inji-verify-sdk`)

# Contents:

This document contains the following sections:

- Installations
- Folder Structure
- Developer Setup
- Demo Setup

---

# Installations:

Prerequisites (per component):

- **verify-ui / inji-verify-sdk** — Node 18

  Can be installed using [nvm](https://github.com/nvm-sh/nvm):

  ```shell
  $ curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
  $ nvm install 18
  ```

- **verify-service** — Java 21

  Can be installed using [sdkman](https://sdkman.io/):

  ```shell
  $ curl -s "https://get.sdkman.io" | bash
  $ sdk install java 21.0.5-tem
  ```

- [Maven](https://maven.apache.org/install.html) (for verify-service)

# Folder Structure:

Once the repository is cloned, following folders can be found under the inji-verify repository folder:

- **api-test:** contains the API automation tests
- **db_scripts:** contains the database scripts for the Inji Verify application
  - sql (contains SQL scripts for database operations)
  - [Readme.md](./db_scripts/README.md)
- **db_upgrade_script:** contains the database upgrade and rollback scripts
  - sql (contains SQL scripts for database upgrade and rollback)
  - [Readme.md](./db_upgrade_script/inji_verify/README.md)
- **deploy:** folder contains deployment scripts required to deploy on K8S
- **docker-compose** : folder containing setup for docker compose
  - config
  - db-init
  - docker-compose.yml
  - [Readme.md](./docker-compose/README.md)
- **docs** : contains the flow, OpenAPI documentation for the Inji Verify application
- **helm:** folder contains helm charts required to deploy on K8S
- **inji-verify-sdk:** contains the Inji Verify SDK
  - src (source code)
  - [Readme.md](./inji-verify-sdk/README.md)
- **ui-test:** contains the ui automation tests
- **utilities:** folder contains sample QR code variation generation utility for testing
- **verify-service:** contains source code for the verify backend service
  - src (source code)
  - Dockerfile
  - [Readme.md](./verify-service/README.md)
- **verify-ui:** contains the application source code for web UI, Dockerfile and docker-compose.yml files
  - src (source code)
  - Dockerfile
  - [Readme.md](./verify-ui/README.md)
---

# Developer Setup:

Once the repo is cloned, move into the inji-verify repository folder and run the following command to check out to the `release-1.0.x` branch:

```shell
cd inji-verify # move into the repository folder
git checkout release-1.0.x
```

### verify-ui (frontend)

```shell
cd verify-ui
npm install
npm start          # generates env.config.js from .env, then runs the app
                   # NOTE: prestart runs all tests first; skip with: react-app-rewired start
```

### verify-service (backend)

The default profile uses HSQLDB in-memory — no database setup needed locally.

```shell
cd verify-service
mvn spring-boot:run                          # run with HSQLDB in-memory (default)
mvn spring-boot:run -Dspring.profiles.active=local  # same, explicitly
```

### Run Docker Image (verify-service):

(Note: Make sure that the following commands are run in the directory where Dockerfile is present)

```shell
mvn -U -B package
docker build -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-service-dev <dockerImageName>:<tag>
```

To build with the local HSQLDB profile:

```shell
mvn -U -B package
docker build --build-arg active_profile=local -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-service-dev <dockerImageName>:<tag>
```

To build the Docker image locally, use the following command. Ensure you are in the directory containing the Dockerfile:

```shell
docker build -t inji-verify-service:local .
```

Stop and delete the docker containers using the following commands:

```shell
docker stop inji-verify-service-dev
docker rm inji-verify-service-dev
```

# Demo Setup:

This section helps to quickly get started with a demo of the Inji Verify application

Once the repository is cloned, move into the inji-verify repository directory.

```shell
cd ./inji-verify # repository folder
git checkout branchName/tagname
```

## [Deployment in K8 cluster](deploy/README.md)
