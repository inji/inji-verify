# Developer Guide — `@injistack/react-inji-verify-sdk`

This guide is for contributors who want to develop, test, or publish the SDK.
For integration instructions, see [README.md](./README.md).

---

## Prerequisites

- Node 18+
- npm 9+
- React 18 (peer dependency — installed automatically for tests, not for builds)

---

## Project Structure

```
inji-verify-sdk/
├── src/
│   ├── index.ts                        # Public exports
│   ├── components/
│   │   ├── qrcode-verification/        # QRCodeVerification component
│   │   └── openid4vp-verification/     # OpenID4VPVerification component
│   ├── types/                          # Shared type declarations
│   └── utils/                          # Shared utilities
├── __tests__/                          # All tests (mirrors src/ structure)
│   ├── components/
│   │   ├── qrcode-verification/
│   │   └── openid4vp-verification/
│   └── utils/
├── __mocks__/                          # Jest module mocks (e.g. CSS stub)
├── dist/                               # Build output (generated)
│   ├── index.js                        # UMD bundle
│   └── index.d.ts                      # TypeScript declarations
├── webpack.config.js                   # Bundle config (UMD, externalises React)
├── tsconfig.json                       # TypeScript config (strict, ES5 target)
└── package.json
```

Public exports are defined in `src/index.ts`. Any new component or type intended for consumers must be re-exported from there.

---

## Setup

```bash
cd inji-verify-sdk
npm install
```

This installs all dev dependencies. React 18 peer dependencies are **not** installed here — they are installed into `node_modules/.peer-test/` only when running tests.

---

## Building

```bash
npm run build
```

This runs in two stages:
1. **webpack** — bundles `src/index.ts` into `dist/index.js` (UMD format). React and React-DOM are externalised — they are not bundled.
2. **tsc** — emits TypeScript declaration files (`dist/index.d.ts` and friends).

The `prebuild` hook runs the full test suite before every build. To build without tests:

```bash
webpack --config webpack.config.js && tsc --declaration --emitDeclarationOnly --outDir dist
```

---

## Running Tests

```bash
npm test
```

This installs React 18 peer deps into a local sandbox (`node_modules/.peer-test/`), then runs Jest with coverage.

Run a single test file:

```bash
npx jest src/__tests__/path/to/file.test.tsx
```

Tests use `@testing-library/react`. Component tests should mock the backend API calls — do not rely on a live `verify-service` for unit tests.

---

## Local Integration Testing

To test SDK changes inside `verify-ui` (or any other consumer) before publishing:

### Option A — Local npm registry (Verdaccio)

```bash
# Start Verdaccio (if not already running)
npx verdaccio

# Publish to local registry
npm run localPublish
```

`localPublish` bumps the patch version, builds, and publishes to `http://localhost:4873`. In the consumer app:

```bash
npm install @injistack/react-inji-verify-sdk --registry http://localhost:4873
```

### Option B — npm link

```bash
# In inji-verify-sdk/
npm run build
npm link

# In the consumer app (e.g. verify-ui/)
npm link @injistack/react-inji-verify-sdk
```

Rebuild the SDK (`webpack --config webpack.config.js && tsc ...`) after each change — `npm link` points to `dist/`, not `src/`.

---

## Adding a Component

1. Create a directory under `src/components/your-component/`.
2. Export the component as a default export from its main file.
3. Add the export to `src/index.ts`:
   ```ts
   export { default as YourComponent } from './components/your-component/YourComponent';
   ```
4. Export any public types alongside it:
   ```ts
   export type { YourComponentProps } from './components/your-component/YourComponent.types';
   ```
5. Add tests under `__tests__/components/your-component/`.

---

## TypeScript Notes

- `strict: true` is enforced — no implicit `any`, no unchecked nulls.
- Target is `ES5` (for broad browser compatibility). Use Babel transforms for modern syntax — they are configured in Jest via `babel.config.js`.
- Declaration files are emitted by `tsc --declaration --emitDeclarationOnly`; webpack handles the bundle separately.
- `skipLibCheck: true` is set — type errors in `node_modules` are ignored.

---

## Publishing (npm)

```bash
npm run build
npm publish --access public
```

Ensure the version in `package.json` is bumped before publishing. The package is scoped to `@injistack` — confirm you are authenticated with the correct npm org before running `npm publish`.

---

## Support

- Backend setup: see the [`verify-service` README](../verify-service/README.md)
- Full API reference: see [README.md](./README.md) and [docs/technical_docs/OpenID4VP_Inji_Verify_SDK.md](../docs/technical_docs/OpenID4VP_Inji_Verify_SDK.md)
- OpenID4VP spec: [openid.net/specs/openid-4-verifiable-presentations-1_0.html](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)
