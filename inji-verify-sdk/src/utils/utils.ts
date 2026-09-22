import {VALID_SD_JWT_TYPES, DC_API_PROTOCOL, DEFAULT_DC_API_TIMEOUT_MS, CLIENT_ID_PREFIX_DECENTRALIZED_IDENTIFIER, CLIENT_ID_PREFIX_X509_SAN_DNS, CLIENT_ID_PREFIX_REDIRECT_URI, MIN_CHROME_DC_API_VERSION} from "./constants";
import {CredentialResult, VCVerificationV2Response} from "../components/qrcode-verification/QRCodeVerification.types";

/** Max delay accepted by `window.setTimeout` (signed 32-bit int). */
const MAX_SET_TIMEOUT_MS = 2_147_483_647;

/** Finite positive ms, floored, at least 1ms, and capped; otherwise the 5-minute default. */
export const normalizeDcApiTimeoutMs = (value: number | undefined): number => {
  if (typeof value !== "number" || !Number.isFinite(value) || value <= 0) {
    return DEFAULT_DC_API_TIMEOUT_MS;
  }
  return Math.min(Math.max(Math.floor(value), 1), MAX_SET_TIMEOUT_MS);
};

export const isMobileDevice = (): boolean => {
  const userAgent = navigator.userAgent;

  const isMobileUA = /Android.*Mobile|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    userAgent
  );

  const isTabletUA =
    /iPad/i.test(userAgent) ||
    (/Macintosh/i.test(userAgent) && "ontouchend" in document) || // iPad iOS13+ (real)
    (/Android/i.test(userAgent) && !/Mobile/i.test(userAgent)); // Android tablet

  return isMobileUA || isTabletUA;
};

export const isSignedRequestScheme = (clientId?: string): boolean =>
  typeof clientId === "string" &&
  (clientId.startsWith(`${CLIENT_ID_PREFIX_DECENTRALIZED_IDENTIFIER}:`) ||
    clientId.startsWith(`${CLIENT_ID_PREFIX_X509_SAN_DNS}:`));

export const isRedirectUriClientId = (clientId?: string): boolean =>
  typeof clientId === "string" &&
  clientId.startsWith(`${CLIENT_ID_PREFIX_REDIRECT_URI}:`);

/** Parse Chrome/CriOS version; null when the UA is not Chromium-based. */
const parseChromiumVersion = (userAgent: string): number[] | null => {
  const match = userAgent.match(/(?:Chrome|CriOS)\/(\d+)(?:\.(\d+)\.(\d+)\.(\d+))?/);
  if (!match) return null;
  return [
    Number(match[1]),
    Number(match[2] || 0),
    Number(match[3] || 0),
    Number(match[4] || 0),
  ];
};

/**
 * Chrome versions before 144.0.7559.59 are affected by CVE-2026-0904
 * (Digital Credentials UI domain spoofing). Non-Chromium UAs skip this gate.
 */
export const isChromeDcApiSecurityVersionMet = (
  userAgent: string = navigator.userAgent,
): boolean => {
  const version = parseChromiumVersion(userAgent);
  if (version === null) return true;
  for (let i = 0; i < MIN_CHROME_DC_API_VERSION.length; i++) {
    if (version[i] > MIN_CHROME_DC_API_VERSION[i]) return true;
    if (version[i] < MIN_CHROME_DC_API_VERSION[i]) return false;
  }
  return true;
};

export const isDcApiSupported = (clientId: string): boolean => {
  if (!isSignedRequestScheme(clientId)) return false;
  if (!isChromeDcApiSecurityVersionMet()) return false;
  if (typeof window.DigitalCredential === "undefined") return false;
  const allows = window.DigitalCredential.userAgentAllowsProtocol;
  if (typeof allows !== "function") return false;
  return allows.call(window.DigitalCredential, DC_API_PROTOCOL) === true;
};

export const isSdJwt = (vpToken: string): boolean => {
    try {
        const jwtParts = vpToken.split('~')[0].split('.');
        if (jwtParts.length !== 3) {
            return false;
        }
        const header = decodeBase64Url(jwtParts[0]);
        const {typ} = JSON.parse(header);
        return VALID_SD_JWT_TYPES.has(typ);
    }catch (e) {
        console.log("Invalid SD-JWT:", e);
        return false;
    }
}


const decodeBase64Url = (encoded: string): string => {
    let base64 = encoded.replace(/-/g, "+").replace(/_/g, "/");
    const pad = base64.length % 4;
    if (pad) base64 += "=".repeat(4 - pad);
    const decoded = atob(base64);
    const decodedBytes = Uint8Array.from(decoded, (c) => c.charCodeAt(0));
    return new TextDecoder().decode(decodedBytes);
};

/**
 * OpenID4VP 1.0 DCQL vp_token: object keyed by credential query id,
 * each value an array of presentations/credentials.
 * Token shape alone cannot reject a key collision with legacy PE
 * (`verifiableCredential`); persist requested query IDs for stricter checks.
 */
