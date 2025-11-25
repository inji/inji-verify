import {AlertInfo, claim, VerificationStepsContentType } from "../types/data-types";
import i18next from 'i18next';

export const Pages = {
    Home: "/upload",
    Scan:"/scan",
    VerifyCredentials: "/verify",
    Offline: "/offline",
    PageNotFound: "*"
}

export const SupportedFileTypes = ["png", "jpeg", "jpg", "pdf"];

export const VerificationSteps: any = {
    "SCAN": {
        QrCodePrompt: 1,
        ActivateCamera: 2,
        Verifying: 3,
        DisplayResult: 4
    },
    "UPLOAD": {
        QrCodePrompt: 1,
        Verifying: 2,
        DisplayResult: 3
    },
    "VERIFY": {
        InitiateVpRequest: 1,
        SelectCredential: 2,
        RequestMissingCredential: 2,
        ScanQrCode: 3,
        SelectWallet:3,
        DisplayResult: 4
    }
}

export const getVerificationStepsContent = (): VerificationStepsContentType => {
    return {
        SCAN: [
            {
                label: i18next.t('VerificationStepsContent:SCAN.QrCodePrompt.label'),
                description: i18next.t('VerificationStepsContent:SCAN.QrCodePrompt.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:SCAN.ActivateCamera.label'),
                description: i18next.t('VerificationStepsContent:SCAN.ActivateCamera.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:SCAN.Verifying.label'),
                description: i18next.t('VerificationStepsContent:SCAN.Verifying.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:SCAN.DisplayResult.label'),
                description: i18next.t('VerificationStepsContent:SCAN.DisplayResult.description'),
            }
        ],
        UPLOAD: [
            {
                label: i18next.t('VerificationStepsContent:UPLOAD.QrCodePrompt.label'),
                description: i18next.t('VerificationStepsContent:UPLOAD.QrCodePrompt.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:UPLOAD.Verifying.label'),
                description: i18next.t('VerificationStepsContent:UPLOAD.Verifying.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:UPLOAD.DisplayResult.label'),
                description: i18next.t('VerificationStepsContent:UPLOAD.DisplayResult.description'),
            }
        ],
        VERIFY: [
            {
                label: i18next.t('VerificationStepsContent:VERIFY.InitiateVpRequest.label'),
                description: i18next.t('VerificationStepsContent:VERIFY.InitiateVpRequest.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:VERIFY.SelectCredential.label'),
                description: i18next.t('VerificationStepsContent:VERIFY.SelectCredential.description'),
            },
            {
              label: i18next.t("VerificationStepsContent:VERIFY.RequestMissingCredential.label"),
              description: i18next.t("VerificationStepsContent:VERIFY.RequestMissingCredential.description"),
            },
            {
                label: i18next.t('VerificationStepsContent:VERIFY.ScanQrCode.label'),
                description: i18next.t('VerificationStepsContent:VERIFY.ScanQrCode.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:VERIFY.SelectWallet.label'),
                description: i18next.t('VerificationStepsContent:VERIFY.SelectWallet.description'),
            },
            {
                label: i18next.t('VerificationStepsContent:VERIFY.DisplayResult.label'),
                description: i18next.t('VerificationStepsContent:VERIFY.DisplayResult.description'),
            }
        ],
        TO_BE_SELECTED: []
    };
};


export const AlertMessages =()=> {
    return {
        qrUploadSuccess: {message: i18next.t("AlertMessages:qrUploadSuccess"), severity: "success", autoHideDuration: 1200} as AlertInfo,
        qrScanSuccess: {message: i18next.t("AlertMessages:qrScanSuccess"), severity: "success", autoHideDuration: 1200} as AlertInfo,
        sessionExpired: {message: i18next.t("AlertMessages:sessionExpired"), severity: "error"} as AlertInfo,
        qrNotDetected: {message: i18next.t("AlertMessages:qrNotDetected"), severity: "error"} as AlertInfo,
        qrNotSupported: {message: i18next.t("AlertMessages:qrNotSupported"), severity: "error"} as AlertInfo,
        unsupportedFileSize: {message: i18next.t("AlertMessages:unsupportedFileSize"), severity: "error"} as AlertInfo,
        verificationMethodComingSoon: {message: i18next.t("AlertMessages:verificationMethodComingSoon"), severity: "warning"} as AlertInfo,
        unsupportedFileType: {message: i18next.t("AlertMessages:unsupportedFileType"), severity: "error"} as AlertInfo,
        pageNotFound: {message: i18next.t("AlertMessages:pageNotFound"), severity: "error"} as AlertInfo,
        failToGenerateQrCode: {message:i18next.t("AlertMessages:failToGenerateQrCode"), severity: "error"} as AlertInfo,
        unexpectedError: {message:i18next.t("AlertMessages:unexpectedError"), severity: "error"} as AlertInfo,
        scanSessionExpired: {message: i18next.t("AlertMessages:scanSessionExpired"), severity: "error"} as AlertInfo,
        partialCredentialShared:{message: i18next.t("AlertMessages:partialCredentialShared"), severity: "error"} as AlertInfo,
        validationFailure:{message: i18next.t("AlertMessages:validationFailure"), severity: "error"} as AlertInfo,
        incorrectCredential:{message: i18next.t("AlertMessages:incorrectCredential"), severity: "error"} as AlertInfo,
    }
};

export const UploadFileSizeLimits = {
    min: 10000, // 10KB
    max: 5000000 // 5MB
}

export const InternetConnectivityCheckEndpoint = window._env_.INTERNET_CONNECTIVITY_CHECK_ENDPOINT ?? "https://dns.google/";

const InternetConnectivityTimeout = Number.parseInt(window._env_.INTERNET_CONNECTIVITY_CHECK_TIMEOUT);
export const InternetConnectivityCheckTimeout = isNaN(InternetConnectivityTimeout)
    ? 10000
    : InternetConnectivityTimeout;

const timeout = Number.parseInt(window._env_.DISPLAY_TIMEOUT);
export const DisplayTimeout = isNaN(timeout) ? 10000 : timeout;

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

export const backgroundColorMapping: any = {
  SUCCESS: "bg-success",
  EXPIRED: "bg-expired",
  INVALID: "bg-invalid",
};
export const textColorMapping: any = {
  SUCCESS: "text-successText",
  EXPIRED: "text-expiredText",
  INVALID: "text-invalidText",
};

export const borderColorMapping: any = {
  SUCCESS: "border-successBorder",
  EXPIRED: "border-expiredBorder",
  INVALID: "border-invalidBorder",
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