import { useCallback, useEffect, useRef, useState } from "react";
import {
  QRCodeVerificationProps,
  scanResult,
  VcStatus,
} from "./QRCodeVerification.types";
import { doFileChecks, scanFilesForQr } from "../../utils/uploadQRCodeUtils";
import {
  acceptedFileTypes,
  BASE64_PADDING,
  CONSTRAINTS_IDEAL_FRAME_RATE,
  CONSTRAINTS_IDEAL_HEIGHT,
  CONSTRAINTS_IDEAL_WIDTH,
  FRAME_PROCESS_INTERVAL_MS,
  INITIAL_ZOOM_LEVEL,
  OvpQrHeader,
  ScanSessionExpiryTime,
  THROTTLE_FRAMES_PER_SEC,
  ZOOM_STEP,
} from "../../utils/constants";
import {
  vcSubmission,
  vcVerification,
  vpRequest,
  vpRequestStatus,
  vpResult
} from "../../utils/api";
import {
  decodeQrData,
  extractRedirectUrlFromQrData,
} from "../../utils/dataProcessor";
import { readBarcodes } from "zxing-wasm/full";
import { MinusOutlined, PlusOutlined } from "@ant-design/icons";
import { Slider } from "@mui/material";
import "./QRCodeVerification.css";
import { isSdJwt } from "../../utils/utils";
import {QrData} from "../../types/OVPSchemeQrData";

