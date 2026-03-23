import React, { useEffect, useRef } from "react";
import ResultSummary from "./ResultSummary";
import { claim, VpSubmissionResultInt } from "../../../../types/data-types";
import VpVerifyResultSummary from "./VpVerifyResultSummary";
import DisplayVcCardView from "./DisplayVcCardView";
import { Button } from "../commons/Button";
import DisplayUnVerifiedVc from "./DisplayUnVerifiedVc";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";
import {useTranslation} from "react-i18next";
import {getCredentialType} from "../../../../utils/commonUtils";
import { resetVpRequest } from "../../../../redux/features/verify/vpVerificationState";
import { DisplayTimeout } from "../../../../utils/config";
import { useAppDispatch } from "../../../../redux/hooks";

type VpSubmissionResultProps = {
  verifiedVcs: VpSubmissionResultInt[];
  unverifiedCredentials: claim[];
  requestCredentials: () => void;
  requestMissingCredentials: () => void;
  restart: () => void;
  isSingleVc: boolean;
};

const VpSubmissionResult: React.FC<VpSubmissionResultProps> = ({
  verifiedVcs,
  unverifiedCredentials,
  requestCredentials,
  requestMissingCredentials,
  restart,
  isSingleVc,
}) => {
  const vcStatus = isSingleVc ? verifiedVcs[0]?.vcStatus : "INVALID";
  const originalSelectedCredentials: claim[] = useVerifyFlowSelector((state) => state.originalSelectedCredentials) || [];
  const isPartiallyShared = useVerifyFlowSelector((state) => state.isPartiallyShared );
  const showResult = useVerifyFlowSelector((state) => state.isShowResult );
  const { t } = useTranslation("Verify");
  const filterVerifiedVcs = verifiedVcs.filter((verifiedVc) =>
    originalSelectedCredentials.some((selectedVc) => getCredentialType(verifiedVc.vc) === (selectedVc.type))
  );
  const dispatch = useAppDispatch();
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  const renderRequestCredentialsButton = (propClasses = "") => (
    <div className={`flex flex-col items-center lg:hidden ${propClasses}`}>
      <Button
        id="request-credentials-button"
        title={t("Verify:rqstButton")}
        className={`w-[339px] mt-5`}
        variant="fill"
        onClick={requestCredentials}
      />
    </div>
  );

  const renderMissingAndResetButton = () => (
    <div className="flex flex-col items-center lg:hidden">
      <Button
        id="missing-credentials-button"
        title={t("missingCredentials")}
        className={`w-[300px] mt-5`}
        variant="fill"
        onClick={requestMissingCredentials}
      />
      <Button
        id="restart-process-button"
        title={t("restartProcess")}
        className={`w-[300px] mt-5`}
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
          unverifiedCredentials={unverifiedCredentials}
        />
      )}
      <div className="relative">
        <div className="flex flex-col items-center space-y-4 lg:space-y-6 mt-[-60px] lg:mt-[-70px]">
          {showResult && verifiedVcs.map(({ vc, vcStatus }, index) => (
            <DisplayVcCardView
              key={index}
              vc={vc}
              vcStatus={vcStatus}
              view={originalSelectedCredentials.length === 1}
            />
          ))}
          {unverifiedCredentials.length > 0 && unverifiedCredentials.map((claim,index) => (
            <DisplayUnVerifiedVc key={index} claim={claim} />
          ))}
        </div>
      </div>
      {isPartiallyShared
        ? renderMissingAndResetButton()
        : renderRequestCredentialsButton()}
    </div>
  );
};

export default VpSubmissionResult;