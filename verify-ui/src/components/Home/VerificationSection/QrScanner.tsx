import { useEffect, useState } from "react";
import CameraAccessDenied from "./CameraAccessDenied";
import { useAppDispatch } from "../../../redux/hooks";
import {
  goToHomeScreen,
  verificationComplete
} from "../../../redux/features/verification/verification.slice";
import { raiseAlert } from "../../../redux/features/alerts/alerts.slice";
import { QRCodeVerification } from "@injistack/react-inji-verify-sdk";
import {evaluateVpStatus, getClientId, isVPSubmissionSupported, vcVerificationV2Request} from "../../../utils/commonUtils";

function QrScanner({ onClose, scannerActive }: {
  onClose: () => void;
  scannerActive: boolean;
}) {
  const dispatch = useAppDispatch();
  const [isCameraBlocked, setIsCameraBlocked] = useState(false);
  const [isScanning, setIsScanning] = useState(false);

  useEffect(() => {
    setIsScanning(true);
  }, []);

  const handleOnVCProcessed = (vcResults: any[]) => {
    try {
      const processedResults = vcResults.map((vcResult) => {
        const vc = vcResult.vc;
        const verificationResponse = vcResult.verificationResponse;
        const vcStatus = evaluateVpStatus(verificationResponse);
        return { vc, vcStatus, verificationResponse };
      });
      const selectedResult = processedResults[0];
      if (!selectedResult) {
        dispatch(
          raiseAlert({
            message: "No verification result to display.",
            severity: "error",
          }),
        );
        dispatch(goToHomeScreen({}));
        return;
      }
      dispatch(
        verificationComplete({ verificationResult: selectedResult }),
      );
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : "Verification failed.";
      dispatch(
        raiseAlert({
          message,
          severity: "error",
        }),
      );
      dispatch(goToHomeScreen({}));
    }
  };

  return (
    <div className="fixed inset-0 z-[100000] flex items-center justify-center bg-black lg:relative lg:inset-auto lg:w-[21rem] lg:h-auto lg:aspect-square lg:bg-transparent">
      {!isCameraBlocked && (
        <div
          id="scanning-line"
          className={`hidden lg:${
            isScanning ? "block" : "hidden"
          } scanning-line absolute z-10`}
        />
      )}

      <div className="w-full h-full lg:h-auto lg:w-full flex items-center justify-center rounded-lg overflow-hidden">
        <QRCodeVerification
          scannerActive={scannerActive}
          verifyServiceUrl={window.location.origin + window._env_.VERIFY_SERVICE_API_URL}
          isEnableUpload={false}
          onVCProcessed={handleOnVCProcessed}
          onClose={onClose}
          onError={(error) => {
            if (error.name === "NotAllowedError") {
              setIsCameraBlocked(true);
            } else {
              dispatch(goToHomeScreen({}));
              dispatch(
                raiseAlert({ message: error.message, severity: "error" })
              );
            }
          }}
          clientId={getClientId()}
          isVPSubmissionSupported={isVPSubmissionSupported()}
          vcVerificationV2Request ={vcVerificationV2Request}
        />
      </div>

      {isCameraBlocked && (
        <CameraAccessDenied
          open={isCameraBlocked}
          handleClose={() => {
            dispatch(goToHomeScreen({}));
            setIsCameraBlocked(false);
          }}
        />
      )}
    </div>
  );
}

export default QrScanner;