import {
    AlertInfo, AlertSeverity,
    claim,
    VcStatus,
    VerificationMethod,
    VerificationStep, VerificationStepsContentType,
} from "../types/data-types";
import i18next from 'i18next';

export const Pages = {
    Home: "/",
    Scan:"/scan",
    VerifyCredentials: "/verify",
    Offline: "/offline",
    PageNotFound: "*"
}

export const SupportedFileTypes = ["png", "jpeg", "jpg", "pdf"];

export const VerificationSteps: Record<string, Record<string, number>> = {
    SCAN: { QrCodePrompt: 1, ActivateCamera: 2, Verifying: 3, DisplayResult: 4 },
    UPLOAD: { QrCodePrompt: 1, Verifying: 2, DisplayResult: 3 },
    VERIFY: {
        InitiateVpRequest: 1, SelectCredential: 2, RequestMissingCredential: 2,
        ScanQrCode: 3, SelectWallet: 3, DisplayResult: 4
    }
}

const verificationStep = (method: string, step: string) => ({
    label: i18next.t(`VerificationStepsContent:${method}.${step}.label`),
    description: i18next.t(`VerificationStepsContent:${method}.${step}.description`),
});

export const getVerificationStepsContent = (): VerificationStepsContentType => ({
    SCAN: [
        verificationStep("SCAN", "QrCodePrompt"),
        verificationStep("SCAN", "ActivateCamera"),
        verificationStep("SCAN", "Verifying"),
        verificationStep("SCAN", "DisplayResult"),
    ],
    UPLOAD: [
        verificationStep("UPLOAD", "QrCodePrompt"),
        verificationStep("UPLOAD", "Verifying"),
        verificationStep("UPLOAD", "DisplayResult"),
    ],
    VERIFY: [
        verificationStep("VERIFY", "InitiateVpRequest"),
        verificationStep("VERIFY", "SelectCredential"),
        verificationStep("VERIFY", "RequestMissingCredential"),
        verificationStep("VERIFY", "ScanQrCode"),
        verificationStep("VERIFY", "SelectWallet"),
        verificationStep("VERIFY", "DisplayResult"),
    ],
    TO_BE_SELECTED: []
});

const createAlert = (key: string, severity: AlertSeverity, autoHideDuration?: number): AlertInfo => ({
    message: i18next.t(`AlertMessages:${key}`),
    severity,
    autoHideDuration
});

export const AlertMessages = () => ({
    qrUploadSuccess: createAlert("qrUploadSuccess", "success", 1200),
    qrScanSuccess: createAlert("qrScanSuccess", "success", 1200),
    sessionExpired: createAlert("sessionExpired", "error"),
    qrNotDetected: createAlert("qrNotDetected", "error"),
    qrNotSupported: createAlert("qrNotSupported", "error"),
    unsupportedFileSize: createAlert("unsupportedFileSize", "error"),
    verificationMethodComingSoon: createAlert("verificationMethodComingSoon", "warning"),
    unsupportedFileType: createAlert("unsupportedFileType", "error"),
    pageNotFound: createAlert("pageNotFound", "error"),
    failToGenerateQrCode: createAlert("failToGenerateQrCode", "error"),
    unexpectedError: createAlert("unexpectedError", "error"),
    scanSessionExpired: createAlert("scanSessionExpired", "error"),
    partialCredentialShared: createAlert("partialCredentialShared", "error"),
    validationFailure: createAlert("validationFailure", "error"),
    incorrectCredential: createAlert("incorrectCredential", "error"),
});

export const UploadFileSizeLimits = { min: 10000, max: 5000000 };

export const InternetConnectivityCheckEndpoint = window._env_.INTERNET_CONNECTIVITY_CHECK_ENDPOINT ?? "https://dns.google/";

const InternetConnectivityTimeout = Number.parseInt(window._env_.INTERNET_CONNECTIVITY_CHECK_TIMEOUT);
export const InternetConnectivityCheckTimeout = Number.isNaN(InternetConnectivityTimeout)
    ? 10000
    : InternetConnectivityTimeout;

const timeout = Number.parseInt(window._env_.DISPLAY_TIMEOUT);
export const DisplayTimeout = Number.isNaN(timeout) ? 10000 : timeout;

export const OvpQrHeader = window._env_.OVP_QR_HEADER;

let VCRenderOrders: any = {};
let verifiableClaims: claim[] = [];

export const getVCRenderOrders = () => VCRenderOrders;
export const getVerifiableClaims = () => verifiableClaims;

export const initializeClaims = async () => {
  try {
    const response = await fetch(window._env_.VERIFIABLE_CLAIMS_CONFIG_URL);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    verifiableClaims = data.verifiableClaims as claim[];
    VCRenderOrders = data.VCRenderOrders as any;
  } catch (error) {
    console.error("Error loading claims from ConfigMap:", error);
  }
};

initializeClaims();

export const backgroundColorMapping: Record<VcStatus, string> ={
  SUCCESS: "bg-success",
  EXPIRED: "bg-expired",
  INVALID: "bg-invalid",
  REVOKED: "bg-revoked",
};
export const textColorMapping: Record<VcStatus, string>  = {
  SUCCESS: "text-successText",
  EXPIRED: "text-expiredText",
  INVALID: "text-invalidText",
  REVOKED: "text-revokedText",
};

export const borderColorMapping: Record<VcStatus, string> = {
  SUCCESS: "border-successBorder",
  EXPIRED: "border-expiredBorder",
  INVALID: "border-invalidBorder",
  REVOKED: "border-revokedBorder",
};

export const isMobileDevice = (): boolean => {
  const ua = navigator.userAgent;

  const isMobileUA = /Android.*Mobile|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua);

  const isTabletUA =
    /iPad/i.test(ua) ||
    (/Macintosh/i.test(ua) && "ontouchend" in document) || // iPad iOS13+ (real)
    (/Android/i.test(ua) && !/Mobile/i.test(ua));          // Android tablet

  return isMobileUA || isTabletUA;
};

export const EXCLUDE_KEYS_SD_JWT_VC = [
  "cnf",
  "iss",
  "iat",
  "nbf",
  "exp",
  "jti",
  "sub",
  "ssn",
  "_sd_alg",
  "_sd",
  "@context",
  "issuer",
  "vct",
].map((key) => key.toLowerCase());

export const getStepConfig = (method: VerificationMethod | string) => VerificationSteps[method] || null;

export interface VerificationStepWithStatus extends VerificationStep {
    stepNumber: number;
    isCompleted: boolean;
    isActive: boolean;
}
