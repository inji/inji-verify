import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import {
  AppError,
  OpenID4VPVerificationProps,
  SessionState,
  VerificationResults,
  VerificationStatus,
} from "./OpenID4VPVerification.types";
import { vpRequest, vpRequestStatus, vpResult } from "../../utils/api";
import "./OpenID4VPVerification.css";
import { isSdJwt } from "../../utils/utils";
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
}) => {
  const [qrCodeData, setQrCodeData] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const isActiveRef = useRef(false);
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

  const clearSessionData = useCallback(() => {
    sessionStateRef.current = null;
  }, []);

  const resetState = useCallback(() => {
    setQrCodeData(null);
    setLoading(false);
    isActiveRef.current = false;
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
            verifyServiceUrl + data.authorizationDetails.presentationDefinitionUri
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
    [verifyServiceUrl, clientId]
  );

  const fetchVPResult = useCallback(
    async (txnId: string) => {
      if (!isActiveRef.current) return;
      setLoading(true);
      try {
        if (onVPProcessed && txnId) {
          const vcResults = await vpResult(verifyServiceUrl, txnId);
          if (!isActiveRef.current) return;

          if (vcResults && vcResults.length > 0) {
            const VPResult: VerificationResults = vcResults.map(
              (vcResult: { vc: any; verificationStatus: VerificationStatus }) => ({
                vc: isSdJwt(vcResult.vc) ? vcResult.vc : JSON.parse(vcResult.vc),
                vcStatus: vcResult.verificationStatus,
              })
            );
            onVPProcessed(VPResult);
            resetState();
            return;
          } else {
            throw new Error("Failed to get the VP result");
          }
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
    [verifyServiceUrl, onVPProcessed, onVPReceived, onError]
  );

  const fetchVPStatus = useCallback(
    async (reqId: string, txnId: string) => {
      if (!isActiveRef.current || !sessionStateRef.current) return;

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
      onError,
      fetchVPResult
    ]
  );

  const createVPRequest = useCallback(async () => {
    if (isActiveRef.current) return;
    isActiveRef.current = true;
    setLoading(true);
    try {
      const data = await vpRequest(
        verifyServiceUrl,
        clientId,
        transactionId ?? undefined,
        presentationDefinitionId,
        presentationDefinition
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
    clientId,
  ]);

  const handleTriggerClick = () => {
    if (isSameDeviceFlowEnabled && isMobileDevice()) {
      startVerification();
    } else {
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
    if (pdParams) {
      window.location.href = `${protocol || DEFAULT_PROTOCOL }authorize?${pdParams}`;
    }
  };

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        if (sessionStateRef.current && isActiveRef.current) {
          const { requestId, transactionId } = sessionStateRef.current;
          fetchVPStatus(requestId, transactionId);
        }
      }
    };

    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [fetchVPStatus]);

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
      if (isSameDeviceFlowEnabled && isMobileDevice()) {
        startVerification();
      } else {
        handleGenerateQRCode();
      }
    }
  }, [triggerElement, isSameDeviceFlowEnabled]);

  useEffect(() => {
    return () => {
      isActiveRef.current = false;
      clearSessionData();
    };
  }, [clearSessionData]);

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
