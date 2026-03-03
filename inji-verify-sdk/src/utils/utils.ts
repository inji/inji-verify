import {VALID_SD_JWT_TYPES} from "./constants";

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