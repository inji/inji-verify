import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  AppError,
  SessionState,
  OpenID4VPVerificationProps,
  VerificationResults,
  CredentialResult,
  VPVerificationSummaryResponse,
  VerificationStatus,
} from "./OpenID4VPVerification.types";
import {
  vpRequestStatus,
  vpSessionRequest,
  vpSessionResults,
} from "../../utils/api";
import "./OpenID4VPVerification.css";
import { clearUrl, normalizeVp } from "../../utils/utils";
import { QrData } from "../../types/OVPSchemeQrData";


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

const OpenID4VPVerification: React.FC<OpenID4VPVerificationProps> = ({
  triggerElement,
  verifyServiceUrl,
  protocol,
  presentationDefinitionId,
  presentationDefinition,
  transactionId,
  onVPReceived,
  onVPProcessed,
  qrCodeStyles,
  onQrCodeExpired,
  onError,
  clientId,
  isSameDeviceFlowEnabled = true,
  acceptVPWithoutHolderProof = false,
  webWalletBaseUrl,
  vpVerificationV2Request
}) => {
  const [qrCodeData, setQrCodeData] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const isActiveRef = useRef(false);
  const redirectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const statusPollTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(
    null,
  );
  const statusRequestInFlightRef = useRef(false);
  const hasFetchedVPResultRef = useRef(false);
  const sessionStateRef = useRef<SessionState>({
    requestId: "",
  });
  const [isSameDeviceFlowWithRedirect,setIsSameDeviceFlowWithRedirect] = useState<boolean>(false);

  const [isCrossDeviceFlow,setIsCrossDeviceFlow] = useState<boolean>(false);

  const shouldShowQRCode = !loading && qrCodeData;

  const DEFAULT_PROTOCOL = "openid4vp://";

  const VPFormat = useMemo(
    () => ({
      ldp_vp: {
        proof_type: [
          "Ed25519Signature2018",
          "Ed25519Signature2020",
          "RsaSignature2018",
        ],
      },
      "vc+sd-jwt": {
        "sd-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
        "kb-jwt_alg_values": ["RS256", "ES256", "ES256K", "EdDSA"],
      },
    }),
    []
  );

  const clearSessionData = useCallback(() => {
    sessionStateRef.current = {
      requestId: "",
    };
  }, []);

  const resetState = useCallback(() => {
    if (redirectTimeoutRef.current) {
      clearTimeout(redirectTimeoutRef.current);
      redirectTimeoutRef.current = null;
    }
    if (statusPollTimeoutRef.current) {
      clearTimeout(statusPollTimeoutRef.current);
      statusPollTimeoutRef.current = null;
    }
    setQrCodeData(null);
    setLoading(false);
    isActiveRef.current = false;
    statusRequestInFlightRef.current = false;
    hasFetchedVPResultRef.current = false;
    clearSessionData();
  }, []);

  const getPresentationDefinitionParams = useCallback(
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
        if (data.authorizationDetails.presentationDefinitionUri) {
          params.set(
            "presentation_definition_uri",
            data.authorizationDetails.presentationDefinitionUri
          );
        } else {
          params.set(
            "presentation_definition",
            JSON.stringify(data.authorizationDetails.presentationDefinition)
          );
        }
        params.set(
          "client_metadata",
          JSON.stringify({
            client_name: clientId,
            vp_formats: VPFormat,
          })
        );
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
        const VPResult: VerificationResults = credentialResults.map(
          (cred: CredentialResult) => ({
            vc: normalizeVp(cred.verifiableCredential),
            verificationResponse: cred,
          }),
        );
        onVPProcessed(VPResult);
      } else if (onVPReceived && response.transactionId) {
        onVPReceived(response.transactionId ?? "");
      }
    },
    [onVPProcessed, onVPReceived]
  );

  const fetchVPResult = useCallback(
    async (responseCode?: string | null) => {
      if (!isActiveRef.current) return;
      if (hasFetchedVPResultRef.current) return;
      hasFetchedVPResultRef.current = true;

      setLoading(true);

      try {
        const response = await vpSessionResults(
          verifyServiceUrl,
          responseCode,
          vpVerificationV2Request,
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
    [verifyServiceUrl, onVPProcessed, onVPReceived, onError, vpVerificationV2Request]
  );

  const fetchVPStatus = useCallback(
    async (reqId: string) => {
      if (!isActiveRef.current) return;
      if (statusRequestInFlightRef.current) return;
      statusRequestInFlightRef.current = true;

      try {
        const response = await vpRequestStatus(verifyServiceUrl, reqId);

        if (response.status === "ACTIVE") {
          if (statusPollTimeoutRef.current) {
            clearTimeout(statusPollTimeoutRef.current);
          }
          statusPollTimeoutRef.current = setTimeout(() => {
            fetchVPStatus(reqId);
          }, 1000);
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
      } finally {
        statusRequestInFlightRef.current = false;
      }
    },
    [
      verifyServiceUrl,
      onQrCodeExpired,
      fetchVPResult,
    ]
  );

  const createVPRequest = useCallback(async () => {
    if (isActiveRef.current) return;
    isActiveRef.current = true;
    setLoading(true);
    try {
      const responseCodeValidationRequired = isSameDeviceFlowWithRedirect ? true : false;

      const data = await vpSessionRequest(
        verifyServiceUrl,
        clientId,
        transactionId ?? undefined,
        presentationDefinitionId,
        presentationDefinition,
        acceptVPWithoutHolderProof,
        responseCodeValidationRequired,
      );

      if (!isSameDeviceFlowWithRedirect) {
        sessionStateRef.current = {
          requestId: data.requestId,
        };
      }

      if (isCrossDeviceFlow) {
        fetchVPStatus(data.requestId);
      }

      return getPresentationDefinitionParams(data);
    } catch (error) {
      onError(error as AppError);
      resetState();
    }
  }, [
    verifyServiceUrl,
    transactionId,
    presentationDefinitionId,
    presentationDefinition,
    getPresentationDefinitionParams,
    onError,
    clientId
  ]);

  const handleTriggerClick = () => {
    if (isSameDeviceFlowEnabled) {
      startVerification();
    } else {
      setIsCrossDeviceFlow(true);
      handleGenerateQRCode();
    }
  };

  const handleGenerateQRCode = async () => {
    const pdParams = await createVPRequest();
    if (pdParams) {
      const qrData = `${protocol || DEFAULT_PROTOCOL}authorize?${pdParams}`;
      setQrCodeData(qrData);
      setLoading(false);
    }
  };

  const startVerification = async () => {
    const pdParams = await createVPRequest();
    if (!pdParams) return;

    if (webWalletBaseUrl) {
      setIsSameDeviceFlowWithRedirect(true);
      let end = webWalletBaseUrl.length;
      while (end > 0 && webWalletBaseUrl[end - 1] === "/") end--;
      const baseUrl = webWalletBaseUrl.slice(0, end);
      window.location.href = `${baseUrl}/authorize?${pdParams}`;
    } else {
      setIsSameDeviceFlowWithRedirect(false);
      window.location.href = `${protocol || DEFAULT_PROTOCOL}authorize?${pdParams}`;
    }
  };

  useEffect(() => {
    const handleVisibilityChange = () => {
      const requestId = sessionStateRef.current.requestId;
      if (
        document.visibilityState === "visible" && isActiveRef.current && requestId) {
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
      const hashParams = new URLSearchParams(window.location.hash.slice(1));
      const responseCode = hashParams.get("response_code") || null;
      console.log("responseCode", responseCode);
      if (responseCode) {
        fetchVPResult(responseCode);
      } else {
        const savedRequestId = sessionStateRef.current.requestId;

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
    if (!presentationDefinitionId && !presentationDefinition) {
      throw new Error(
        "Either presentationDefinitionId or presentationDefinition must be provided, but not both"
      );
    }
    if (presentationDefinitionId && presentationDefinition) {
      throw new Error(
        "Both presentationDefinitionId and presentationDefinition cannot be provided simultaneously"
      );
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
  }, [
    createVPRequest,
    onError,
    onQrCodeExpired,
    onVPProcessed,
    onVPReceived,
    presentationDefinition,
    presentationDefinitionId,
    triggerElement,
  ]);

  useEffect(() => {
    if (!triggerElement) {
      if (isSameDeviceFlowEnabled) {
        startVerification();
      } else {
        handleGenerateQRCode();
      }
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

