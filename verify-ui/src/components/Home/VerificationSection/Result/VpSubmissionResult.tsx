import React, { useEffect, useRef, useState } from "react";
import ResultSummary from "./ResultSummary";
import { claim, VpSubmissionResultInt } from "../../../../types/data-types";
import VpVerifyResultSummary from "./VpVerifyResultSummary";
import DisplayVcCardView from "./DisplayVcCardView";
import { Button } from "../commons/Button";
import DisplayUnVerifiedVc from "./DisplayUnVerifiedVc";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";
import { useTranslation } from "react-i18next";
import { getCredentialType } from "../../../../utils/commonUtils";
import { resetVpRequest } from "../../../../redux/features/verify/vpVerificationState";
import { DisplayTimeout } from "../../../../utils/config";
import { useAppDispatch } from "../../../../redux/hooks";

import StatusPopup from "../commons/StatusPopup";
import acceptGif from "../../../../assets/truckpassTheme/accepted-gif.gif";
import rejectGif from "../../../../assets/truckpassTheme/rejected-gif.gif";
import rejectIcon from "../../../../assets/truckpassTheme/reject-icon.svg";
import acceptIcon from "../../../../assets/truckpassTheme/accept-icon.svg";

type VpSubmissionResultProps = {
  verifiedVcs: VpSubmissionResultInt[];
  unverifiedClaims: claim[];
  requestCredentials: () => void;
  requestMissingCredentials: () => void;
  restart: () => void;
  isSingleVc: boolean;
};

// Rectangular decision button for Accept / Reject
const DecisionButton = ({
  title,
  color,
  iconSrc,
  onClick,
}: {
  title: string;
  color: string;
  iconSrc: string;
  onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    className="flex items-center gap-3 px-6 py-3 rounded-[10px] text-white font-semibold shadow-[0_10px_30px_rgba(0,0,0,0.12)] hover:opacity-95 transition"
    style={{ minWidth: 200, backgroundColor: color }}
  >
    <span className="inline-flex items-center justify-center w-6 h-6 rounded-full">
      <img src={iconSrc} alt={title} className="w-4 h-4" />
    </span>
    <span>{title}</span>
  </button>
);

const VpSubmissionResult: React.FC<VpSubmissionResultProps> = ({
  verifiedVcs,
  unverifiedClaims,
  requestCredentials,
  requestMissingCredentials,
  restart,
  isSingleVc,
}) => {
  const vcStatus = isSingleVc ? verifiedVcs[0].vcStatus : "INVALID";
  const originalSelectedClaims: claim[] =
    useVerifyFlowSelector((state) => state.originalSelectedClaims) || [];
  const isPartiallyShared = useVerifyFlowSelector(
    (state) => state.isPartiallyShared
  );
  const showResult = useVerifyFlowSelector((state) => state.isShowResult);

  const { t } = useTranslation();
  const filterVerifiedVcs = verifiedVcs.filter((verifiedVc) =>
    originalSelectedClaims.some(
      (selectedVc) => getCredentialType(verifiedVc.vc) === selectedVc.type
    )
  );

  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const [showAcceptPopup, setShowAcceptPopup] = useState(false);
  const [showRejectPopup, setShowRejectPopup] = useState(false);

  const renderRequestCredentialsButton = (propClasses = "") => (
    <div className={`flex flex-col items-center lg:hidden ${propClasses}`}>
      <Button
        id="request-credentials-button"
        title={t("Verify:rqstButton")}
        className="w-[339px] mt-5"
        variant="fill"
        onClick={requestCredentials}
      />
    </div>
  );

  const renderMissingAndResetButton = () => (
    <div className="flex flex-col items-center lg:hidden">
      <Button
        id="missing-credentials-button"
        title={t("Verify:missingCredentials")}
        className="w-[300px] mt-5"
        variant="fill"
        onClick={requestMissingCredentials}
      />
      <Button
        id="restart-process-button"
        title={t("Verify:restartProcess")}
        className="w-[300px] mt-5"
        onClick={restart}
      />
    </div>
  );

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
    <div className="space-y-6 mb-[100px] lg:mb-0">
      {isSingleVc && verifiedVcs.length > 0 ? (
        <ResultSummary status={vcStatus} />
      ) : (
        <VpVerifyResultSummary
          verifiedVcs={[...filterVerifiedVcs]}
          unverifiedClaims={unverifiedClaims}
        />
      )}

      <div className="relative">
        <div className="flex flex-col items-center space-y-4 lg:space-y-6 mt-[-60px] lg:mt-[-70px]">
          {showResult &&
            verifiedVcs.map(({ vc, vcStatus }, index) => (
              <DisplayVcCardView
                key={index}
                vc={vc}
                vcStatus={vcStatus}
                view={isSingleVc}
              />
            ))}

          {unverifiedClaims.length > 0 &&
            unverifiedClaims.map((claim, index) => (
              <DisplayUnVerifiedVc key={index} claim={claim} />
            ))}
        </div>
      </div>

      {/* Accept / Reject buttons – only when single VC result is shown */}
      {isSingleVc && verifiedVcs.length > 0 && showResult && (
        <div className="flex justify-center gap-6 mt-6">
          <DecisionButton
            title={t("Common:Button.reject")}
            color="#CB4241"
            iconSrc={rejectIcon}
            onClick={() => setShowRejectPopup(true)}
          />
          <DecisionButton
            title={t("Common:Button.accept")}
            color="#1F9F60"
            iconSrc={acceptIcon}
            onClick={() => setShowAcceptPopup(true)}
          />
        </div>
      )}

      {isPartiallyShared
        ? renderMissingAndResetButton()
        : renderRequestCredentialsButton()}

      {/* Accept popup */}
      <StatusPopup
        isOpen={showAcceptPopup}
        onClose={() => setShowAcceptPopup(false)}
        title={t("Accepted:Accept.title")}
        description={t("Accepted:Accept.description")}
        buttonLabel={t("Accepted:Accept.button")}
        gifSrc={acceptGif}
        gifAlt="verified"
      />

      {/* Reject popup */}
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

export default VpSubmissionResult;
