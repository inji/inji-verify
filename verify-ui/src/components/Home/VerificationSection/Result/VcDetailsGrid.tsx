import React from "react";
import { convertToId, convertToTitleCase, getDisplayValue } from "../../../../utils/misc";
import { SharableLink } from "../../../../utils/theme-utils";
import { AnyVc } from "../../../../types/data-types";

interface VcDetailsGridProps {
  orderedDetails: { key: string; value: any }[];
  vc?: AnyVc;
}

const VcDetailsGrid: React.FC<VcDetailsGridProps> = ({
  orderedDetails,
  vc ,
}) => {
  const BIOMETRIC_KEYS  = ["face", "portrait", "signature_usual_mark"];

  const biometricItems = orderedDetails.filter((item) =>
    BIOMETRIC_KEYS.includes(item.key)
  );
  const otherItems = orderedDetails.filter(
    (item) => !BIOMETRIC_KEYS.includes(item.key)
  );

  const renderingItems = [...biometricItems, ...otherItems];

  return (
    <div className="grid relative lg:grid-cols-12 lg:gap-y-4">
      {renderingItems.map((label, index) => {
        const isImage = BIOMETRIC_KEYS.includes(label.key);
        const isEven = index % 2 === 0;
        const normalizeKey = (key: string) => key.toLowerCase().trim();
        const hasDisclosedClaims =
          vc &&
          typeof vc === "object" &&
          vc !== null &&
          "disclosedClaims" in vc &&
          vc.disclosedClaims;

        const isDisclosed = hasDisclosedClaims
          ? Object.keys(vc.disclosedClaims).some(
              (key) => normalizeKey(key) === normalizeKey(label.key),
            )
          : false;

        return (
          <div
            key={label.key}
            className={`py-2.5 px-1 xs:col-end-13 ${
              isEven
                ? "lg:col-start-1 lg:col-end-6"
                : "lg:col-start-8 lg:col-end-13"
            }`}
          >
            {isImage ? (
              <img
                src={label.value}
                alt={label.key}
                style={{
                  width: 80,
                  height: 80,
                  borderRadius: 10,
                  marginTop: 10,
                }}
              />
            ) : (
              <>
                <p
                  id={convertToId(label.key)}
                  className="font-normal text-verySmallTextSize break-all text-[#666666] flex items-center gap-1"
                >
                  {convertToTitleCase(label.key)}
                  {isDisclosed && <SharableLink />}
                </p>
                <p
                  id={`${convertToId(label.key)}-value`}
                  className="font-bold text-smallTextSize break-all"
                >
                  {getDisplayValue(label.value)}
                </p>
              </>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default VcDetailsGrid;
