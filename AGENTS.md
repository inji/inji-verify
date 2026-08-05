# AGENTS.md

This file provides guidance for AI coding assistants working with code in this repository.

## Repository Overview

Inji Verify is a web application for verifying Verifiable Credentials (VCs) via QR code scan/upload and the OpenID4VP protocol. The repo contains three independently deployable components:

- **`verify-ui/`** — React/TypeScript frontend (Node 18)
- **`verify-service/`** — Spring Boot 3.4.10 backend (Java 21, Maven)
- **`inji-verify-sdk/`** — React component library published as `@injistack/react-inji-verify-sdk`

Supporting directories: `docker-compose/` (local full-stack), `db_scripts/` (PostgreSQL), `api-test/`, `ui-test/`.

Documentation: `docs/technical_docs/` — Markdown docs for each VC format, OpenID4VP flow, SDK, and API overview. `docs/stoplight_docs/api-documentation-openapi.yaml` — OpenAPI 3.0 spec.

---

## Commands

### verify-ui (frontend)

```bash
cd verify-ui
npm install
npm start          # generates env.config.js from .env, then runs react-app-rewired start
                   # NOTE: prestart runs all tests first; use react-app-rewired start directly to skip
npm test           # jest with coverage and snapshot updates
npm run test:ci    # jest in CI mode (used by prestart/prebuild)
npm run build      # runs tests then react-app-rewired build
```

To skip the prestart test run during development (while still generating `env.config.js`):
```bash
cd verify-ui && ./configure_start_local.sh && react-app-rewired start
```

Run a single test file:
```bash
cd verify-ui && npx jest src/__tests__/path/to/file.spec.tsx
```

### verify-service (backend)

The default `application.properties` uses HSQLDB in-memory. PostgreSQL config is present but commented out — use it for production or when you need persistence.

```bash
cd verify-service
mvn spring-boot:run                          # run with HSQLDB in-memory (default, no DB setup needed)
mvn spring-boot:run -Dspring.profiles.active=local  # same HSQLDB profile explicitly
mvn test                                     # run all tests
mvn test -Dtest=VPRequestControllerTest      # run a single test class
mvn -U -B package                            # build jar
```

### inji-verify-sdk

```bash
cd inji-verify-sdk
npm install
npm test           # installs peer deps (React 18), then runs jest
npm run build      # webpack bundle + TypeScript declarations into dist/
```

---

## Architecture

- **Frontend (verify-ui)**: React/TypeScript app supporting three verification methods — file **UPLOAD**, camera **SCAN**, and **VERIFY** (OpenID4VP). Redux-managed state, theme-driven UI.
  - **UPLOAD/SCAN** verify a credential the user already holds as a QR code: the frontend decodes the QR and posts the VC directly to `/v2/vc-verification` for synchronous verification.
  - **VERIFY (OpenID4VP 1.0)** orchestrates a live request-and-present exchange with a wallet on a separate device, rather than checking a credential the user already has — see the OpenID4VP flow below.
- **Backend API (verify-service)**: Spring Boot service exposing VC verification and OpenID4VP endpoints (session/request creation, wallet submission, result retrieval, DID document). Legacy v1 endpoints exist alongside current v2 endpoints.

All paths below are relative to the server's context path, `/v1/verify` by default (`server.servlet.context-path`, set via `verify.context-path` in `application.properties`) — e.g. `POST /v2/vc-verification` is actually `POST /v1/verify/v2/vc-verification`.

| Endpoint | Description |
|---|---|
| `POST /v2/vc-verification` | Verify a VC (JSON body) |
| `POST /v2/vp-session-request` | Create OpenID4VP session; sets HttpOnly `transaction_id` cookie |
| `POST /v2/vp-request` | Create VP request without session cookie (server-to-server) |
| `GET /vp-request/{requestId}/status` | Long-poll for VP submission status (`DeferredResult`) |
| `GET /v2/vp-request/{requestId}` | Fetch VP request as signed JWT (`application/oauth-authz-req+jwt`) |
| `POST /v2/vp-submission/direct-post` | Wallet submits DCQL `vp_token` (form-encoded; no `presentation_submission`) |
| `POST /vp-session-results` | Fetch final verification result using `transaction_id` cookie |
| `POST /v2/vp-results/{transactionId}` | Fetch VP result by transaction ID (server-to-server) |
| `GET /did.json` | DID Web document |
| `POST /vc-submission` | Legacy v1: submit a standalone VC for later result retrieval |
| `POST /vc-verification` | Legacy v1: verify a VC (raw string body + `Content-Type` header) |
| `GET /vp-result/{transactionId}` | Legacy v1: fetch VP result by transaction ID; falls back to VC-submission lookup if no VP request exists |

- **OpenID4VP flow**:
  1. Frontend POSTs to `/v2/vp-session-request` → receives `requestId` and `transaction_id` HttpOnly cookie
  2. Frontend displays QR code containing the `openid4vp://` authorization request URI
  3. Frontend long-polls `GET /vp-request/{requestId}/status`
  4. Wallet scans QR, fetches request JWT from `GET /v2/vp-request/{requestId}`, POSTs `vp_token` to `/v2/vp-submission/direct-post`
  5. Backend resolves the pending request and notifies the frontend
  6. Frontend POSTs to `/vp-session-results` to fetch the final result
- **Supported VC formats**: `ldp_vc`, `dc+sd-jwt`, `vc+sd-jwt`.
- **Database**: PostgreSQL in production (schema/migrations in `db_scripts/`, `db_upgrade_script/`); HSQLDB in-memory for local/default runs.
- **SDK (inji-verify-sdk)**: React component library (`@injistack/react-inji-verify-sdk`) wrapping the verification UX for embedding in other apps; consumed by `verify-ui` itself.
- **Local full-stack setup**: `docker-compose/` runs the whole stack together; note that OpenID4VP requires the backend to be reachable from a wallet on a separate device (publicly accessible URL or tunnel).
