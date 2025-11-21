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
import { resetVpRequest } from "../../../../redux/features/verify/vpVerificationState";
import { DisplayTimeout } from "../../../../utils/config";

// <-- Confirm these file names/paths exist in your repo. If not, update imports.
import acceptGif from "../../../../assets/truckpassTheme/accepted-gif.gif";
import rejectGif from "../../../../assets/truckpassTheme/rejected-gif.gif";
import rejectIcon from "../../../../assets/truckpassTheme/reject-icon.svg";
import acceptIcon from "../../../../assets/truckpassTheme/accept-icon.svg";



const Result = () => {
  const { vc, vcStatus } = useVerificationFlowSelector(
    (state) => state.verificationResult ?? { vc: null, vcStatus: null }
  );
  const { method } = useVerificationFlowSelector((state) => ({
    method: state.method,
  }));
  const [isModalOpen, setModalOpen] = useState(false);
  const [claims, setClaims] = useState<AnyVc | null>(null);
  const [credentialType, setCredentialType] = useState<string>("");
  const { t } = useTranslation("");
  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // popup states
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

  const handleAccept = () => {
    setShowAcceptPopup(true);
  };

  const handleReject = () => {
    setShowRejectPopup(true);
  };

  useEffect(() => {
    const fetchDecodedClaims = async () => {
      if (typeof vc === "string") {
        const claims = await decodeSdJwtToken(vc);
        setClaims(claims as SdJwtVc);
        setCredentialType(claims.regularClaims.vct);
      } else {
        setClaims(vc as LdpVc);
        if (vc && Array.isArray((vc as any).type)) {
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
      <div className={`text-whiteText`}>
        <ResultSummary status={vcStatus} />
      </div>

      <div>
        <div className={`h-[3px] border-b-2 border-b-transparent`} />

        {claims && (
          <DisplayVcDetailView
            vc={claims}
            onExpand={() => setModalOpen(true)}
            className={`h-auto rounded-t-0 rounded-b-lg overflow-y-auto mt-[-30px]`}
          />
        )}

        {/* Accept / Reject buttons (side-by-side) */}
        <div className="flex justify-center gap-6 mt-6">
          <button
            type="button"
            onClick={handleReject}
            aria-label="Reject Entry"
            className="flex items-center gap-3 px-6 py-3 rounded-lg bg-[#CB4241] text-white font-semibold shadow-lg hover:opacity-95"
            style={{ minWidth: 160 }}
          >
            {/* Small icon (optional). Keep plain text if you prefer) */}
            <span className="inline-flex items-center justify-center w-6 h-6 rounded-full">
              <img src={rejectIcon} alt="Reject" className="w-4 h-4" />
            </span>

            <span>{t("Common.Button.reject")}</span>
          </button>

          <button
            type="button"
            onClick={handleAccept}
            aria-label="Allow Entry"
            className="flex items-center gap-3 px-6 py-3 rounded-lg bg-[#1F9F60] text-white font-semibold shadow-lg hover:opacity-95"
            style={{ minWidth: 160 }}
          >
            <span className="inline-flex items-center justify-center w-6 h-6 rounded-full">
              <img src={acceptIcon} alt="Accept" className="w-4 h-4" />
            </span>
            <span>{t("Common.Button.accept")}</span>
          </button>
        </div>

        {/* Verify another QR code */}
        <div className="grid content-center justify-center">
          <Button
            title={t("Common:Button.verifyAnotherQrCode")}
            onClick={handleVerifyAnotherQrCode}
            className="mx-auto mt-6 mb-20 lg:mb-6 lg:w-[339px]"
          />
        </div>
      </div>

      {/* Details modal */}
      {claims && (
        <DisplayVcDetailsModal
          isOpen={isModalOpen}
          onClose={() => setModalOpen(false)}
          vc={claims}
          status={vcStatus}
          vcType={credentialType}
        />
      )}

      {/* Accept popup */}
      {showAcceptPopup && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-8 w-[480px] max-w-[90vw] text-center shadow-xl">
            {acceptGif && (
              <img
                src={acceptGif}
                alt="verified"
                className="mx-auto w-28 mb-4"
              />
            )}
            <h2 className="text-2xl font-semibold mb-2">{t("Accepted.Accept.title")}</h2>
            <p className="text-gray-700 mb-6">
              {t("Accepted.Accept.description")}
            </p>

            <button
              onClick={() => setShowAcceptPopup(false)}
              className="px-6 py-3 rounded-lg bg-[#006DE7] text-white font-semibold w-full"
            >
              {t("Accepted.Accept.button")}
            </button>
          </div>
        </div>
      )}

      {/* Reject popup */}
      {showRejectPopup && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-8 w-[480px] max-w-[90vw] text-center shadow-xl">
            {rejectGif && (
              <img
                src={rejectGif}
                alt="rejected"
                className="mx-auto w-28 mb-4"
              />
            )}
            <h2 className="text-2xl font-semibold mb-2">{t("Rejected.Reject.title")}</h2>
            <p className="text-gray-700 mb-6">{t("Rejected.Reject.description")}</p>

            <button
              onClick={() => setShowRejectPopup(false)}
              className="px-6 py-3 rounded-lg bg-[#006DE7] text-white font-semibold w-full"
            >
              {t("Rejected.Reject.button")}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Result;
