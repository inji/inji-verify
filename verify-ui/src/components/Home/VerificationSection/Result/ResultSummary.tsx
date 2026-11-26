import React from "react";
import { VerificationSuccessIcon, VerificationFailedIcon } from "../../../../utils/theme-utils";
import { useTranslation } from "react-i18next";

const backgroundColorMapping: Record<string, string> = {
  SUCCESS: "bg-successText",
  EXPIRED: "bg-expiredText",
  INVALID: "bg-invalidText",
  REVOKED: "bg-revokedText",
  TIMEOUT: "bg-expiredText",
};

const ResultSummary = ({
  status,
}: {
  status?: "SUCCESS" | "EXPIRED" | "INVALID" | "TIMEOUT" | "REVOKED" | string | null;
}) => {
  if (!status) {
    return null;
  }

  const bgColor = backgroundColorMapping[status] || "bg-invalidText";
  const { t } = useTranslation("ResultSummary");
  return (
    <div
      className={`flex flex-col items-center justify-center h-[170px] lg:h-[186px] ${bgColor}`}
    >
      <div className={`block mb-2.5 text-white`}>
        {status === "SUCCESS" ? (
          <VerificationSuccessIcon id="success_message_icon" />
        ) : (
          <VerificationFailedIcon />
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
