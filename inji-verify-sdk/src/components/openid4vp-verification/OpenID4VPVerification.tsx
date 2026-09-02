import "./OpenID4VPVerification.css";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  AppError,
  OpenID4VPVerificationProps,
  VerificationResults,
  CredentialResult,
  DcApiSubmissionData
} from "./OpenID4VPVerification.types";
import {
  vpRequestStatus,
  vpSessionRequest,
  vpSessionResults,
  getVpRequestJwt,
  vpResultSubmission,
  isAppError,
} from "../../utils/api";
import {clearUrl, summariseVPResult, normalizeVp, isMobileDevice, normalizeDcApiTimeoutMs, isDcApiSupported, isSignedRequestScheme, isRedirectUriClientId} from "../../utils/utils";
import { QrData } from "../../types/OVPSchemeQrData";
import { DC_API_PROTOCOL, DEFAULT_DC_API_TIMEOUT_MS, DEFAULT_PROTOCOL, VP_FORMATS_SUPPORTED } from "../../utils/constants";

const OpenID4VPVerification: React.FC<OpenID4VPVerificationProps> = ({
  triggerElement,
  verifyServiceUrl,
  protocol,
  dcqlQuery,
  transactionId,
  onVPReceived,
  onVPProcessed,
  qrCodeStyles,
  onQrCodeExpired,
  onError,
  clientId,
  isSameDeviceFlowEnabled = true,
  enableDcApi = false,
  dcApiTimeoutMs = DEFAULT_DC_API_TIMEOUT_MS,
  webWalletBaseUrl,
  vpVerificationRequest,
  summariseResults = true
}) => {
  const [qrCodeData, setQrCodeData] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const isActiveRef = useRef(false);
  const requestIdRef = useRef("");

  const shouldShowQRCode = !loading && qrCodeData;

  const resetState = useCallback(() => {
    setQrCodeData(null);
    setLoading(false);
    isActiveRef.current = false;
    requestIdRef.current = "";
  }, []);

  const getAuthorizationRequestParams = useCallback(
    (data: QrData) => {
      const params = new URLSearchParams();
      params.set("client_id", clientId);
      if (data.requestUri) {
        params.set("request_uri", data.requestUri);
      } else if (data.authorizationDetails) {
        params.set("state", data.requestId);
        params.set("response_mode", data.authorizationDetails.responseMode);
        params.set("response_type", data.authorizationDetails.responseType);
        params.set("nonce", data.authorizationDetails.nonce);
        params.set("response_uri", data.authorizationDetails.responseUri);
        if (data.authorizationDetails.dcqlQuery) {
          params.set("dcql_query", JSON.stringify(data.authorizationDetails.dcqlQuery));
        }
        if (isSignedRequestScheme(clientId) || isRedirectUriClientId(clientId)) {
          params.set(
            "client_metadata",
            JSON.stringify({
              vp_formats_supported: VP_FORMATS_SUPPORTED,
            })
          );
        }
      }
      return params.toString();
    },
    [clientId]
  );

  const processVPResultResponse = useCallback(
      (response: {
            credentialResults?: CredentialResult[];
            transactionId?: string;
        }) => {
            const credentialResults = response.credentialResults ?? [];

            if (onVPProcessed) {
                if (summariseResults) {
                    const vcResults = credentialResults.map((cred) => {
                        const vc = normalizeVp(cred.verifiableCredential);
                        const vcStatus = summariseVPResult(cred);
                        return { vc, vcStatus };
                    });

                    const vpResultStatus = credentialResults.length > 0 &&
                    credentialResults.every((c: CredentialResult) => c.allChecksSuccessful)
                        ? "SUCCESS" : "INVALID";

                    const result: VerificationResults = vcResults.map(v => ({
                        vc: v.vc,
                        verificationResponse: {
                            vcResults,
                            vpResultStatus,
                        },
                    }));

                    onVPProcessed(result);
                } else {
                    const VPResult: VerificationResults = credentialResults.map(
                        (cred: CredentialResult) => ({
                            vc: normalizeVp(cred.verifiableCredential),
                            verificationResponse: cred,
                        }),
                    );
                    onVPProcessed(VPResult);}
            } else if (onVPReceived && response.transactionId) {
                onVPReceived(response.transactionId);
            }
        },
        [onVPProcessed, onVPReceived, summariseResults]
    );
  const fetchVPResult = useCallback(async (responseCode?: string | null) => {
      if (!isActiveRef.current) return;
      setLoading(true);

      try {
        const response = await vpSessionResults(
          verifyServiceUrl,
          responseCode,
          vpVerificationRequest,
        );

        if (!response) {
          throw new Error(
            "An unexpected error occurred while processing the VP session result. Empty response.",
          );
        }

        processVPResultResponse(response);
        resetState();
      } catch (error) {
        if (isActiveRef.current) {
          onError(error as AppError);
          resetState();
        }
      } finally {
        clearUrl(["response_code"]);
      }
    },
    [verifyServiceUrl, onVPProcessed, onVPReceived, onError, vpVerificationRequest]
  );

  const fetchVPStatus = useCallback(
    async (reqId: string) => {
      if (!isActiveRef.current) return;

      try {
        const response = await vpRequestStatus(verifyServiceUrl, reqId);

        if (!isActiveRef.current) return;

        if (response.status === "ACTIVE") {
            fetchVPStatus(reqId);
        } else if (response.status === "VP_SUBMITTED") {
          fetchVPResult();
        } else if (response.status === "EXPIRED") {
          resetState();
          onQrCodeExpired();
        }
      } catch (error) {
        if (isActiveRef.current) {
          setLoading(false);
          resetState();
          onError(error as AppError);
        }
      }
    },
    [
      verifyServiceUrl,
      onQrCodeExpired,
      fetchVPResult,
      resetState,
      onError,
    ]
  );

  const createVPRequest = useCallback(async (isCrossDeviceFlow: boolean, responseMode: "direct_post" | "dc_api") => {
    if (isActiveRef.current) return;
    isActiveRef.current = true;
    setLoading(true);
    try {
      const responseCodeValidationRequired =
        responseMode !== "dc_api" && !isCrossDeviceFlow;

      const data = await vpSessionRequest(
        verifyServiceUrl,
        dcqlQuery,
        clientId,
        transactionId ?? undefined,
        responseCodeValidationRequired,
        responseMode,
      );

      if (responseMode !== "dc_api" && (isCrossDeviceFlow || webWalletBaseUrl == null)) {
        requestIdRef.current = data.requestId;
      }
      if (responseMode !== "dc_api" && isCrossDeviceFlow) {
        fetchVPStatus(data.requestId);
      }
      return data;
    } catch (error) {
      onError(error as AppError);
      resetState();
    }
  }, [
    verifyServiceUrl,
    transactionId,
    dcqlQuery,
    onError,
    clientId,
    webWalletBaseUrl,
    fetchVPStatus,
    resetState,
  ]);

  const processQRCodeGenerationFlow = async () => {
    const data = await createVPRequest(true, "direct_post");
    if (data) {
      const authParams = getAuthorizationRequestParams(data);
      const qrData = `${protocol || DEFAULT_PROTOCOL}authorize?${authParams}`;
      setQrCodeData(qrData);
      setLoading(false);
    }
  };

  const processDeepLinkFlow = async () => {
    // Web wallet redirect works on desktop and mobile when webWalletBaseUrl is set.
    // Desktop cannot launch a native wallet protocol, so same-device without a web
    // wallet URL (and without DC API) is invalid.
    if (!webWalletBaseUrl && !isMobileDevice()) {
      onError({
        errorMessage:"On desktop, same-device flow requires a web wallet URL (webWalletBaseUrl.",
        errorCode: "MISSING_WEB_WALLET_BASE_URL"
      });
      return;
    }

    const data = await createVPRequest(false, "direct_post");
    if (!data) return;
    const authParams = getAuthorizationRequestParams(data);

    if (webWalletBaseUrl) {
      let end = webWalletBaseUrl.length;
      while (end > 0 && webWalletBaseUrl[end - 1] === "/") end--;
      const baseUrl = webWalletBaseUrl.slice(0, end);
      window.location.href = `${baseUrl}/authorize?${authParams}`;
      return;
    }

    window.location.href = `${protocol || DEFAULT_PROTOCOL}authorize?${authParams}`;
  };

  const processDcAPIFlow = async () => {
    const controller = new AbortController();
    const timeoutMs = normalizeDcApiTimeoutMs(dcApiTimeoutMs);
    const timeoutId = window.setTimeout(() => controller.abort("DC_API_TIMEOUT"), timeoutMs);

    try {
      const data = await createVPRequest(false, "dc_api");
      if (!data) return;

      if (!data.requestUri) {
        onError({
          errorCode: "NO_AUTH_REQUEST",
          errorMessage: "VP session response missing requestUri",
        });
        resetState();
        return;
      }

      if (!data.responseUri) {
        onError({
          errorCode: "DC_API_MISSING_RESPONSE_URI",
          errorMessage: "VP session response missing responseUri for DC API submission",
        });
        resetState();
        return;
      }

      const signedJwt = await getVpRequestJwt(data.requestUri, controller.signal);
      const credential = await navigator.credentials.get({
        signal: controller.signal,
        digital: {
          requests: [
            {
              protocol: DC_API_PROTOCOL,
              data: { request: signedJwt },
            },
          ],
        },
      });

      if (!credential) {
        onError({
          errorCode: "DC_API_NO_CREDENTIAL",
          errorMessage: "No digital credential was returned",
        });
        resetState();
        return;
      }

      let submissionPayloadData: DcApiSubmissionData;
      try {
        const raw = credential.data;
        submissionPayloadData = (typeof raw === "string" ? JSON.parse(raw) : raw) as DcApiSubmissionData;
      } catch (error) {
        onError({
          errorCode: "DC_API_INVALID_CREDENTIAL_DATA",
          errorMessage:
            error instanceof Error
              ? error.message
              : "Digital credential data is malformed",
        });
        resetState();
        return;
      }

      try {
        await vpResultSubmission(data.responseUri, data.requestId, submissionPayloadData);
        await fetchVPResult();
      } catch (error) {
        onError(
          isAppError(error)
            ? error
            : {
                errorCode: "DC_API_SUBMIT_FAILED",
                errorMessage:
                  error instanceof Error ? error.message : "Failed to submit DC API presentation",
              },
        );
        resetState();
        return;
      }
      
    } catch (err) {
      const name = err instanceof DOMException ? err.name : "";
      const abortedForTimeout =
        controller.signal.aborted && controller.signal.reason === "DC_API_TIMEOUT";

      if (abortedForTimeout || name === "TimeoutError") {
        onError({
          errorCode: "DC_API_TIMEOUT",
          errorMessage: "Credential request timed out",
        });
        resetState();
        return;
      }
      if (name === "AbortError" || name === "NotAllowedError") {
        onError({
          errorCode: "DC_API_CANCELLED",
          errorMessage: err instanceof Error ? err.message : "Credential request cancelled",
        });
        resetState();
        return;
      }
      if (isAppError(err)) {
        onError(err);
        resetState();
        return;
      }
      onError({
        errorCode: "DC_API_ERROR",
        errorMessage: err instanceof Error ? err.message : "Digital Credentials API failed",
      });
      resetState();
    } finally {
      window.clearTimeout(timeoutId);
    }
  };

  const startVerificationFlow = () => {
    if (!isSameDeviceFlowEnabled) {
      processQRCodeGenerationFlow();
      return;
    }
    // Prefer DC API when enabled and supported; otherwise fall back to deep-link
    // silently (unsupported browser / client_id must not surface DC_API_NOT_SUPPORTED).
    if (enableDcApi && !webWalletBaseUrl && isDcApiSupported(clientId)) {
      processDcAPIFlow();
      return;
    }
    processDeepLinkFlow();
  };

  const handleTriggerClick = () => {
    startVerificationFlow();
  };

  useEffect(() => {
    const handleVisibilityChange = () => {
      const requestId = requestIdRef.current;
      if (
        document.visibilityState === "visible" &&
        isActiveRef.current &&
        requestId
      ) {
        fetchVPStatus(requestId);
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [fetchVPStatus]);

  useEffect(() => {
    if (!isActiveRef.current) {
      const hash = window.location.hash;
      const params = new URLSearchParams(hash.substring(1));
      const responseCode = params.get("response_code");
      if (responseCode) {
        isActiveRef.current = true;
        fetchVPResult(responseCode);
      } else {
        const savedRequestId = requestIdRef.current;

        if (savedRequestId) {
          isActiveRef.current = true;
          setLoading(true);
          fetchVPStatus(savedRequestId);
        }
      }
    }

    return () => {
      isActiveRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!dcqlQuery) {
      throw new Error("dcqlQuery must be provided");
    }
    if (!onVPReceived && !onVPProcessed) {
      throw new Error(
        "Either onVpReceived or onVpProcessed must be provided, but not both"
      );
    }
    if (onVPReceived && onVPProcessed) {
      throw new Error(
        "Both onVPReceived and onVPProcessed cannot be provided simultaneously"
      );
    }
    if (!onQrCodeExpired) {
      throw new Error("onQrCodeExpired callback is required");
    }
    if (!onError) {
      throw new Error("onError callback is required");
    }
    if (enableDcApi && webWalletBaseUrl) {
      throw new Error(
        "enableDcApi and webWalletBaseUrl cannot be used together. Choose either Digital Credentials API or a web wallet redirect."
      );
    }
  }, [
    createVPRequest,
    onError,
    onQrCodeExpired,
    onVPProcessed,
    onVPReceived,
    dcqlQuery,
    triggerElement,
    enableDcApi,
    webWalletBaseUrl,
  ]);

  useEffect(() => {
    if (!triggerElement) {
      startVerificationFlow();
    }
  }, [triggerElement, isSameDeviceFlowEnabled]);

  return (
    <div className={"ovp-root-div-container"}>
      {loading && <div id="ovp-loader" className={"ovp-loader"} />}

      {!loading && triggerElement && !qrCodeData && (
        <div onClick={handleTriggerClick} style={{ cursor: "pointer" }}>
          {triggerElement}
        </div>
      )}

      {shouldShowQRCode && (
        <QRCodeSVG
          value={qrCodeData}
          size={qrCodeStyles?.size || 200}
          level={qrCodeStyles?.level || "L"}
          bgColor={qrCodeStyles?.bgColor || "#ffffff"}
          fgColor={qrCodeStyles?.fgColor || "#000000"}
          marginSize={qrCodeStyles?.margin || 10}
          style={{ borderRadius: qrCodeStyles?.borderRadius || 10 }}
        />
      )}
    </div>
  );
};

export default OpenID4VPVerification;

