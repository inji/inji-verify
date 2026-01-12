import React from "react";
import VerificationMethodTabs from "./VerificationMethodTabs";
import { useTranslation } from "react-i18next";

function Header(props: any) {
  const { t } = useTranslation("Header");
  return (
    <div>
      <div
        data-testid="header-container"
        className={`w-full bg-${window._env_.DEFAULT_THEME}-lighter-gradient py-[26px] lg:py-9 max-w-[100vw] text-center`}
      >
        <p
          id="verify-credentials-heading"
          className="mx-auto my-1.5 font-bold text-lgMediumTextSize lg:text-lgBoldTextSize max-w-[80vw] text-headerLabelText"
        >
          {t("heading")}
          <span
            id="heading-highlighted-content"
            className={`bg-${window._env_.DEFAULT_THEME}-gradient bg-clip-text text-transparent lg:text-lgBoldTextSize font-bold`}
          >
            {t("headingHighlight")}
          </span>
        </p>
        <p
          id="verify-credentials-description"
          className="mx-auto my-1.5 text-headerDescriptionText text-normalTextSize lg:text-lgNormalTextSize font-normal px-[22px] lg:max-w-[624px]"
        >
          {t("description")}
        </p>
      </div>
      <VerificationMethodTabs />
    </div>
  );
}

export default Header;
