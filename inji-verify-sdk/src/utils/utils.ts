import {VALID_SD_JWT_TYPES} from "./constants";
import {CredentialResult, VCVerificationV2Response} from "../components/qrcode-verification/QRCodeVerification.types";

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
        if (isSdJwt(vp)) return { raw: vp };
        try {
            return JSON.parse(vp);
        } catch {
            return { raw: vp };
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

    if (cred.statusChecks?.length) {
        for (const status of cred.statusChecks) {
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
