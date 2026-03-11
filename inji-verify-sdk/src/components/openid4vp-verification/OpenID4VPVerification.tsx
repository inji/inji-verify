import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  AppError,
  SessionState,
  OpenID4VPVerificationProps,
  VerificationResults,
  CredentialResult
} from "./OpenID4VPVerification.types";
import {
  vpRequestStatus,
  vpRequest,
  vpResult,
  vpResultUsingResponseCode
} from "../../utils/api";
import "./OpenID4VPVerification.css";
import { clearUrl, normalizeVp } from "../../utils/utils";
import { QrData } from "../../types/OVPSchemeQrData";
import { CROSS_DEVICE_FLOW, SAME_DEVICE_FLOW } from "../../utils/constants";

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
  const sessionStateRef = useRef<SessionState | null>(null);

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

  const presentationFlow = isSameDeviceFlowEnabled ? SAME_DEVICE_FLOW : CROSS_DEVICE_FLOW;

  const resetState = useCallback(() => {
    if (redirectTimeoutRef.current) {
      clearTimeout(redirectTimeoutRef.current);
      redirectTimeoutRef.current = null;
    }
    setQrCodeData(null);
    setLoading(false);
    isActiveRef.current = false;
    sessionStateRef.current = null;
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
    (response: { credentialResults?: CredentialResult[]; transactionId?: string }) => {
        const VPResult: VerificationResults =
        (response.credentialResults ?? []).map((cred: CredentialResult) => ({
          vc: normalizeVp(cred.verifiableCredential),
          verificationResponse: cred,
        }));
      if (onVPProcessed) {
        onVPProcessed(VPResult);
      } else if (onVPReceived && (response.transactionId)) {
        onVPReceived(response.transactionId);
      }
    },
    [onVPProcessed, onVPReceived]
  );

  const fetchVPResult = useCallback(
    async (txnId: string) => {
      if (!isActiveRef.current) return;

      setLoading(true);

      try {
        if (onVPProcessed && txnId) {
          const response = await vpResult(
            verifyServiceUrl,
            txnId,
            vpVerificationV2Request
          );

          const VPResult: VerificationResults =
            (response.credentialResults ?? []).map((cred: CredentialResult) => {
              const vc = normalizeVp(cred.verifiableCredential);

              return {
                vc,
                verificationResponse: cred,
              };
            });

          onVPProcessed(VPResult);
          resetState();
          return;
        }

        if (onVPReceived && txnId && isActiveRef.current) {
          onVPReceived(txnId);
          resetState();
        }
      } catch (error) {
        if (isActiveRef.current) {
          onError(error as AppError);
          resetState();
        }
      }
    },
    [verifyServiceUrl, onVPProcessed, onVPReceived, onError, vpVerificationV2Request]
  );

  const fetchVPResultUsingResponseCode = useCallback(
    async (responseCode: string) => {
      if (!isActiveRef.current) return;

      setLoading(true);

      try {
          const response = await vpResultUsingResponseCode(
            verifyServiceUrl,
            responseCode,
            vpVerificationV2Request
          )

          processVPResultResponse(response);
          resetState();
          return;
      } catch (error) {
        if (isActiveRef.current) {
          onError(error as AppError);
          resetState();
        }
      } finally {
        clearUrl(["response_code"]);
      }
    },
    [verifyServiceUrl, onError, vpVerificationV2Request, processVPResultResponse]
  );

  const fetchVPStatus = useCallback(
    async (reqId: string, txnId: string) => {
      if (!isActiveRef.current) return;

      try {
        const response = await vpRequestStatus(verifyServiceUrl, reqId);

        if (response.status === "ACTIVE") {
          fetchVPStatus(reqId, txnId);
        } else if (response.status === "VP_SUBMITTED") {
          fetchVPResult(txnId);
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
    ]
  );

  const createVPRequest = useCallback(async (presentationFlow?: string) => {
    if (isActiveRef.current) return;
    isActiveRef.current = true;
    setLoading(true);
    try {
      const data = await vpRequest(
        verifyServiceUrl,
        clientId,
        transactionId ?? undefined,
        presentationDefinitionId,
        presentationDefinition,
        acceptVPWithoutHolderProof,
        presentationFlow
      );

      sessionStateRef.current = {
        requestId: data.requestId,
        transactionId: data.transactionId,
      };

      if (!isSameDeviceFlowEnabled || !isMobileDevice()) {
        fetchVPStatus(data.requestId, data.transactionId);
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
      handleGenerateQRCode();
    }
  };

  const handleGenerateQRCode = async () => {
    const pdParams = await createVPRequest(presentationFlow);
    if (pdParams) {
      const qrData = `${protocol || DEFAULT_PROTOCOL}authorize?${pdParams}`;
      setQrCodeData(qrData);
      setLoading(false);
    }
  };

  const startVerification = async () => {
    const pdParams = await createVPRequest(presentationFlow);
    if (!pdParams) return;

    if (webWalletBaseUrl) {
      let end = webWalletBaseUrl.length;
      while (end > 0 && webWalletBaseUrl[end - 1] === "/") end--;
      const baseUrl = webWalletBaseUrl.slice(0, end);
      window.location.href = `${baseUrl}/authorize?${pdParams}`;
    } else {
      window.location.href = `${protocol || DEFAULT_PROTOCOL}authorize?${pdParams}`;
    }
  };

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible" && isActiveRef.current && sessionStateRef.current) {
        const { requestId, transactionId } = sessionStateRef.current;
        fetchVPStatus(requestId, transactionId);
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
      const searchParams = new URLSearchParams(window.location.search);
      const responseCode = hashParams.get("response_code") ?? searchParams.get("response_code");
      if (responseCode) {
        isActiveRef.current = true;
        setLoading(true);
        fetchVPResultUsingResponseCode(responseCode);
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

