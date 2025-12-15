import { useEffect } from "react";
import { QrIcon, ScanOutline } from "../../../utils/theme-utils";
import { useVerifyFlowSelector } from "../../../redux/features/verification/verification.selector";
import Loader from "../../commons/Loader";
import VpSubmissionResult from "./Result/VpSubmissionResult";
import { useAppDispatch } from "../../../redux/hooks";
import {
  getVpRequest,
  resetVpRequest,
  setSelectCredential,
  verificationSubmissionComplete,
} from "../../../redux/features/verify/vpVerificationState";
import { VCShareType, VpSubmissionResultInt } from "../../../types/data-types";
import { closeAlert, raiseAlert } from "../../../redux/features/alerts/alerts.slice";
import { AlertMessages } from "../../../utils/config";
import { OpenID4VPVerification } from "@mosip/react-inji-verify-sdk";
import { Button } from "./commons/Button";
import { useTranslation } from "react-i18next";
import { VerificationResults } from "@mosip/react-inji-verify-sdk/dist/components/openid4vp-verification/OpenID4VPVerification.types";
import { decodeSdJwtToken } from "../../../utils/decodeSdJwt";

const DisplayActiveStep = () => {
  const { t } = useTranslation("Verify");
  const isLoading = useVerifyFlowSelector((state) => state.isLoading);
  const sharingType = useVerifyFlowSelector((state) => state.sharingType);
  const isSingleVc = sharingType === VCShareType.SINGLE;
  const selectedClaims = useVerifyFlowSelector((state) => state.selectedClaims);
  const verifiedVcs: VpSubmissionResultInt[] = useVerifyFlowSelector(
    (state) => state.verificationSubmissionResult
  );
  const unverifiedClaims = useVerifyFlowSelector(
    (state) => state.unVerifiedClaims
  );
  const presentationDefinition = useVerifyFlowSelector(
    (state) => state.presentationDefinition
  );
  const qrSize = window.innerWidth <= 1024 ? 240 : 320;
  const activeScreen = useVerifyFlowSelector((state) => state.activeScreen);
  const showResult = useVerifyFlowSelector((state) => state.isShowResult);
  const flowType = useVerifyFlowSelector((state) => state.flowType);
  const incorrectCredentialShared =
    selectedClaims.length === 1 &&
    unverifiedClaims.length === 1 &&
    isSingleVc;

  const dispatch = useAppDispatch();

  const handleRequestCredentials = () => {
    dispatch(setSelectCredential());
  };

  const handleMissingCredentials = () => {
    dispatch(getVpRequest({ selectedClaims: unverifiedClaims }));
  };

  const handleRestartProcess = () => {
    dispatch(resetVpRequest());
  };

  const handleOnVpProcessed = async (vpResults: VerificationResults) => {
    const decodedVpResults = await Promise.all(
      vpResults.map(async (vpResult) => {
        if (typeof vpResult?.vc === "string") {
          const decodedSdJwt = await decodeSdJwtToken(vpResult.vc);
          return { ...vpResult, vc: decodedSdJwt };
        }
        return vpResult;
      })
    );
    dispatch(
      verificationSubmissionComplete({ verificationResult: decodedVpResults })
    );
  };

  const handleOnQrExpired = () => {
    dispatch(raiseAlert({ ...AlertMessages().sessionExpired, open: true }));
    dispatch(resetVpRequest());
  };

  const handleOnError = (error: any) => {
    dispatch(closeAlert({}));
    dispatch(resetVpRequest());
    if (error.errorCode) {
      error.message =
        "We’re unable to complete your request. Please contact support for assistance.";
    }
    dispatch(
      raiseAlert({
        title: "Request Failed",
        errorCode: error.errorCode,
        errorReason: error.errorMessage,
        message: error.message,
        referenceId: error.transactionId,
        severity: "error",
        open: true,
        autoHideDuration: 120000,
      })
    );
  };

  const getClientId = () => {
    return isSingleVc && selectedClaims[0]?.isAuthRequestEmbedded
      ? window._env_.CLIENT_ID
      : window._env_.CLIENT_ID_DID;
  };

  useEffect(() => {
    if (selectedClaims.length > 0 && activeScreen === 3) {
      setTimeout(() => {
        const triggerElement = document.getElementById(
          "OpenID4VPVerification_trigger"
        );
        if (triggerElement) {
          const event = new MouseEvent("click", {
            bubbles: true,
            cancelable: true,
          });
          triggerElement.dispatchEvent(event);
        }
      }, 100);
    }
  }, [selectedClaims, activeScreen]);

  if (isLoading) {
    return <Loader className="absolute lg:top-[200px] right-[100px]" />;
  }

  if (incorrectCredentialShared) {
    dispatch(resetVpRequest());
    dispatch(
      raiseAlert({ ...AlertMessages().incorrectCredential, open: true })
    );
    return null;
  }

  if (showResult) {
    return (
      <div className="w-[100vw] lg:w-[50vw] display-flex flex-col items-center justify-center">
        <VpSubmissionResult
          verifiedVcs={verifiedVcs}
          unverifiedClaims={unverifiedClaims}
          requestCredentials={handleRequestCredentials}
          requestMissingCredentials={handleMissingCredentials}
          restart={handleRestartProcess}
          isSingleVc={isSingleVc}
        />
      </div>
    );
  } else if (flowType === "crossDevice") {
    return (
      <div className="flex flex-col mt-10 lg:mt-0 pt-0 pb-[100px] lg:py-[42px] px-0 lg:px-[104px] text-center content-center justify-center">
        <div className="xs:col-end-13">
          <div
            className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto bg-cover`}
            style={{ backgroundImage: `url(${ScanOutline})` }}
          >
            <div className="flex flex-col items-center">
              <div
                className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[250px] lg:w-[320px] aspect-square content-center justify-center`}
              >
                <OpenID4VPVerification
                  triggerElement={
                    <QrIcon
                      id="OpenID4VPVerification_trigger"
                      className="w-[78px] lg:w-[100px]"
                      aria-disabled={
                        presentationDefinition.input_descriptors.length === 0
                      }
                    />
                  }
                  verifyServiceUrl={window._env_.VERIFY_SERVICE_API_URL}
                  presentationDefinition={presentationDefinition}
                  onVPProcessed={handleOnVpProcessed}
                  onQrCodeExpired={handleOnQrExpired}
                  onError={handleOnError}
                  qrCodeStyles={{ size: qrSize }}
                  clientId={getClientId()}
                  isSameDeviceFlowEnabled={false}
                />
              </div>
              <Button
                id="request-credentials-button"
                title={t("rqstButton")}
                className={`w-[300px] mx-auto lg:ml-[76px] mt-10 lg:hidden`}
                variant="fill"
                onClick={handleRequestCredentials}
                disabled={activeScreen === 3}
              />
            </div>
          </div>
        </div>
      </div>
    );
  } else if (flowType === "sameDevice") {
    return (
      <div className="flex flex-col mt-10 lg:mt-0 pt-0 pb-[100px] lg:py-[42px] px-0 lg:px-[104px] text-center content-center justify-center">
        <div className="xs:col-end-13">
          <div
            className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto bg-cover`}
            style={{ backgroundImage: `url(${ScanOutline})` }}
          >
            <div className="flex flex-col items-center">
              <div
                className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[250px] lg:w-[320px] aspect-square content-center justify-center`}
              >
                <OpenID4VPVerification
                  triggerElement={
                    <QrIcon
                      id="OpenID4VPVerification_trigger"
                      className="w-[78px] lg:w-[100px]"
                      aria-disabled={
                        presentationDefinition.input_descriptors.length === 0
                      }
                    />
                  }
                  verifyServiceUrl={window._env_.VERIFY_SERVICE_API_URL}
                  presentationDefinition={presentationDefinition}
                  onVPProcessed={handleOnVpProcessed}
                  onQrCodeExpired={handleOnQrExpired}
                  onError={handleOnError}
                  clientId={getClientId()}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
};

export const VpVerification = () => {
  return <div>{DisplayActiveStep()}</div>;
};
