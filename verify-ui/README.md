# Inji Verify

Injiverify is a web interface to verify the validity of the QR / credential using a browser from smartphone / tablet / computer. A user should be able to do primariliy 4 key actions - Scan, Validate, Fetch, Display.

# Contents:

This document contains the following sections:

- Installations
- Configuration
- Developer Setup
- Demo Setup
- Troubleshoot

---

# Installations:

Prerequisites:

- **Node 18**

  Can be installed using [nvm](https://github.com/nvm-sh/nvm). Run following commands to install node

  ```shell
  $ curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
  $ nvm install 18
  ```

- **Docker**

  - [Install on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
  - [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
  - [Other platforms](https://docs.docker.com/engine/install/)

# Configuration:

The configuration to the Inji Verify application can be passed using the .env file which is present inside the **verify-ui** folder.

It accepts INTERNET_CONNECTIVITY_CHECK_ENDPOINT and INTERNET_CONNECTIVITY_CHECK_TIMEOUT variables at this moment. These are used to check the availability of the internet connection and can be configured when required. The default values are added in the .env file.

---

# Developer Setup:

Once the repo is cloned, move into the inji-verify repository folder and run the following command to check out to the release-0.18.x branch:

```shell
cd inji-verify # move into the repository folder
git checkout release-0.18.x
cd verify-ui # contains source code and Dockerfile
```

### Development server:

To get a development server up and running, run the following commands:

```shell
npm install
npm start
```

### Run Docker Image:

(Note: Make sure that the following commands are run in the directory where Dockerfile is present)

Run the following commands to build and test the application as docker images

```shell
docker build -t <dockerImageName>:<tag> .
docker run -it -d -p 3000:8000 --env-file ./.env --name inji-verify-dev <dockerImageName>:<tag>
```

To build the Docker image locally, use the following command. Ensure you are in the directory containing the Dockerfile:

```shell
docker build -t inji-verify:local
```

Stop and delete the docker containers using the following commands:

```shell
docker stop inji-verify-dev
docker rm inji-verify-dev
```

# Demo Setup:

This section helps to quickly get started with a demo of the Inji Verify application

Once the repository is cloned, move into the inji-verify repository directory.
Choose one of the branches that are currently available for the demo:

release branches:

- release-0.8.x
- release-0.9.x
- release-0.10.x
- release-0.11.x
- release-0.12.x
- release-0.13.x
- release-0.14.x

tags :

- v0.14.0
- v0.13.0
- v0.12.3
- v0.11.1
- v0.11.0
- v0.10.0
- v0.9.0
- v0.8.1
- v0.8.0

active branches:

- master
- develop
- release-0.18.x

```shell
cd ./inji-verify # repository folder
git checkout branchName/tagname # choose from any of the above branches
```

To start the application, run the following commands:

```shell
$ cd ./inji-verify # source code folder
$ docker-compose up -d # if docker compose is installed as a standalone command.
$ docker compose up -d # if docker compose is installed as a plugin to docker command
```

The application is now accessible at http://localhost:3000.

Once the demo is done, cleanup using the following command:

```shell
$ docker-compose down # if docker compose is installed as a standalone command.
$ docker compose down # if docker compose is installed as a plugin to docker command
```

# Troubleshoot:

This section contains some common problems that could occur during the setup and steps to resolve then:

## Issue with starting docker compose:

```
no configuration file provided: not found
```

or

```
Can't find a suitable configuration file in this directory or any
parent. Are you in the right directory?

Supported filenames: docker-compose.yml, docker-compose.yaml, compose.yml, compose.yaml
```

### Solution:

Make sure that you are in the right directory `inji-verify/verify-ui` and the docker-compose.yml file is present in this directory.

Check using `ls` command in ubuntu terminal or `dir` command in windows command prompt for the contents of the current directory

## Issue with ports:

```
Error response from daemon: Ports are not available: exposing port TCP 0.0.0.0:3000 -> 0.0.0.0:0: listen tcp 0.0.0.0:80: bind: An attempt was made to access a socket in a way forbidden by its access permissions.
```

### Solution:

Try updating the port in the docker-compose.yml file from 3000:80 to <other_port>:80 and try again

## Issue with building docker image:

```
ERROR: failed to solve: failed to read dockerfile: no such file or directory
```

### Solution:

Make sure that you are in the right directory `inji-verify/verify-ui` and the Dockerfile is present in this directory.

Check using `ls` command in ubuntu terminal or `dir` command in windows command prompt for the contents of the current directory

## Issue with docker engine:

```
docker engine/socket not available
```

### Solution:

In Windows: Start/Restart Docker desktop application

In Ubuntu: Run the following command to make sure that the docker service is running

```shell
sudo systemctl restart docker.service
```
