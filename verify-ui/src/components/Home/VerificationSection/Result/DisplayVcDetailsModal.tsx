import React from "react";
import { VectorCollapse, VectorDownload } from "../../../../utils/theme-utils";
import { convertToTitleCase, saveData } from "../../../../utils/misc";
import ActionButton from "../commons/ActionButton";
import { useTranslation } from "react-i18next";
import { getDetailsOrder } from "../../../../utils/commonUtils";
import { AnyVc, VcStatus } from "../../../../types/data-types";
import {
  backgroundColorMapping,
  borderColorMapping,
  textColorMapping,
} from "../../../../utils/config";
import VcDetailsGrid from "./VcDetailsGrid";
import i18next from "i18next";
import VcSvgTemplate from "./VcSvgTemplate";
import { getTemplateUrl } from "../../../../utils/svg-template-utils";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  vc: AnyVc;
  status: VcStatus;
  vcType: string;
  logo?: { url: any; alt: string };
}

const DisplayVcDetailsModal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  vc,
  status,
  vcType,
  logo,
}) => {
  const { t } = useTranslation("Verify");
  const currentLang = i18next.language;
  const orderedDetails = vc && getDetailsOrder(vc, currentLang);
  const templateUrl = getTemplateUrl(vc);

  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
      <div className="bg-white rounded-xl w-full max-w-2xl p-6 shadow-lg h-[90vh] overflow-y-auto custom-scrollbar">
        <div className="flex justify-between items-center mb-4">
          <div className="flex items-center space-x-2">
            {logo && (
              <img
                src={logo.url}
                alt={logo.alt}
                className="w-[50px] h-[50px] mr-1"
              />
            )}

            <div>
              <h2 className="text-lg font-bold">
                {convertToTitleCase(vcType)}
              </h2>
              <div
                className={`rounded-xl max-w-[120px] ${backgroundColorMapping[status]} border ${borderColorMapping[status]} p-1`}
              >
                <p
                  className={`font-normal text-smallTextSize text-center ${textColorMapping[status]}`}
                >
                  {t(status)}
                </p>
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700"
          >
            ✕
          </button>
        </div>

        <div className="space-y-4">
          {templateUrl ? (
            <VcSvgTemplate vc={vc} templateUrl={templateUrl} />
          ) : (
            <VcDetailsGrid orderedDetails={orderedDetails} vc={vc} />
          )}
        </div>

        <div className="flex justify-end space-x-4 mt-6">
          <div className="relative">
            <ActionButton
              label={t("collapse")}
              onClick={onClose}
              icon={<VectorCollapse />}
              positionClasses="right-[10px] lg:right-[5px] lg:hover:right-[5px] bottom-[70px]"
            />
            <ActionButton
              label={t("download")}
              onClick={() => saveData(vc)}
              icon={<VectorDownload />}
              positionClasses="right-[10px] lg:right-[5px] lg:hover:right-[5px] bottom-[20px]"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default DisplayVcDetailsModal;
