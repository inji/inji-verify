import React from "react";
import { useTranslation } from "react-i18next";
import injiLogo from "../../assets/truckpassTheme/inji-verify.svg";

function Copyrights(props: any) {
  const { t } = useTranslation("CopyRight");

  return (
    <div className="fixed grid bottom-0 w-[100vw] lg:w-[49vw] content-center justify-center bg-white">
      <div className="xs:w-[90vw] lg:w-[40vw] mx-auto border-b-[1px] border-b-copyRightsBorder opacity-20" />

      <div className="py-4 px-0 w-[100%] flex items-center justify-center gap-2 text-normalTextSize font-normal text-copyRightsText">
        <img
          src={injiLogo}
          alt="Inji Logo"
          className="h-4 w-auto opacity-90"
        />

        <p
          id="copyrights-content"
          className="text-center"
          dangerouslySetInnerHTML={{ __html: t("content") }}
        />
      </div>
    </div>
  );
}

export default Copyrights;
