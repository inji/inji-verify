import { useEffect, useState } from "react";
import CameraAccessDenied from "./CameraAccessDenied";
import { useAppDispatch } from "../../../redux/hooks";
import {
  goToHomeScreen,
  verificationComplete
} from "../../../redux/features/verification/verification.slice";
import { raiseAlert } from "../../../redux/features/alerts/alerts.slice";
import { QRCodeVerification } from "@injistack/react-inji-verify-sdk";
import { getClientId, isVPSubmissionSupported, vcVerificationV2Request} from "../../../utils/commonUtils";

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

const handleOnVCProcessed = (data: any[]) => {
        const vc = data[0].vc;
        const verificationResponse = data[0].verificationResponse;
    const vcStatus = verificationResponse.verificationStatus ??
        verificationResponse.vcResults?.[0]?.vcStatus ??
        verificationResponse.vpResultStatus;

        dispatch(verificationComplete({verificationResult: {
                    vc,
                    vcStatus,
                    verificationResponse
                }
            })
        );
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