
import { useEffect, useState } from "react";
import CameraAccessDenied from "./CameraAccessDenied";
import { useAppDispatch } from "../../../redux/hooks";
import {
  goToHomeScreen,
  verificationComplete,
} from "../../../redux/features/verification/verification.slice";
import { raiseAlert } from "../../../redux/features/alerts/alerts.slice";
import { QRCodeVerification } from "@mosip/react-inji-verify-sdk";
import { DisplayTimeout } from "../../../utils/config";

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

  const scheduleVcDisplayTimeOut = () => {
    setTimeout(() => {
      dispatch(goToHomeScreen({}));
    }, DisplayTimeout)
  };

  const handleOnVCProcessed = (data: {
    vc: unknown;
    vcStatus: string
  }[]) => {
    dispatch(verificationComplete({verificationResult: data[0]}));
    scheduleVcDisplayTimeOut();
  }

  function getClientId() {
    return window._env_.CLIENT_ID;
  }

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
          verifyServiceUrl={window._env_.VERIFY_SERVICE_API_URL}
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
