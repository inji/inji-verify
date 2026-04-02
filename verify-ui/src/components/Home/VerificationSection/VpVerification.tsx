import { useEffect } from "react";
import { QrIcon } from "../../../utils/theme-utils";
import { useVerifyFlowSelector } from "../../../redux/features/verification/verification.selector";
import Loader from "../../commons/Loader";
import VpSubmissionResult from "./Result/VpSubmissionResult";
import { useAppDispatch } from "../../../redux/hooks";
import {
  resetVpRequest,
  setSelectCredential,
  showMissingCredentialOptions,
  verificationSubmissionComplete,
  OVP_SESSION_SELECTED_CREDENTIALS_KEY,
} from "../../../redux/features/verify/vpVerificationState";
import { VCShareType, VpSubmissionResultInt } from "../../../types/data-types";
import { closeAlert, raiseAlert } from "../../../redux/features/alerts/alerts.slice";
import { AlertMessages } from "../../../utils/config";
import { OpenID4VPVerification } from "@injistack/react-inji-verify-sdk";
import { Button } from "./commons/Button";
import { useTranslation } from "react-i18next";
import {VerificationResults} from "@injistack/react-inji-verify-sdk/dist/components/openid4vp-verification/OpenID4VPVerification.types";
import {decodeSdJwtToken} from "../../../utils/decodeSdJwt";
import {vpVerificationV2Request} from "../../../utils/commonUtils";

