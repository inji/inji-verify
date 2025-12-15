import { useEffect, useRef, useState } from "react";
import ResultSummary from "./ResultSummary";
import { useVerificationFlowSelector } from "../../../../redux/features/verification/verification.selector";
import DisplayVcDetailsModal from "./DisplayVcDetailsModal";
import DisplayVcDetailView from "./DisplayVcDetailView";
import { Button } from "../commons/Button";
import StatusPopup from "../commons/StatusPopup";
import { useTranslation } from "react-i18next";
import { useAppDispatch } from "../../../../redux/hooks";
import {
  goToHomeScreen,
  qrReadInit,
} from "../../../../redux/features/verification/verification.slice";
import { decodeSdJwtToken } from "../../../../utils/decodeSdJwt";
import { AnyVc, LdpVc, SdJwtVc } from "../../../../types/data-types";
import { resetVpRequest } from "../../../../redux/features/verify/vpVerificationState";
import { DisplayTimeout } from "../../../../utils/config";

import acceptGif from "../../../../assets/truckpassTheme/accepted-gif.gif";
import rejectGif from "../../../../assets/truckpassTheme/rejected-gif.gif";
import rejectIcon from "../../../../assets/truckpassTheme/reject-icon.svg";
import acceptIcon from "../../../../assets/truckpassTheme/accept-icon.svg";

type ActionButtonProps = {
  title: string;
  color: string;
  iconSrc: string;
  onClick: () => void;
  ariaLabel: string;
};

const ActionButton = ({
  title,
  color,
  iconSrc,
  onClick,
  ariaLabel,
}: ActionButtonProps) => {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={ariaLabel}
      className="flex items-center gap-3 px-6 py-3 rounded-[10px] text-white font-semibold shadow-[0_10px_30px_rgba(0,0,0,0.12)] hover:opacity-95 transition"
      style={{ minWidth: 200, backgroundColor: color }}
    >
      <span className="inline-flex items-center justify-center w-6 h-6 rounded-full">
        <img src={iconSrc} alt={title} className="w-4 h-4" />
      </span>
      <span>{title}</span>
    </button>
  );
};

const Result = () => {
  console.log("VP Result component rendered ✅");

  const { vc, vcStatus } = useVerificationFlowSelector(
    (state) => state.verificationResult ?? { vc: null, vcStatus: null }
  );
  const { method } = useVerificationFlowSelector((state) => ({
    method: state.method,
  }));

  const [isModalOpen, setModalOpen] = useState(false);
  const [claims, setClaims] = useState<AnyVc | null>(null);
  const [credentialType, setCredentialType] = useState<string>("");

  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const [showAcceptPopup, setShowAcceptPopup] = useState(false);
  const [showRejectPopup, setShowRejectPopup] = useState(false);

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

  const handleAccept = () => setShowAcceptPopup(true);
  const handleReject = () => setShowRejectPopup(true);

  useEffect(() => {
    const fetchDecodedClaims = async () => {
      if (typeof vc === "string") {
        const claimsDecoded = await decodeSdJwtToken(vc);
        setClaims(claimsDecoded as SdJwtVc);
        setCredentialType(claimsDecoded.regularClaims.vct);
      } else if (vc) {
        setClaims(vc as LdpVc);
        if (Array.isArray((vc as any).type)) {
          const typeEntry = (vc as any).type[1];
          if (typeof typeEntry === "string") {
            setCredentialType(typeEntry);
          } else if (typeof typeEntry === "object" && "_value" in typeEntry) {
            setCredentialType(typeEntry._value);
          }
        }
      }
    };

    fetchDecodedClaims();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [vc]);

  const clearTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  useEffect(() => {
    clearTimer();
    timerRef.current = setTimeout(() => {
      dispatch(resetVpRequest());
    }, DisplayTimeout);

    return () => clearTimer();
  }, [dispatch]);

  return (
    <div id="result-section" className="relative mb-[100px]">
      <div className="text-whiteText">
        <ResultSummary status={vcStatus} />
      </div>

      <div>
        <div className="h-[3px] border-b-2 border-b-transparent" />

        {claims && (
          <DisplayVcDetailView
            vc={claims}
            onExpand={() => setModalOpen(true)}
            className="h-auto rounded-t-0 rounded-b-lg overflow-y-auto mt-[-30px]"
          />
        )}

        {/* Accept / Reject buttons */}
        {claims && (
          <div className="flex justify-center gap-6 mt-6 border border-dashed">
            <ActionButton
              title={t("Common:Button.reject")}
              color="#CB4241"
              iconSrc={rejectIcon}
              onClick={handleReject}
              ariaLabel="Reject Entry"
            />
            <ActionButton
              title={t("Common:Button.accept")}
              color="#1F9F60"
              iconSrc={acceptIcon}
              onClick={handleAccept}
              ariaLabel="Allow Entry"
            />
          </div>
        )}

        {/* Verify another QR code */}
        <div className="grid content-center justify-center">
          <Button
            title={t("Common:Button.verifyAnotherQrCode")}
            onClick={handleVerifyAnotherQrCode}
            className="mx-auto mt-6 mb-20 lg:mb-6 lg:w-[339px]"
          />
        </div>
      </div>

      {claims && (
        <DisplayVcDetailsModal
          isOpen={isModalOpen}
          onClose={() => setModalOpen(false)}
          vc={claims}
          status={vcStatus}
          vcType={credentialType}
        />
      )}

      {/* ACCEPT POPUP */}
      <StatusPopup
        isOpen={showAcceptPopup}
        onClose={() => setShowAcceptPopup(false)}
        title={t("Accepted:Accept.title")}
        description={t("Accepted:Accept.description")}
        buttonLabel={t("Accepted:Accept.button")}
        gifSrc={acceptGif}
        gifAlt="verified"
      />

      {/* REJECT POPUP */}
      <StatusPopup
        isOpen={showRejectPopup}
        onClose={() => setShowRejectPopup(false)}
        title={t("Rejected:Reject.title")}
        description={t("Rejected:Reject.description")}
        buttonLabel={t("Rejected:Reject.button")}
        gifSrc={rejectGif}
        gifAlt="rejected"
      />
    </div>
  );
};

export default Result;
