# Inji Verify – Docker Compose Setup

- **Docker**

  - [Install on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
  - [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
  - [Other platforms](https://docs.docker.com/engine/install/)

- **Docker Compose**

  `Note: Requires installation of Docker. This step can be skippped if Docker desktop(Windows) is installed as it comes along with docker compose. Please install Docker using above links before proceeding for the installation of docker compose`

  - [Install as plugin to docker command](https://docs.docker.com/compose/install/#scenario-two-install-the-compose-plugin)
  - [Install the Compose standalone](https://docs.docker.com/compose/install/#scenario-three-install-the-compose-standalone)

Once installed, use Docker compose option below to run the Inji Verify application for a quick demo.

---

## Setup


### OpenID4VP config

The configuration file can be found under `config` directory.

**Example Configuration Explanation**

Let's look at the "MOSIP ID" example to understand how these properties work together:

```json
{
  "logo": "/assets/cert.png",
  "name": "MOSIP ID",
  "type": "MOSIPVerifiableCredential",
  "essential": true,
  "clientIdScheme":"did",
  "definition": {
    "purpose": "Relying party is requesting your digital ID for the purpose of Self-Authentication",
    "format": {
      "ldp_vc": {
        "proof_type": [
          "RsaSignature2018"
        ]
      }
    },
    "input_descriptors": [
      {
        "id": "id card credential",
        "format": {
          "ldp_vc": {
            "proof_type": [
              "RsaSignature2018"
            ]
          }
        },
        "constraints": {
          "fields": [
            {
              "path": [
                "$.type"
              ],
              "filter": {
                "type": "object",
                "pattern": "MOSIPVerifiableCredential"
              }
            }
          ]
        }
      }
    ]
  }
}
```
`logo`: The image /assets/cert.png will be shown on the credential selection panel.

`name`: The name that has to be shown on the credential selection panel.

`type`: Internally, this configuration is used to identify what are the different types of credential.

`essential`: This credential is required for the verification to succeed.

`clientIdScheme: did`: The corresponding VP request will use `client_id_scheme` as `DID` and Auth Request will be available to wallet via Request_Uri within the VP request.

`clientIdScheme: pre_registered`: The corresponding VP request will use `client_id_scheme` as `pre_registered` and Auth Request will be available to wallet directly within the VP request.

`definition` : The presentation definition for the particular type of credential. For more details check [[DIF.PresentationExchange]](https://identity.foundation/presentation-exchange/spec/v2.0.0/)

---

### OpenID4VP Setting Up Proxy For Localhost

To get the OpenID4VP flow working locally, use a proxy service like ngrok or localtunnel 
to create a proxy url like https://proxyurl.ngrok.app for http://localhost:3000.

This is required since wallet running on your mobile / tablet device, will not be able to invoke the http://localhost:3000 url,
while sharing the credentials.

#### In docker-compose.yml file replace `VERIFY_SERVICE_PROXY_FOR_LOCALHOST` with `proxyurl.ngrok.app`. 
Save the `docker-compose.yml` file.

### Cross Device Flow

To test the Cross Device flow on your mobile / tablet device, scan the VP request QR code directly.
For Credentials which use `client_id_scheme` as`pre_registered` in the VP request, the wallet will not be able to share the VC since
your locally running Verify application will not be pre registered with the wallet. 
For other Credentials which use `client_id_scheme` as `DID` in the VP request, the wallet will be able to share the VC. 
For `pre_registered`, we should add our client_id into `mimoto-trusted-verifiers.json` which is referred by Inji Wallet.

### Same Device Flow

To test the Same Device flow on your mobile / tablet device, hit the URL https://proxyurl.ngrok.app. 
This will open app. 

---

## Run Using Docker Compose:

Navigate to the docker-compose directory:

```shell
cd docker-compose
```

> Make sure ports 3000, 8080, and 5432 are free.

Run the following command to build and start all services:

```shell
docker-compose up -d # if docker compose is installed as a standalone command.
docker compose up -d # if docker compose is installed as a plugin to docker command
```

This will start:

* verify-service (backend)
* verify-ui (frontend)
* postgres (database)

The UI will be accessible at: http://localhost:3000

API (verify-service) swagger runs at: http://localhost:8080/v1/verify/swagger-ui/index.html

To stop the application, run the following command:

```shell
docker-compose down # if docker compose is installed as a standalone command.
docker compose down # if docker compose is installed as a plugin to docker command
```

To remove volumes as well (clean reset):

```shell
docker-compose down -v # if docker compose is installed as a standalone command.
docker compose down -v # if docker compose is installed as a plugin to docker command
```
---
### Troubleshooting

To check container logs:

```shell
docker-compose logs -f
```
