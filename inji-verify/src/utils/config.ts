import {AlertInfo} from "../types/data-types";

export const Pages = {
    Home: "/verification",
    VerifyCredentials: "/",/*"/verify"*/
    Offline: "/offline",
    Redirect: "/redirect",
    PageNotFound: "*",
    LandingPage: "/",
    AccountOpeningPage: "/userprofile",
    Application: "/application"
}

export const SUPPORTED_DID_METHODS = ["web"];

export const SUPPORTED_QR_HEADERS = [''];
export const HEADER_DELIMITER = '';

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
    }
}

export const VerificationStepsContent: any = {
    SCAN: [
        {
            label: 'Scan QR Code',
            description: 'Tap "Scan" to start scanning the document or card with a QR code',
        },
        {
            label: 'Activate Camera and Position QR Code',
            description: 'Activate your device camera and hold the QR code within the frame to initiate verification.',
        },
        {
            label: 'Verification in Progress',
            description: 'The QR code verification is in progress.'
        },
        {
            label: 'View result',
            description: 'View the verification result.'
        }
    ],
    UPLOAD: [
        {
            label: 'Upload QR Code',
            description: 'Upload a file that contains a QR code',
        },
        {
            label: 'Verify document',
            description: 'Verification for the document or card is in progress.',
        },
        {
            label: 'View result',
            description: 'View the verification result.'
        }
    ]
};


export const AlertMessages = {
    qrUploadSuccess: {message: "QR code uploaded successfully!", severity: "success", autoHideDuration: 1200} as AlertInfo,
    sessionExpired: {message: "The scan session has expired due to inactivity. Please initiate a new scan.", severity: "error"} as AlertInfo,
    qrNotDetected: {message: "No MultiFormat Readers were able to detect the QR code.", severity: "error"} as AlertInfo,
    qrNotSupported: {message: "QR code format is not supported.", severity: "error"} as AlertInfo,
    unsupportedFileSize: {message: "File size not supported. The file size should be between 10 KB and 5 MB.", severity: "error"} as AlertInfo,
    verificationMethodComingSoon: {message: "Coming soon", severity: "warning"} as AlertInfo,
    unsupportedFileType: {message: "Unsupported file format. Allowed file formats are: png, jpeg, jpg, pdf", severity: "error"} as AlertInfo,
    pageNotFound: {message: "Page Not Found!!", severity: "error"} as AlertInfo
};

// TODO: Update the error messages for the following
// maintain mapping between the error codes and
export const OvpErrors: any = {
  invalid_scope: "Invalid Scope", //presently this won't be shown, as no scope is being passed
  invalid_request: "Please check your input and try again",
  invalid_client: "Invalid Client", //handled in inji web, no redirection
  vp_formats_not_supported: "VP Formats Not Supported", // presently not handled specifically, bad request (invalid_request error is responded)
  invalid_presentation_definition_uri: "Invalid Presentation Definition URI", // not being used, presentation definition being used
  invalid_presentation_definition_reference:
    "Invalid Presentation Definition Reference", // not being used, presentation definition being used.
  resource_not_found: "Something went wrong with your request. Please check and try again.",
  request_time_out: "The request took too long. Please try again later.",
  uri_too_long: "The URL is too long. Please use a shorter link.",
  internal_server_error:
    "We're having trouble processing your request. Please try again later.",
  server_unavailable:
    "The service is currently unavailable. Please try again later",
  invalid_vp_token:
    "The credentials don't meet the requirements. Please check and try again.",
  unsupported_format: "VP Formats Not Supported"
};

export const ScanSessionExpiryTime = 60000; // in milliseconds

export const UploadFileSizeLimits = {
    min: 5000, // 5KB
    max: 5000000 // 5MB
}

export const InternetConnectivityCheckEndpoint = window._env_.INTERNET_CONNECTIVITY_CHECK_ENDPOINT ?? "https://dns.google/";

export const InternetConnectivityCheckTimeout = isNaN(Number.parseInt(window._env_.INTERNET_CONNECTIVITY_CHECK_TIMEOUT))
    ? 10000
    : Number.parseInt(window._env_.INTERNET_CONNECTIVITY_CHECK_TIMEOUT); //milliseconds

export const OvpQrHeader = window._env_.OVP_QR_HEADER;
