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