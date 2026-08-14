# Inji Verify – Docker Compose Setup

A clean and structured guide to run **Inji Verify** locally using Docker Compose, including OpenID4VP flows and local SDK setup.

---

# 🧱 Architecture Overview

## OPENID4VP

![SETUP](<mermaid-diagram.png>)

## OPENID4VC

---

# ⚙️ Prerequisites

## Docker

* Install Docker from: [https://docs.docker.com/engine/install/](https://docs.docker.com/engine/install/)

## Docker Compose

> Included by default in Docker Desktop (Windows/Mac)

Install separately if needed:

* Plugin: [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)
* Standalone: [https://docs.docker.com/compose/install/](https://docs.docker.com/compose/install/)

---

# 🚀 Quick Start

```bash
cd docker-compose

docker compose up -d
```

## Access:

* UI → [http://localhost:3000](http://localhost:3000)
* API → [http://localhost:8080/v1/verify/swagger-ui/index.html](http://localhost:8080/v1/verify/swagger-ui/index.html)

---

# 🔐 OpenID4VP Configuration

Located in: `config/`

## Example

```json
{
  "logo": "/assets/cert.png",
  "name": "Health Insurance",
  "clientIdPrefix": "decentralized_identifier",
  "purpose": "Relying party is requesting your digital ID for the purpose of Self-Authentication",
  "dcqlQuery": {
    "credentials": [
      {
        "id": "health_insurance_credential_id",
        "format": "ldp_vc",
        "meta": {
          "type_values": [
            ["https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential"]
          ]
        }
      }
    ]
  }
}
```

## Key Fields

* `logo` → Display image
* `name` → Credential name
* `essential` → Required or optional
* `clientIdPrefix`

  * `decentralized_identifier` → Uses `request_uri` (DID-based signed JWT)
  * `pre_registered` → Embedded request; wallet must have `inji-verify-ui` registered as trusted verifier
* `dcqlQuery` → DCQL credential query (`type_values` for `ldp_vc`, `vct_values` for SD-JWT)

---

# 👛 Web Wallet Configuration

File: `config/config.json`

```json
{
  "WebWallets": [
    {
      "id": "inji-wallet",
      "name": "Inji Wallet",
      "iconUrl": "/assets/inji-web-wallet-icon.svg",
      "walletBaseUrl": "https://injiweb.dev.mosip.net"
    }
  ]
}
```

## ⚠️ Important

* Replace `walletBaseUrl` with your own wallet URL if needed
* Set to empty string to disable the wallet button

---

# 🌐 Localhost Proxy Setup

Required for mobile / cross-device flows.

## Why?

Mobile devices cannot access `localhost`.

## Solution:

```bash
ngrok http 3000
```

Example:

```
https://abc123.ngrok.app → http://localhost:3000
```

## Update docker-compose.yml

Replace:

```
VERIFY_SERVICE_PROXY_FOR_LOCALHOST
```

With:

```
abc123.ngrok.app
```

---

# 📱 Flows

## Cross Device Flow

To test the cross-device flow on a mobile or tablet device, scan the VP request QR code directly. For credentials with `clientIdPrefix` set to `pre_registered`, the wallet cannot share the VC unless the locally running Verify application is registered as a trusted verifier. For credentials with `clientIdPrefix` set to `decentralized_identifier`, the wallet can share the VC. For `pre_registered`, add the client ID to `mimoto-trusted-verifiers.json`, which Inji Wallet uses as its trusted verifier list.

### Behavior:

* `decentralized_identifier` → Works directly
* `pre_registered` → Needs trusted verifier config

---

## Same Device Flow

To test the Same Device flow on your mobile / tablet device, hit the URL https://proxyurl.ngrok.app. This will open the app.

> **Note:** VP submission is disabled by default (`VP_SUBMISSION_SUPPORTED=false`). Set it to `true` in `docker-compose.yml` to enable the OpenID4VP tab.

---

# 🐳 Docker Commands

## Start

```bash
docker compose up -d
```

## Stop

```bash
docker compose down
```

## Reset (with volumes)

```bash
docker compose down -v
```

## Logs

```bash
docker compose logs -f
```

---

# 🛠 Local Development

## 1. Enable Local Build

```yaml
verify-service:
  #image: injistackdev/inji-verify-service:develop  
  build:
    context: ../verify-service
  image: inji-verify-service:local    
verify-ui:
  #image: injistackdev/inji-verify-ui:develop
  build:
    context: ../verify-ui
  image: inji-verify-ui:local    
```

---

## 2. Build verify-service locally first

`verify-service`'s Dockerfile does **not** run Maven — it only packages a jar that must already
exist in `verify-service/target/`. Unlike `verify-ui` (whose Dockerfile builds the React app from
source), `docker compose build` will silently reuse whatever's already in `target/` if you skip
this step, including a stale jar from before your latest changes.

> **Never `cd verify-service` and build it standalone.** It depends on the sibling `verify-core`
> module; building it alone resolves `verify-core` from your `~/.m2` cache instead of current
> source, which silently uses a **stale** `verify-core` jar if it's changed since your last
> install — producing confusing `NoClassDefFoundError`s at container startup that don't reproduce
> locally. Always build from the repo root.

```bash
cd inji-verify   # repo root
mvn clean install -Dgpg.skip   # builds + installs verify-core and verify-service together
```

## 3. Clear Cache and Start Docker Compose

```bash
cd docker-compose
docker compose build --no-cache
docker compose up
```

---

# 🧪 Testing

Open:

```
https://<ngrok-url>
```

---

# ⚡ Tips

* Use `--no-cache` for fresh builds
* Hard refresh browser if needed
* If ngrok does not work or gives CORS error, you can try with localtunnel or any other proxy
* If stuck:

```bash
docker compose down --volumes --remove-orphans
# Last resort only: this removes unused Docker resources across your machine.
docker system prune -a --volumes
```

---

# ✅ You're Ready

You now have:

* Local Verify UI + Service
* Wallet integration
* Cross-device support

---
