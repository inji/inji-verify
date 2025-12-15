// src/components/Home/Header/index.tsx
import React, { useMemo } from "react";
import VerificationMethodTabs from "./VerificationMethodTabs";
import { useTranslation } from "react-i18next";

function Header(props: any) {
  // using the "Header" namespace (your en.json structure)
  const { t } = useTranslation("Header");

  // Determine active theme (fall back to class name approach if env var missing)
  const activeTheme = (window as any)._env_?.DEFAULT_THEME
    || document.documentElement.className.split(/\s+/).find((c: string) => c?.endsWith("_theme"))
    || "default_theme";

  const isTruckpass = useMemo(() => activeTheme === "truckpass_theme", [activeTheme]);

  // choose correct i18n keys
  const heading = isTruckpass ? t("heading_truckpass") : t("heading");
  const headingHighlight = isTruckpass ? "" : t("headingHighlight");
  const description = isTruckpass ? t("description_truckpass") : t("description");

  // style override for truckpass description color
  const descriptionStyle: React.CSSProperties = isTruckpass ? { color: "#2D7ACF" } : {};

  return (
    <div>
      <div className={`w-full bg-${window._env_.DEFAULT_THEME}-lighter-gradient py-[26px] lg:py-9 max-w-[100vw] text-center`}>
        <p
          id="verify-credentials-heading"
          className="mx-auto my-1.5 font-bold text-lgMediumTextSize lg:text-lgBoldTextSize max-w-[80vw] text-headerLabelText"
        >
          {heading}
          {/* Only render the highlighted span for non-truckpass themes */}
          {!isTruckpass && (
            <span
              id="heading-highlighted-content"
              className={`bg-${window._env_.DEFAULT_THEME}-gradient bg-clip-text text-transparent lg:text-lgBoldTextSize font-bold`}
            >
              {headingHighlight}
            </span>
          )}
        </p>

        <p
          id="verify-credentials-description"
          className="mx-auto my-1.5 text-headerDescriptionText text-normalTextSize lg:text-lgNormalTextSize font-normal px-[22px] lg:max-w-[624px]"
          style={descriptionStyle}
        >
          {description}
        </p>
      </div>

      <VerificationMethodTabs />
    </div>
  );
}

export default Header;
