import { useEffect, useRef, useState } from "react";
import ResultSummary from "./ResultSummary";
import { useVerificationFlowSelector } from "../../../../redux/features/verification/verification.selector";
import DisplayVcDetailsModal from "./DisplayVcDetailsModal";
import DisplayVcDetailView from "./DisplayVcDetailView";
import { Button } from "../commons/Button";
import { useTranslation } from "react-i18next";
import { useAppDispatch } from "../../../../redux/hooks";
import {
  goToHomeScreen,
  qrReadInit,
} from "../../../../redux/features/verification/verification.slice";
import { decodeSdJwtToken } from "../../../../utils/decodeSdJwt";
import { AnyVc, LdpVc, SdJwtVc } from "../../../../types/data-types";
import { DisplayTimeout } from "../../../../utils/config";
import { extractMappedClaim, isCWT, uint8ArrayToHex } from "../../../../utils/cborUtils";
import { raiseAlert } from "../../../../redux/features/alerts/alerts.slice";

const Result = () => {
  const { vc, vcStatus } = useVerificationFlowSelector((state) => state.verificationResult ?? { vc: null, vcStatus: null });
  const { method } = useVerificationFlowSelector((state) => ({ method: state.method }));
  const [isModalOpen, setModalOpen] = useState(false);
  const [claims, setClaims] = useState<AnyVc | null>(null);
  const [credentialType, setCredentialType] = useState<string>("");
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const shouldDecodeCredential = vcStatus === "SUCCESS" || vcStatus === "EXPIRED";
  
  const handleVerifyAnotherQrCode = () => {
    if (method === "SCAN") {
      dispatch(qrReadInit({ method: "SCAN" }));
    } else {
      dispatch(goToHomeScreen({}));
      setTimeout(() => {
        document.getElementById("upload-qr")?.click();
      }, 50);
    }
  };

  useEffect(() => {
    let active = true;

    const fetchDecodedClaims = async () => {
      setClaims(null);
      setCredentialType("");
      setModalOpen(false);

      if (!shouldDecodeCredential) {
        return;
      }

      if (isCWT(vc)) {
        try {
          const cwtHex =
            vc instanceof Uint8Array
              ? uint8ArrayToHex(vc)
              : vc instanceof ArrayBuffer
                ? uint8ArrayToHex(new Uint8Array(vc))
                : (vc as string);
          const claims = extractMappedClaim(cwtHex, 169);
          if (active) {
            setClaims(claims as LdpVc);
          }

        } catch (err) {
          const message = err instanceof Error ? err.message : String(err);
          if (active) {
            dispatch(raiseAlert({ message, severity: "error", open: true }));
          }

        }
      } else if (typeof vc === "string") {
        try {
          const claims = await decodeSdJwtToken(vc);
          if (active) {
            setClaims(claims as SdJwtVc);
            setCredentialType(claims.regularClaims.vct);
          }
        } catch (err) {
          const message = err instanceof Error ? err.message : String(err);
          if (active) {
            dispatch(raiseAlert({ message, severity: "error", open: true }));
          }
        }
      } else {
        if (!active || !vc) {
          return;
        }

        setClaims(vc as LdpVc);
        const typeEntry = vc.type[1];
        if (typeof typeEntry === "string") {
          setCredentialType(typeEntry);
        } else if (typeof typeEntry === "object" && "_value" in typeEntry) {
          setCredentialType(typeEntry._value);
        }
      }
    };

    void fetchDecodedClaims();

    return () => {
      active = false;
    };
  }, [dispatch, shouldDecodeCredential, vc]);

  const shouldShowCredentialDetails = shouldDecodeCredential && claims !== null;

  const clearTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  useEffect(() => {
    clearTimer();
    timerRef.current = setTimeout(() => {
       dispatch(goToHomeScreen({}));
    }, DisplayTimeout);

    return () => clearTimer();
  }, [dispatch]);

  return (
    <div id="result-section" className="relative mb-[100px]">
      <div className={`text-whiteText`}>
        <ResultSummary status={vcStatus} />
      </div>
      <div>
        <div className={`h-[3px] border-b-2 border-b-transparent`} />
        {shouldShowCredentialDetails && (
          <DisplayVcDetailView
            vc={claims}
            onExpand={() => setModalOpen(true)}
            className={`h-auto rounded-t-0 rounded-b-lg overflow-y-auto mt-[-30px]`}
          />
        )}
        <div className="grid content-center justify-center">
          <Button
            title={t("Common:Button.verifyAnotherQrCode")}
            onClick={handleVerifyAnotherQrCode}
            className="mx-auto mt-6 mb-20 lg:mb-6 lg:w-[339px]"
          />
        </div>
      </div>
      {shouldShowCredentialDetails && (
        <DisplayVcDetailsModal
          isOpen={isModalOpen}
          onClose={() => setModalOpen(false)}
          vc={claims}
          status={vcStatus}
          vcType={credentialType}
        />
      )}
    </div>
  );
};

export default Result;
