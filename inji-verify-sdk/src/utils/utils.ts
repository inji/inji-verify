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
    const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64);
    const decodedBytes = Uint8Array.from(decoded, c => c.charCodeAt(0));
    return new TextDecoder().decode(decodedBytes);
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
