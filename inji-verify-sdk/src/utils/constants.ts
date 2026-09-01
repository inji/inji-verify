// Constants for file types and size limits
export const SupportedFileTypes = ["png", "jpeg", "jpg", "pdf"];
export const UploadFileSizeLimits = {
  min: 10000, // 10KB
  max: 5000000, // 5MB
};

// Constants for frame processing
export const FRAME_PROCESS_INTERVAL_MS = 100;
export const THROTTLE_FRAMES_PER_SEC = 500; // Throttle frame processing to every 500ms (~2 frames per second)
export const ZOOM_STEP = 2.5;
export const INITIAL_ZOOM_LEVEL = 0;

// Constants for camera constraints
export const CONSTRAINTS_IDEAL_WIDTH = 2560;
export const CONSTRAINTS_IDEAL_HEIGHT = 1440;
export const CONSTRAINTS_IDEAL_FRAME_RATE = 30;

// Constants for QR code processing
export const HEADER_DELIMITER = "";
export const SUPPORTED_QR_HEADERS = [""];
export const ZIP_HEADER = "PK";
export const ScanSessionExpiryTime = 60000; // in milliseconds
export const OvpQrHeader = "INJI_OVP://";
export const BASE64_PADDING = "=="

// Helper for accepted file types string
export const acceptedFileTypes = SupportedFileTypes.map(
  (ext) => `.${ext}`
).join(", ");

// Constants for SD-JWT validation
export const VALID_SD_JWT_TYPES = new Set(['vc+sd-jwt', 'dc+sd-jwt']);

export const DC_API_PROTOCOL = "openid4vp-v1-signed";
export const DEFAULT_DC_API_TIMEOUT_MS = 300_000;

export const CLIENT_ID_PREFIX_DECENTRALIZED_IDENTIFIER = "decentralized_identifier";
export const CLIENT_ID_PREFIX_X509_SAN_DNS = "x509_san_dns";
export const CLIENT_ID_PREFIX_REDIRECT_URI = "redirect_uri";

// CVE-2026-0904: Digital Credentials UI domain spoofing in Chrome prior to this version.
export const MIN_CHROME_DC_API_VERSION = [144, 0, 7559, 59] as const;

// OpenID4VP deep-link protocol used when no `protocol` prop is provided.
export const DEFAULT_PROTOCOL = "openid4vp://";

// VP formats advertised in `client_metadata.vp_formats_supported`
// for signed-request (DID / x509_san_dns) and redirect_uri client_ids.
export const VP_FORMATS_SUPPORTED = {
  ldp_vp: {
    proof_type: [
      "Ed25519Signature2018",
      "Ed25519Signature2020",
      "RsaSignature2018",
    ],
  },
  "dc+sd-jwt": {
    "sd-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
    "kb-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
  },
  "vc+sd-jwt": {
    "sd-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
    "kb-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
  },
} as const;