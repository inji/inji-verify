[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=develop&project=mosip_inji-verify&id=mosip_inji-verify&metric=alert_status)](https://sonarcloud.io/dashboard?branch=develop&id=mosip_inji-verify)
# Inji Verify

Injiverify is a web interface to verify the validity of the QR / credential using a browser from smartphone / tablet / computer. A user should be able to do primariliy 4 key actions - Scan, Validate, Fetch, Display.

# Contents:

This document contains the following sections:

- Installations
- Folder Structure
- Developer Setup
- Demo Setup

---

# Installations:

Prerequisites:

- **JAVA 21**

  Can be installed using [sdkman](https://sdkman.io/). Run following commands to install node

  ```shell
  $ curl -s "https://get.sdkman.io" | bash
  $ sdk install java 21.0.5-tem
  ```
- [Maven](https://maven.apache.org/install.html) 

# Folder Structure:

Once the repo is cloned, following folders can be found under the inji-verify repository folder:

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

Once the repo is cloned, move into the inji-verify repository folder and run the following command to check out to the develop branch:

```shell
cd inji-verify # move into the repository folder
git checkout develop
```

### Development server:

To get a development server up and running, run the following commands:

```shell
mvn clean
mvn spring-boot:run
```

### Run Docker Image:

(Note: Make sure that the following commands are run in the directory where Dockerfile is present)

Run the following commands to build and test the application as docker images

```shell
mvn -U -B package
docker build -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-service-dev <dockerImageName>:<tag>
```

Inji verify backend is designed to run in local with in memory H2 DB also we have another spring profile to do same. This can
be controlled by passing `active_profile_env` environment variable while building the docker image

```shell
mvn -U -B package
docker build --build-arg active_profile=local -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-service-dev <dockerImageName>:<tag>
```

To build the Docker image locally, use the following command. Ensure you are in the directory containing the Dockerfile:

```shell
docker build -t inji-verify-service:local
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