const QRCodeVerification: React.FC<QRCodeVerificationProps> = ({
  scannerActive = true,
  triggerElement,
  verifyServiceUrl,
  transactionId,
  onVCReceived,
  onVCProcessed,
  onClose,
  onError,
  isEnableUpload = true,
  isEnableScan = true,
  uploadButtonId,
  uploadButtonStyle,
  isEnableZoom = true,
  clientId,
  isVPSubmissionSupported = false
}) => {
  const [isScanning, setScanning] = useState(false);
  const [isUploading, setUploading] = useState(false);
  const [isLoading, setLoading] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(INITIAL_ZOOM_LEVEL);
  const [isMobile, setIsMobile] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(document.createElement("canvas"));
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamingRef = useRef(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const scanSessionCompletedRef = useRef(false);
  const frameProcessingRef = useRef(false);
  const startingRef = useRef(false);
  const shouldEnableZoom = isEnableZoom && isMobile;
  const hasFetchedVPResultRef = useRef(false);

  const clearTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  if (!verifyServiceUrl) throw new Error("verifyServiceUrl is required.");
  if (!isEnableUpload && !isEnableScan)
    throw new Error("Either scan or upload must be enabled.");
  if (!onVCReceived && !onVCProcessed)
    throw new Error("One of onVCReceived or onVCProcessed is required.");
  if (onVCReceived && onVCProcessed)
    throw new Error(
      "Only one of onVCReceived or onVCProcessed can be provided."
    );
  if (!onError) throw new Error("onError callback is required.");
  if (!clientId) throw new Error("clientId is required.");

  const readQrCodeFromCanvas = useRef(async (canvas: HTMLCanvasElement) => {
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    const imageData = ctx?.getImageData(0, 0, canvas.width, canvas.height);
    if (!imageData) return;
    try {
      const results = await readBarcodes(imageData);
      if (results[0]?.text) {
        frameProcessingRef.current = false;
        clearTimer();
        stopVideoStream();
        setScanning(true);
        const text = results[0].text;
        processScanResult(text);
      }
    } catch (error) {
      handleError(error);
    }
  });

  const processFrame = useCallback(() => {
    if (!frameProcessingRef.current) return;
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !video.videoWidth || !video.videoHeight) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);
    readQrCodeFromCanvas.current(canvas);

    if (frameProcessingRef.current) {
      setTimeout(
        () => requestAnimationFrame(processFrame),
        THROTTLE_FRAMES_PER_SEC
      );
    }
  }, []);

  const stopVideoStream = useCallback(() => {
    frameProcessingRef.current = false;
    try {
      const anyWindow = window as any;
      const video = videoRef.current;
      if (video) {
        video.pause();
        const stream = video.srcObject as MediaStream | null;
        if (stream) {
          stream.getTracks().forEach((track) => {
            track.stop();
          });
        }
        video.srcObject = null;
        video.onloadedmetadata = null;
        video.oncanplay = null;
        video.onplay = null;
        video.onerror = null;
      }

      if (anyWindow.__inji_all_streams && anyWindow.__inji_all_streams.length) {
        anyWindow.__inji_all_streams.forEach((mediaStream: MediaStream) => {
          try {
            mediaStream.getTracks().forEach((t: MediaStreamTrack) => {
              try {
                t.stop();
              } catch (e) {}
            });
          } catch (e) {
            console.warn("[stopVideoStream] error stopping registry stream", e);
          }
        });
        anyWindow.__inji_all_streams = [];
      }

    } catch (err) {
      console.error("[stopVideoStream] unexpected error:", err);
    } finally {
      streamingRef.current = false;
      startingRef.current = false;
    }
  }, []);

  const stopTracks = (stream: MediaStream | null | undefined) => {
    if (!stream) return;
      stream.getTracks().forEach((track) => track.stop());
  };

  const registerStream = (stream: MediaStream) => {
    const anyWindow = window as any;
    anyWindow.__inji_all_streams = anyWindow.__inji_all_streams || [];
    anyWindow.__inji_all_streams.push(stream);
    anyWindow.__inji_current_stream = stream;
  };

  const startSessionTimer = (
    clearTimer: () => void,
    timerRef: React.MutableRefObject<any>,
    stopVideoStream: () => void,
    onError?: (err: Error) => void
  ) => {
    clearTimer();
    timerRef.current = setTimeout(() => {
      stopVideoStream();
      onError?.(new Error("Session expired. Please Scan again."));
    }, ScanSessionExpiryTime);
  };

  const startFrameProcessing = (
    frameProcessingRef: React.MutableRefObject<boolean>,
    processFrame: () => void
  ) => {
    frameProcessingRef.current = true;
    setTimeout(processFrame, FRAME_PROCESS_INTERVAL_MS);
  };

  const attachStreamToVideoElement = (
    stream: MediaStream,
    videoRef: React.MutableRefObject<HTMLVideoElement | null>,
    startingRef: React.MutableRefObject<boolean>,
    streamingRef: React.MutableRefObject<boolean>,
    frameProcessingRef: React.MutableRefObject<boolean>,
    clearTimer: () => void,
    timerRef: React.MutableRefObject<any>,
    stopVideoStream: () => void,
    onError: ((err: Error) => void) | undefined,
    processFrame: () => void
  ) => {
    const video = videoRef.current;
    if (!video) {
      stopTracks(stream);
      startingRef.current = false;
      return;
    }

    video.srcObject = stream;
    video.disablePictureInPicture = true;
    video.playsInline = true;
    video.controls = false;
    video.muted = true;

    video.onloadedmetadata = () => {
      video
        .play()
        .then(() => {
          streamingRef.current = true;
          startingRef.current = false;
          startSessionTimer(clearTimer, timerRef, stopVideoStream, onError);
          startFrameProcessing(frameProcessingRef, processFrame);
        })
        .catch((error) => {
          startingRef.current = false;
          stopVideoStream();
          onError?.(error);
        });
    };

    video.onerror = () => {
      startingRef.current = false;
      stopVideoStream();
      onError?.(new Error("Video stream error"));
    };

    return;
  };

  const startVideoStream = useCallback(() => {
    if (
      !isEnableScan ||
      streamingRef.current ||
      startingRef.current ||
      scanSessionCompletedRef.current
    )
      return;

    startingRef.current = true;

    navigator.mediaDevices
      .getUserMedia({
        video: {
          width: { ideal: CONSTRAINTS_IDEAL_WIDTH },
          height: { ideal: CONSTRAINTS_IDEAL_HEIGHT },
          frameRate: { ideal: CONSTRAINTS_IDEAL_FRAME_RATE },
          facingMode: "environment",
        },
      })
      .then((stream) => {
        const anyWindow = window as any;
        anyWindow.__inji_all_streams = anyWindow.__inji_all_streams || [];

        const existing = anyWindow.__inji_all_streams.find(
          (stream: MediaStream) => stream && stream.active
        );

        if (existing) {
          stopTracks(stream);
          const video = videoRef.current;
          if (video) video.srcObject = existing;
          streamingRef.current = true;
          startingRef.current = false;
          startSessionTimer(clearTimer, timerRef, stopVideoStream, onError);
          startFrameProcessing(frameProcessingRef, processFrame);
          return;
        }

        registerStream(stream);
        attachStreamToVideoElement(
          stream,
          videoRef,
          startingRef,
          streamingRef,
          frameProcessingRef,
          clearTimer,
          timerRef,
          stopVideoStream,
          onError,
          processFrame
        );
      })
      .catch((error) => {
        startingRef.current = false;
        onError?.(error);
      });
  }, [isEnableScan, onError, processFrame, stopVideoStream]);

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    try {
      clearTimer();
      stopVideoStream();
      const file = e.target?.files?.[0];
      if (!file || !doFileChecks(file)) return (e.target.value = "");
      setUploading(true);
      const result: scanResult = await scanFilesForQr(file);
      if (result.error) throw result.error;
      await processScanResult(result.data);
      e.target.value = "";
    } catch (error) {
      e.target.value = "";
      setUploading(false);
      handleError(error);
    }
  };

  const processScanResult = async (data: any) => {
    setLoading(true);
    try {
      const vc = await extractVerifiableCredential(data);
      if (vc instanceof Error) throw vc;
      if (vc && vc.toString().endsWith(BASE64_PADDING)) {
        throw Error("Vc Type Not Supported");
      }
      if (vc) {
        await triggerCallbacks(vc);
      }
    } catch (error) {
      throw error;
    }
  };

  const storeStates = (data: QrData) => {
    sessionStorage.setItem("transactionId", data.transactionId);
    sessionStorage.setItem("requestId", data.requestId);
    sessionStorage.setItem("pathName", window.location.pathname);
  };

  const createVPRequest = async (presentationDefinition: any) => {
    try {
      let presentationDefinitionId;
      const data = await vpRequest(
        verifyServiceUrl,
        clientId,
        transactionId ?? undefined,
        presentationDefinitionId,
        presentationDefinition
      );

      storeStates(data);

      return data;
    } catch (error) {
      resetState();
      throw error instanceof Error ? error : new Error(String(error));
    }
  };

  const parsePresentationDefinition = (pdParams: string) => {
    try {
      const decoded = JSON.parse(pdParams);
      const {inputDescriptors, ...rest} = decoded;

      if (inputDescriptors) return {
        ...rest,
        input_descriptors: inputDescriptors
      };
      return decoded;
    } catch (error) {
      throw new Error("Failed to create VP request, due to invalid presentation definition");
    }
  };

  const buildRedirectUrl = (
    baseRedirectUrl: string,
    state: string,
    responseUri: string,
    nonce: string
  ) => {
    const redirectUri = `${window.location.origin}/`;

    const url = new URL(baseRedirectUrl);
    url.hash = "";
    url.searchParams.set("client_id", clientId);
    url.searchParams.set("redirect_uri", redirectUri);
    url.searchParams.set("state", state);
    url.searchParams.set("response_mode", "direct_post");
    url.searchParams.set("response_uri", responseUri);
    url.searchParams.set("nonce", nonce);

    return `${url.toString()}#`;
  };

  const extractVerifiableCredential = async (data: any) => {
    try {
      if (data?.vpToken) return data.vpToken.verifiableCredential[0];
      //check if QRCode contains OVP_QR in the header, it means this is a
      // data share VC
      if (typeof data === "string" && data.startsWith(OvpQrHeader)) {
        //extract the redirect Url from QRCode
        const redirectUrl = extractRedirectUrlFromQrData(data);
        if (!redirectUrl)
          throw new Error("Failed to extract redirect URL from QR data");

        if (!isVPSubmissionSupported) {
          const encodedOrigin = encodeURIComponent(window.location.origin);
          window.location.href = `${redirectUrl}&client_id=${clientId}&redirect_uri=${encodedOrigin}%2F#`;
          return;
        }

        const parsedUrl = new URL(redirectUrl);
        const pdParams = parsedUrl.searchParams.get("presentation_definition");

        if (!pdParams) throw new Error("Missing presentation_definition in redirect URL");

        const presentationDefinition = parsePresentationDefinition(pdParams);
        //call /v1/verify/vp-request endpoint to get the request_id and
        // transaction_id to be sent to the redirectUrl
        const response = await createVPRequest(presentationDefinition);

        if (!response) throw new Error("Unable to access the shared VC, due to failure in creating VP request");

        const { requestId: state , authorizationDetails } = response;

        if (!authorizationDetails) throw new Error("Unable to access the shared VC, due to Missing authorization details in VP Request");

        const { responseUri, nonce } = authorizationDetails;

        if (!responseUri || !nonce) throw new Error("Unable to access the shared VC, due to missing responseUri or nonce in authorization details");
        //call the redirectUrl
        window.location.href = buildRedirectUrl(parsedUrl.toString(), state, responseUri, nonce);
        return;
      }

      if (typeof data === "string") {
        const decoded = await decodeQrData(new TextEncoder().encode(data));
        return JSON.parse(decoded);
      }
      throw new Error("Unable to access the shared VC, due to unsupported QR data format");
    } catch (error) {
      resetState();
      return error;
    }
  };

  const resetState = () => {
    sessionStorage.removeItem("transactionId");
    sessionStorage.removeItem("requestId");
    sessionStorage.removeItem("pathName");
    hasFetchedVPResultRef.current = false;
    scanSessionCompletedRef.current = true;
    frameProcessingRef.current = false;
    clearTimer();
    stopVideoStream();
    setScanning(false);
    setUploading(false);
    setLoading(false);
  };

  const triggerCallbacks = async (vc: any) => {
    try {
      if (onVCReceived) {
        const txnId = await vcSubmission(vc, verifyServiceUrl, transactionId);
        onVCReceived(txnId);
      } else if (onVCProcessed) {
        const status = await vcVerification(vc, verifyServiceUrl);
        onVCProcessed([{ vc, vcStatus: status }]);
      }
    } catch (error) {
      handleError(error);
    } finally {
      resetState();
    }
  };

  const handleError = (error: unknown) => {
    frameProcessingRef.current = false;
    stopVideoStream();
    onError(
      error instanceof Error ? error : new Error("An unexpected error occurred while processing VC")
    );
  };

  const handleZoomChange = (value: number) => {
    if (value >= 0 && value <= 10) setZoomLevel(value);
  };

  const handleSliderChange = (_: any, value: number | number[]) => {
    if (typeof value === "number") handleZoomChange(value);
  };

  function base64UrlDecode(base64url: string): string {
    // Convert base64url to base64
    let base64 = base64url.replace(/-/g, "+").replace(/_/g, "/");
    const pad = base64.length % 4;
    if (pad) base64 += "=".repeat(4 - pad);
    return atob(base64);
  }

  const fetchVPResult = async (transactionId: string) => {
    if (hasFetchedVPResultRef.current) return;
    hasFetchedVPResultRef.current = true;
    try {
      if (transactionId) {
        const vcResults = await vpResult(verifyServiceUrl, transactionId);
        if (vcResults && vcResults.length > 0) {
          const VCResult = vcResults.map((vcResult: any) => ({
            vc: isSdJwt(vcResult.vc) ? vcResult.vc : JSON.parse(vcResult.vc),
            vcStatus: vcResult.verificationStatus as VcStatus,
          }));
          if (onVCReceived) {
            const txnId = await vcSubmission(VCResult[0].vc, verifyServiceUrl, transactionId);
            onVCReceived(txnId);
          } else if (onVCProcessed) {
            onVCProcessed(VCResult);
          }
          resetState();
          return;
        } else {
          throw new Error("Unable to process the VC, due to invalid VP submission");
        }
      }
    } catch (error) {
      handleError(error);
      resetState();
    }
  };

  const fetchVPStatus = async (transactionId: string, requestId: string) => {
    setLoading(true);
    try {
      const response = await vpRequestStatus(verifyServiceUrl, requestId);
      const hasRequiredKeys = sessionStorage.getItem("transactionId") && sessionStorage.getItem("requestId");
      if (response.status === "VP_SUBMITTED" && hasRequiredKeys) {
        await fetchVPResult(transactionId);
      } else {
        resetState();
        throw new Error("VP submission failed or not completed");
      }
    } catch (error) {
      handleError(error);
    }
  };

  const startScanning =
    Boolean(scannerActive) &&
    isEnableScan &&
    !isUploading &&
    !isScanning &&
    !scanSessionCompletedRef.current;

  useEffect(() => {
    if (scannerActive) {
      if (startScanning) startVideoStream();
    } else {
      frameProcessingRef.current = false;
      clearTimer();
      stopVideoStream();
    }
  }, [scannerActive, startScanning, startVideoStream, stopVideoStream]);

  useEffect(() => {
    const resize = () => setIsMobile(window.innerWidth < 768);
    window.addEventListener("resize", resize);
    resize();
    return () => window.removeEventListener("resize", resize);
  }, []);

  useEffect(() => {
    let vpToken, presentationSubmission, error, errorDescripton;
    try {
      const searchParams = new URLSearchParams(window.location.search); //"?error=abc123&error_description=xyz
      const hash = window.location.hash; // "#vp_token=abc123&state=xyz"
      const params = new URLSearchParams(hash.substring(1));
      const vpTokenParam = params.get("vp_token");
      const decoded = vpTokenParam && base64UrlDecode(vpTokenParam);
      const parseVpToken = decoded && JSON.parse(decoded);
      vpToken = vpTokenParam ? parseVpToken : null;
      presentationSubmission = params.get("presentation_submission")
        ? decodeURIComponent(params.get("presentation_submission") as string)
        : undefined;
      error = params.get("error") || searchParams.get("error");
      errorDescripton = params.get("error_description") || searchParams.get("error_description") || `We’re unable to complete your request`;

      if (error) {
        onError(new Error(`${errorDescripton}, ${error}`));
        resetState();
        window.history.replaceState(null, "", window.location.pathname);
      }

      if (vpToken && presentationSubmission) {
        processScanResult({ vpToken, presentationSubmission });
        window.history.replaceState(null, "", window.location.pathname);
      } else {
        const requestId = sessionStorage.getItem("requestId");
        const transactionId = sessionStorage.getItem("transactionId");

        if (requestId && transactionId && !vpToken && !error) {
          fetchVPStatus(transactionId, requestId);
        }
      }
    } catch (error) {
      console.error(
        "Error occurred while reading params in redirect url, Error: ",
        error
      );
      onError(error instanceof Error ? error : new Error("An unexpected error occurred while processing the request"));
      resetState();
    }
  }, []);

  useEffect(() => {
    return () => {
      frameProcessingRef.current = false;
      clearTimer();
      stopVideoStream();
      scanSessionCompletedRef.current = false;
    };
  }, [stopVideoStream]);

  return (
    <div className="qrcode-container">
      {triggerElement && !isUploading && !isScanning && !isLoading && (
        <div className="cursor-pointer">{triggerElement}</div>
      )}
      {(isUploading || isScanning || isLoading) && (
        <div className="loader"></div>
      )}
      <div className={`qr-wrapper ${!shouldEnableZoom ? "no-zoom" : ""}`}>
        {startScanning && (
          <div
            className={`scan-container ${
              shouldEnableZoom ? "zoom-enabled" : "no-zoom"
            }`}
          >
            {shouldEnableZoom && (
              <button
                onClick={() => {
                  stopVideoStream();
                  onClose?.();
                }}
                className="qr-close-button"
                aria-label="Close Scanner"
              >
                ✕
              </button>
            )}
            <video
              ref={videoRef}
              className="qr-video"
              style={{
                transform: shouldEnableZoom
                  ? `scale(${1 + zoomLevel / ZOOM_STEP})`
                  : undefined,
              }}
              playsInline
              autoPlay
              muted
            />
            {shouldEnableZoom && (
              <div className="qr-overlay">
                <div className="centered-row">
                  <MinusOutlined
                    onClick={() => handleZoomChange(zoomLevel - 1)}
                    className={`zoom-button-decrease${
                      zoomLevel === 0 ? " disabled" : ""
                    }`}
                  />
                  <div className="slider-container">
                    <Slider
                      key={`${zoomLevel}`}
                      aria-label="Zoom Level"
                      min={0}
                      max={10}
                      step={1}
                      value={zoomLevel}
                      onChange={handleSliderChange}
                      onChangeCommitted={handleSliderChange}
                      marks
                      valueLabelDisplay="on"
                    />
                  </div>
                  <PlusOutlined
                    onClick={() => handleZoomChange(zoomLevel + 1)}
                    className={`zoom-button-increase${
                      zoomLevel === 10 ? " disabled" : ""
                    }`}
                  />
                </div>
              </div>
            )}
          </div>
        )}
        {isEnableUpload && (
          <div
            className={`upload-container ${
              shouldEnableZoom ? "fixed-enabled" : "default"
            }`}
          >
            <input
              type="file"
              id={uploadButtonId || "upload-qr"}
              name={uploadButtonId || "upload-qr"}
              accept={acceptedFileTypes}
              className={`upload-button ${
                uploadButtonStyle || "upload-button-default"
              }`}
              onChange={handleUpload}
              disabled={isUploading}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default QRCodeVerification;