const DisplayActiveStep = () => {
  const { t } = useTranslation("Verify");
  const isLoading = useVerifyFlowSelector((state) => state.isLoading);
  const sharingType = useVerifyFlowSelector((state) => state.sharingType);
  const isSingleVc = sharingType === VCShareType.SINGLE;
  const selectedCredentials = useVerifyFlowSelector((state) => state.selectedCredentials);
  const originalSelectedCredentials = useVerifyFlowSelector((state) => state.originalSelectedCredentials);
  const verifiedVcs: VpSubmissionResultInt[] = useVerifyFlowSelector((state) => state.verificationSubmissionResult );
  const unverifiedCredentials = useVerifyFlowSelector((state) => state.unVerifiedCredentials );
  const presentationDefinition = useVerifyFlowSelector((state) => state.presentationDefinition );
  const qrSize = window.innerWidth <= 1024 ? 240 : 320;
  const activeScreen = useVerifyFlowSelector((state) => state.activeScreen);
  const showResult = useVerifyFlowSelector((state) => state.isShowResult);
  const flowType = useVerifyFlowSelector((state) => state.flowType);
  const openSelectWallet = useVerifyFlowSelector((state) => state.SelectWalletPanel);
  const selectedWalletBaseUrl = useVerifyFlowSelector((state) => state.selectedWalletBaseUrl);
  // Only show "wrong credential" error when on the result screen. When the user has
  // clicked "Request Missing Credential", selectedCredentials becomes unVerifiedCredentials (1 item),
  // which would otherwise trigger this; we must not show the error in that flow.
  const incorrectCredentialShared =
    selectedCredentials.length === 1 && unverifiedCredentials.length === 1 && isSingleVc && showResult;
  const sdkInstanceKey = useVerifyFlowSelector((state) => state.sdkInstanceKey);
  
  const dispatch = useAppDispatch();

  const handleRequestCredentials = () => {
    dispatch(setSelectCredential());
  };

  const handleMissingCredentials = () => {
    dispatch(showMissingCredentialOptions());
  };

  const handleRestartProcess = () => {
    dispatch(resetVpRequest());
  };

    const handleOnVpProcessed = async (vpResults: VerificationResults) => {
        try {
            const processedResults = await Promise.all(
                vpResults.flatMap(async (vpResult) => {
                    const response = vpResult.verificationResponse;
                    if ("vcResults" in response && Array.isArray(response.vcResults)) {

                        return Promise.all(
                            response.vcResults.map(async (item, i) => {
                                let vc = item.vc;
                                if (typeof vc === "string") {
                                    vc = await decodeSdJwtToken(vc);
                                }

                                return {vc, vcStatus: item.vcStatus,
                                };
                            })
                        );
                    }
                    throw new Error("Expected summarised VP response with vcResults");
                })
            );

            const flattenedResults = processedResults.flat();
            localStorage.removeItem(OVP_SESSION_SELECTED_CREDENTIALS_KEY);
            dispatch(verificationSubmissionComplete({verificationResult: flattenedResults,
                })
            );
        } catch (error: any) {
            handleOnError(error);
        }
    };

  const handleOnQrExpired = () => {
    dispatch(raiseAlert({ ...AlertMessages().sessionExpired, open: true }));
    dispatch(resetVpRequest());
  };

  const handleOnError = (error: any) => {
    dispatch(closeAlert({}));
    dispatch(resetVpRequest());
    if (error.errorCode) {
      error.message = "We’re unable to complete your request. Please contact support for assistance.";
    }
    dispatch(raiseAlert({ title: "Request Failed", errorCode:error.errorCode, errorReason: error.errorMessage, message: error.message, referenceId: error.transactionId, severity: "error", open: true, autoHideDuration: 120000 }));
  };

  const getClientId = () => {
    return (isSingleVc && selectedCredentials[0]?.clientIdScheme === "pre_registered") ? window._env_.CLIENT_ID : window._env_.CLIENT_ID_DID;
  }

  useEffect(() => {

    const searchParams = new URLSearchParams(window.location.search);
    if (searchParams.has("response_code")) return;

    // Auto-trigger SDK only when we're on the ScanQrCode step and NOT in the
    // wallet selection panel. This avoids firing when the user is choosing a wallet.
    if (selectedCredentials.length > 0 && activeScreen === 3 && !openSelectWallet) {
      setTimeout(() => {
        const triggerElement = document.getElementById("OpenID4VPVerification_trigger");
        if (triggerElement) {
          const event = new MouseEvent("click", { bubbles: true, cancelable: true });
          triggerElement.dispatchEvent(event);
        }
      }, 100); // Delay to ensure the DOM is updated
    }
  }, [selectedCredentials, activeScreen, openSelectWallet]);

  useEffect(() => {
    if (originalSelectedCredentials.length === 0) {
      localStorage.removeItem(OVP_SESSION_SELECTED_CREDENTIALS_KEY);
      return;
    }

    if (activeScreen === 3 || unverifiedCredentials.length > 0) {
      localStorage.setItem(
        OVP_SESSION_SELECTED_CREDENTIALS_KEY,
        JSON.stringify(originalSelectedCredentials)
      );
    }
  }, [activeScreen, originalSelectedCredentials, unverifiedCredentials]);

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
          unverifiedCredentials={unverifiedCredentials}
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
            className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto`}
          >
            <div className="flex flex-col items-center">
              <div
                className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[300px] lg:w-[350px] aspect-square content-center justify-center`}
              >
                <OpenID4VPVerification
                  key={`${flowType}-${sdkInstanceKey}`}
                  triggerElement={ <QrIcon id="OpenID4VPVerification_trigger" className="w-[78px] lg:w-[100px]" aria-disabled={presentationDefinition.input_descriptors.length === 0 } /> }
                  verifyServiceUrl={window.location.origin + window._env_.VERIFY_SERVICE_API_URL}
                  presentationDefinition={presentationDefinition}
                  onVPProcessed={handleOnVpProcessed}
                  onQrCodeExpired={handleOnQrExpired}
                  onError={handleOnError}
                  qrCodeStyles={{ size: qrSize }}
                  clientId={getClientId()}
                  isSameDeviceFlowEnabled={false}
                  vpVerificationV2Request={vpVerificationV2Request}
                />
              </div>
              <Button	
                id="request-credentials-button"	
                title={t("rqstButton")}	
                className={`w-[300px] mx-auto lg:ml-[76px] mt-10 lg:hidden`}	
                variant="fill"	
                onClick={handleRequestCredentials}	
                disabled={activeScreen === 3 }	
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
            className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto`}
          >
            <div className="flex flex-col items-center">
              <div
                className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[300px] lg:w-[350px] aspect-square content-center justify-center`}
              >
                <OpenID4VPVerification
                  key={`${flowType}-${sdkInstanceKey}`}
                  triggerElement={ <QrIcon id="OpenID4VPVerification_trigger" className="w-[78px] lg:w-[100px]" aria-disabled={presentationDefinition.input_descriptors.length === 0 } /> }
                  verifyServiceUrl={window.location.origin + window._env_.VERIFY_SERVICE_API_URL}
                  presentationDefinition={presentationDefinition}
                  onVPProcessed={handleOnVpProcessed}
                  onQrCodeExpired={handleOnQrExpired}
                  onError={handleOnError}
                  clientId={getClientId()}
                  webWalletBaseUrl={selectedWalletBaseUrl}
                  vpVerificationV2Request={vpVerificationV2Request}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  } else {
    return <></>;
  }
};

export const VpVerification = () => {
  return (
    <div>
      <DisplayActiveStep />
    </div>
  );
};