export const isDcqlVpToken = (vpToken: unknown): vpToken is Record<string, unknown[]> => {
  if (!vpToken || typeof vpToken !== "object" || Array.isArray(vpToken)) {
    return false;
  }
  const entries = Object.entries(vpToken as Record<string, unknown>);
  return (
    entries.length > 0 &&
    entries.every(([, value]) => Array.isArray(value))
  );
};

/**
 * Read a hash query param without URLSearchParams decoding, so literal
 * percent sequences in plain JSON vp_token values are preserved.
 */
export const getRawHashParam = (hash: string, key: string): string | undefined => { 
  const prefix = `${key}=`;
  const params = hash.replace(/^#/, "").split("&"); 
  for (const param of params) {
    if (param.startsWith(prefix)) {
      return param.slice(prefix.length);
    }
  }
  return undefined;
};

/**
 * Decode application/x-www-form-urlencoded text (spaces as `+`, then %-escapes).
 * Matches Java URLEncoder / Spring @RequestParam so redirect vp_tokens verify
 * the same claim bytes as direct-post submissions.
 */
const decodeFormUrlEncoded = (value: string): string =>
  decodeURIComponent(value.replace(/\+/g, " "));

/**
 * Parse vp_token from a redirect hash fragment (URL-encoded or plain JSON).
 * Plain JSON is tried first so literal `%` sequences in claim values are kept;
 * the fallback applies form-urlencoded decoding (`+` → space, then percent-decode).
 */
export const parseVpTokenFromFragment = (vpTokenParam: string): unknown => {
  try {
    return JSON.parse(vpTokenParam);
  } catch {
    return JSON.parse(decodeFormUrlEncoded(vpTokenParam));
  }
};

/**
 * Extract a single VC from an OpenID4VP 1.0 DCQL vp_token.
 */
export const extractVcFromVpToken = (vpToken: unknown): unknown => {
  if (!isDcqlVpToken(vpToken)) {
    throw new Error("Unsupported vp_token format in redirect URL");
  }

  const presentations = Object.values(vpToken)[0];
  if (!presentations?.length) {
    throw new Error("Empty credential entry in vp_token");
  }
  const presentation = presentations[0];
  if (presentation == null || presentation === "") {
    throw new Error("Empty credential entry in vp_token");
  }
  if (
    presentation &&
    typeof presentation === "object" &&
    "verifiableCredential" in presentation
  ) {
    const nested = (presentation as { verifiableCredential?: unknown[] })
      .verifiableCredential;
    if (Array.isArray(nested) && nested.length) {
      return nested[0];
    }
  }
  // DCQL ldp_vc: credential object is the presentation entry itself
  return presentation;
};

export const normalizeVp = (vp: any): Record<string, unknown> => {
    if (typeof vp === "string") {
        if (isSdJwt(vp)) vp ;
        try {
            return JSON.parse(vp);
        } catch {
           {vp};
        }
    }
    return vp;
};

export const clearUrl = (params: string[] = []) => {
    const url = new URL(window.location.href);

    const hashParamsObj = new URLSearchParams(url.hash.slice(1));
    params.forEach(param => {
        url.searchParams.delete(param);
        hashParamsObj.delete(param);
    });
    url.hash = hashParamsObj.toString()
      ? `#${hashParamsObj.toString()}`
      : "";

    window.history.replaceState(null, "", url.pathname + url.search + url.hash);
};

export const summariseVCResult = (
    response: VCVerificationV2Response
): "SUCCESS" | "INVALID" | "EXPIRED" | "REVOKED" => {

    if (!response.schemaAndSignatureCheck?.valid) {
        return "INVALID";
    }

    if (!response.expiryCheck?.valid) {
        return "EXPIRED";
    }

    if (response.statusCheck?.length) {
        for (const status of response.statusCheck) {
            if (status.error) {
                throw new Error(
                    status.error.errorMessage || "Status check error occurred"
                );
            }

            const isRevoked =
                status.purpose === "revocation" &&
                !status.valid &&
                status.error == null;

            if (isRevoked) return "REVOKED";
        }
    }

    return response.allChecksSuccessful ? "SUCCESS" : "INVALID";
};
export const summariseVPResult = (cred: CredentialResult): "SUCCESS" | "INVALID" | "EXPIRED" | "REVOKED" => {
    if (cred.holderProofCheck?.valid === false) return "INVALID";

    if (cred.schemaAndSignatureCheck?.valid === false) return "INVALID";

    if (cred.expiryCheck?.valid === false) return "EXPIRED";

    if (cred.statusCheck?.length) {
        for (const status of cred.statusCheck) {
            if (status.error) {
                throw new Error(status.error.errorMessage || "Status check error occurred");}

            const isRevoked =
                status.purpose === "revocation" &&
                    status.valid === false &&
                status.error == null;

            if (isRevoked) return "REVOKED";
        }
    }

    return cred.allChecksSuccessful ? "SUCCESS" : "INVALID";
};
