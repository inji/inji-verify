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
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [claims, setClaims] = useState<AnyVc | null>(null);
  const [credentialType, setCredentialType] = useState<string>("");
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  
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
    const fetchDecodedClaims = async () => {
      try {
        if (isCWT(vc)) {
          let cwtHex = vc as string;
          if (vc instanceof Uint8Array) cwtHex = uint8ArrayToHex(vc);
          if (vc instanceof ArrayBuffer) cwtHex = uint8ArrayToHex(new Uint8Array(vc));
          setClaims(extractMappedClaim(cwtHex, 169) as LdpVc);
          return;
        }
        if (typeof vc === "string") {
          const decodedClaims = await decodeSdJwtToken(vc);
          setClaims(decodedClaims as SdJwtVc);
          setCredentialType(decodedClaims.regularClaims.vct);
          return;
        }
        setClaims(vc as LdpVc);
        const typeEntry = vc.type[1];
        if (typeof typeEntry === "string") {
          setCredentialType(typeEntry);
        } else if (typeof typeEntry === "object" && "_value" in typeEntry) {
          setCredentialType(typeEntry._value);
        }
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        dispatch(raiseAlert({ message, severity: "error", open: true }));
      }
    };
    void fetchDecodedClaims().catch((err) => {
      const message = err instanceof Error ? err.message : String(err);
      dispatch(raiseAlert({ message, severity: "error", open: true }));
    });
  }, [dispatch, vc]);

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
        {claims && (
          <DisplayVcDetailView
            vc={claims}
            onExpand={() => setIsModalOpen(true)}
            className={`h-auto rounded-t-0 rounded-b-lg overflow-y-auto mt-[-30px]`}
          />
        )}
        <div className="grid content-center justify-center">
          <Button
            title={t("Common:Button.verifyAnotherQrCode")}
            onClick={handleVerifyAnotherQrCode}
            className="mx-auto mt-6 mb-20 w-[339px] lg:mb-6"
          />
        </div>
      </div>
      {claims && (
        <DisplayVcDetailsModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          vc={claims}
          status={vcStatus}
          vcType={credentialType}
        />
      )}
    </div>
  );
};

export default Result;
