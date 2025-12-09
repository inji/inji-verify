import { QrIcon } from "../utils/theme-utils";
import { UploadQrCode } from "../components/Home/VerificationSection/UploadQrCode";
import { ScanOutline } from "../utils/theme-utils";
import { useTranslation } from "react-i18next";
import {
  goToHomeScreen,
  verificationComplete,
} from "../redux/features/verification/verification.slice";
import { raiseAlert } from "../redux/features/alerts/alerts.slice";
import { useAppDispatch } from "../redux/hooks";
import { QRCodeVerification } from "@mosip/react-inji-verify-sdk";
import { getClientId, isVPSubmissionSupported } from "../utils/commonUtils";

export const Upload = () => {
  const { t } = useTranslation("Upload");
  const dispatch = useAppDispatch();

  const triggerElement = (
    <div>
      <div
        className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[250px] lg:w-[320px] aspect-square content-center justify-center`}
      ></div>
      <div className="absolute top-[58px] left-[98px] lg:top-[165px] lg:left-[50%] lg:translate-x-[-50%] lg:translate-y-[-50%]">
        <QrIcon className="w-[78px] lg:w-[100px]" />
      </div>
      <UploadQrCode
        className="absolute top-[130px] left-[45px] lg:top-[200px] lg:left-[95px]"
        displayMessage={t("Common:Button.upload")}
      />
    </div>
  );

  const handleOnVCProcessed = (data: {
    vc: unknown;
    vcStatus: string
  }[]) => {
    dispatch(verificationComplete({verificationResult: data[0]}));
  }

  return (
    <div className="flex flex-col pt-0 pb-[100px] lg:py-[42px] px-0 lg:px-[104px] text-center content-center justify-center">
      <div className="xs:col-end-13">
        <div
          className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto bg-cover`}
          style={{ backgroundImage: `url(${ScanOutline})` }}
        >
          <QRCodeVerification
            triggerElement={triggerElement}
            verifyServiceUrl={window.location.origin + window._env_.VERIFY_SERVICE_API_URL}
            isEnableScan={false}
            onVCProcessed={handleOnVCProcessed}
            uploadButtonId={"upload-qr"}
            uploadButtonStyle="hidden"
            onError={(error) => {
              dispatch(
                raiseAlert({
                  message: error.message,
                  severity: "error",
                  open: true,
                })
              );
              dispatch(goToHomeScreen({}));
            }}
            clientId={getClientId()}
            isVPSubmissionSupported={isVPSubmissionSupported()}
          />
        </div>
        <div className="grid text-center content-center justify-center pt-2">
          <p
            id="file-format-constraints"
            className="font-normal text-normalTextSize text-uploadDescription w-[280px]"
          >
            {t("format")}
          </p>
        </div>
      </div>
    </div>
  );
};
