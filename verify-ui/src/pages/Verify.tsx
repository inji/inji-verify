import React from "react";
import VerificationProgressTracker from "../components/Home/VerificationProgressTracker";
import { VpVerification } from "../components/Home/VerificationSection/VpVerification";
import SelectionPanel from "../components/Home/VerificationSection/commons/SelectionPanel";
import { Button } from "../components/Home/VerificationSection/commons/Button";
import { useTranslation } from "react-i18next";
import { useVerifyFlowSelector } from "../redux/features/verification/verification.selector";
import { getVpRequest, resetVpRequest, setSelectCredential } from "../redux/features/verify/vpVerificationState";
import { useAppDispatch } from "../redux/hooks";

export function Verify() {
  const { t } = useTranslation("Verify");
  const openSelection = useVerifyFlowSelector((state) => state.SelectionPanel);
  const dispatch = useAppDispatch();
  const unverifiedClaims = useVerifyFlowSelector((state) => state.unVerifiedClaims );
  const activeScreen = useVerifyFlowSelector((state) => state.activeScreen );

  const handleRequestCredentials = () => {
    dispatch(setSelectCredential());
  };

  const HandelGenerateQr = () => {
    dispatch(getVpRequest({ selectedClaims: unverifiedClaims }));
  };

  const HandelRestartProcess = () => {
    dispatch(resetVpRequest());
  };

  const renderRequestCredentialsButton = () => (
    <Button
      id="stepper-request-credentials-button"
      title={t("rqstButton")}
      className={`w-[300px] mx-auto lg:ms-[76px] mt-10 hidden lg:block`}
      variant="fill"
      onClick={handleRequestCredentials}
      disabled={activeScreen === 3 }
    />
  );

  const renderMissingAndResetButton = () => (
    <div className="hidden lg:flex items-center justify-around mt-10">
      <Button
        id="missing-credentials-button"
        title={t("missingCredentials")}
        className={`w-[250px]`}
        onClick={HandelGenerateQr}
      />
      <Button
        id="restart-process-button"
        title={t("restartProcess")}
        className={`w-[200px]`}
        onClick={HandelRestartProcess}
        variant="outline"
      />
    </div>
  );


  return (
      <div className="grid grid-cols-13 gap-y-8 lg:gap-0">
        <div className="col-start-1 col-end-13 lg:col-end-6 lg:bg-pageBackGroundColor w-full lg:max-w-[50vw] lg:pb-[100px] flex flex-col items-center">
          <VerificationProgressTracker />
          {unverifiedClaims.length > 0 ? renderMissingAndResetButton() : renderRequestCredentialsButton() }
          {openSelection && <SelectionPanel />}
        </div>
        <div className="col-start-1 col-end-13 lg:col-start-7 xs:w-[100vw] lg:max-w-[50vw]">
          <VpVerification />
        </div>
      </div>
  );
}
