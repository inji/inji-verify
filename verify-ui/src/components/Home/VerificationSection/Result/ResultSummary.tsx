import React from "react";
import {
  VerificationSuccessIcon,
  VerificationFailedIcon,
} from "../../../../utils/theme-utils";
import { useTranslation } from "react-i18next";

const backgroundColorMapping: any = {
  SUCCESS: "bg-successText",
  EXPIRED: "bg-expiredText",
  INVALID: "bg-invalidText",
  REVOKED: "bg-revokedText",
};

const ResultSummary = ({
  status,
}: {
  status: "SUCCESS" | "EXPIRED" | "INVALID" | "TIMEOUT" | "REVOKED";
}) => {
  const bgColor = backgroundColorMapping[status];
  const { t } = useTranslation("ResultSummary");

  return (
    <div
      data-testid="result-summary-container"
      className={`flex flex-col items-center justify-center h-[170px] lg:h-[186px] ${
        bgColor ?? ""
      }`}
    >
      <div className={`block mb-2.5 text-white`}>
        {status === "SUCCESS" ? (
          <VerificationSuccessIcon data-testid="success-icon" />
        ) : (
          <VerificationFailedIcon data-testid="failed-icon" />
        )}
      </div>

      <div className={`rounded-xl p-1`}>
        <p
          id="vc-result-display-message"
          className={`font-normal text-normalTextSize lg:text-lgNormalTextSize text-center text-white`}
        >
          {t(`${status}`)}
        </p>
      </div>
    </div>
  );
};

export default ResultSummary;
