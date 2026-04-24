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
```
{
      "logo": "/assets/cert.png",
      "name": "Health Insurance",
      "type": "InsuranceCredential",
      "clientIdScheme":"pre_registered",
      "definition": {
        "purpose": "Relying party is requesting your digital ID for the purpose of Self-Authentication",
        "format": {
          "ldp_vc": {
            "proof_type": ["Ed25519Signature2018"]
          }
        },
        "input_descriptors": [
          {
            "id": "id card credential",
            "format": {
              "ldp_vc": {
                "proof_type": ["Ed25519Signature2020"]
              }
            },
            "constraints": {
              "fields": [
                {
                  "path": ["$.type"],
                  "filter": {
                    "type": "object",
                    "pattern": "InsuranceCredential"
                  }
                }
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
* `type` → Credential identifier
* `essential` → Required or optional
* `clientIdScheme`

  * `did` → Uses request_uri
  * `pre_registered` → Embedded request
* `definition` → Presentation Exchange spec

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
      "walletBaseUrl": "http://localhost:3001"
    }
  ]
}
```

## ⚠️ Important

Default wallet URL may fail if unreachable.

### Options:

* Use hosted wallet
* Run locally → `http://localhost:3001`
* Remove entry to disable

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

To test the cross-device flow on a mobile or tablet device, scan the VP request QR code directly. For credentials with `client_id_scheme` set to `pre_registered`, the wallet cannot share the VC unless the locally running Verify application is registered as a trusted verifier. For credentials with `client_id_scheme` set to `did`, the wallet can share the VC. For `pre_registered`, add the client ID to `mimoto-trusted-verifiers.json`, which Inji Wallet uses as its trusted verifier list.

### Behavior:

* `did` → Works directly
* `pre_registered` → Needs trusted verifier config
---

## Same Device Flow

To test the Same Device flow on your mobile / tablet device, hit the URL https://proxyurl.ngrok.app. This will open app.

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

## 2. Clear Cache and Start Docker Compose

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
or
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
