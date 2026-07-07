# Inji Verify

React/TypeScript frontend for verifying Verifiable Credentials via QR code scan/upload and OpenID4VP.

# Contents:

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

Configuration is passed via the `.env` file inside the **verify-ui** folder. At startup, `npm start` generates `public/env.config.js` from this file and injects values as `window._env_`.

For a full description of all environment variables see [docs/technical_docs/Inji_Verify_API_Overview.md](../docs/technical_docs/Inji_Verify_API_Overview.md).

---

# Developer Setup:

Once the repo is cloned, move into the inji-verify repository folder and run the following command to check out to the `release-1.0.x` branch:

```shell
cd inji-verify # move into the repository folder
git checkout release-1.0.x
cd verify-ui # contains source code and Dockerfile
```

### Development server:

To get a development server up and running, run the following commands:

```shell
npm install
npm start   # NOTE: prestart runs all tests first
```

To skip the prestart test run during development:

```shell
react-app-rewired start
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
docker build -t inji-verify:local .
```

Stop and delete the docker containers using the following commands:

```shell
docker stop inji-verify-dev
docker rm inji-verify-dev
```

# Demo Setup:

This section helps to quickly get started with a demo of the Inji Verify application.

Once the repository is cloned, move into the inji-verify repository directory and check out the desired branch or tag:

```shell
cd ./inji-verify # repository folder
git checkout branchName/tagname
```

To start the application, run the following commands:

```shell
$ cd ./verify-ui # source code folder
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

This section contains some common problems that could occur during the setup and steps to resolve them:

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
