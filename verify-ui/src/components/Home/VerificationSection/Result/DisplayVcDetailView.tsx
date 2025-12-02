import React, { useEffect, useState } from "react";
import { saveData } from "../../../../utils/misc";
import {
  DocumentIcon,
  SharableLink,
  VectorDownload,
  VectorExpand,
} from "../../../../utils/theme-utils";
import ActionButton from "../commons/ActionButton";
import { useTranslation } from "react-i18next";
import { getDetailsOrder } from "../../../../utils/commonUtils";
import { isRTL } from "../../../../utils/i18n";
import { AnyVc } from "../../../../types/data-types";
import VcDetailsGrid from "./VcDetailsGrid";
import i18next from "i18next";
import { getTemplateUrl } from "../../../../utils/svg-template-utils";
import VcSvgTemplate from "./VcSvgTemplate";
import { useAppDispatch } from "../../../../redux/hooks";
import { raiseAlert } from "../../../../redux/features/alerts/alerts.slice";

function DisplayVcDetailView({
  vc,
  onExpand,
  className,
}: {
  vc: AnyVc;
  onExpand: any;
  className?: string;
}) {
  const { t, i18n } = useTranslation("Verify");
  const currentLang = i18next.language;
  const templateUrl = getTemplateUrl(vc);
  const orderedDetails = vc && getDetailsOrder(vc, currentLang);
  const isRtl = isRTL(i18n.language);
  const [templateError, setTemplateError] = useState(false);

  const positionLeft = `left-[250px] ${
    templateUrl ? "lg:left-[310px]" : "lg:left-[350px]"
  } ${templateUrl ? "lg:hover:left-[197px]" : "lg:hover:left-[237px]"}`;
  const positionRight = `right-[250px] ${
    templateUrl ? "lg:right-[310px]" : "lg:right-[350px]"
  } ${templateUrl ? "lg:hover:right-[197px]" : "lg:hover:right-[237px]"}`;
  const buttonPosition = isRtl ? positionRight : positionLeft;

  const showTemplate = templateUrl && !templateError;
  const dispatch = useAppDispatch();

  const onTemplateError = (error: Error) => {
    setTemplateError(true);
    dispatch(
      raiseAlert({ message: error.message, severity: "error", open: true })
    );
  };

  useEffect(() => {
    setTemplateError(false);
  }, [templateUrl, vc]);

  return (
    <div>
      <div
        className={`w-[339px] lg:w-[410px] m-auto rounded-lg ${
          showTemplate ? "" : "bg-white shadow-lg"
        } px-[15px] mb-4 ${className}`}
      >
        {vc ? (
          <div className="relative">
            {showTemplate ? (
              <VcSvgTemplate
                vc={vc}
                templateUrl={templateUrl!}
                onError={onTemplateError}
              />
            ) : (
              <VcDetailsGrid orderedDetails={orderedDetails} vc={vc} />
            )}

            <div className="absolute inset-x-0 bottom-0 flex justify-end lg:justify-start px-4">
              <ActionButton
                label={t("expand")}
                onClick={onExpand}
                icon={<VectorExpand />}
                positionClasses={`hidden lg:flex ${buttonPosition} bottom-[60px]`}
              />
              <ActionButton
                label={t("download")}
                onClick={() => saveData(vc)}
                icon={<VectorDownload />}
                positionClasses={`bottom-[30px] ${buttonPosition} lg:bottom-[10px]`}
              />
            </div>
            {!templateUrl && (
              <div className="flex gap-2 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-gray-700 mb-3 w-[240px] lg:w-[320px]">
                <SharableLink />
                <span className="text-verySmallTextSize">{t("IconToolTip")}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="grid content-center justify-center w-[100%] h-[320px] text-documentIcon">
            <DocumentIcon />
          </div>
        )}
      </div>
    </div>
  );
}

export default DisplayVcDetailView;
