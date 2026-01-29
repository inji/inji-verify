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
  vc
}) => {
  const BIOMETRIC_KEYS = ["face", "portrait", "signature_usual_mark"].map((key) => key.toLowerCase());

  const biometricItems = orderedDetails.filter((item) =>
    BIOMETRIC_KEYS.includes(item.key.toLocaleLowerCase())
  );
  const otherItems = orderedDetails.filter(
    (item) => !BIOMETRIC_KEYS.includes(item.key.toLocaleLowerCase())
  );

  const renderingItems = [...biometricItems, ...otherItems];

  return (
    <div className="grid relative lg:grid-cols-12 lg:gap-y-4">
      {renderingItems.map((label, index) => {
        const isImage = BIOMETRIC_KEYS.includes(label.key.toLocaleLowerCase());
        const isEven = index % 2 === 0;
        const normalizeKey = (key: string) => key.toLowerCase().trim();
        const disclosedClaims =
          vc &&
          typeof vc === "object" &&
          vc !== null &&
          "disclosedClaims" in vc
            ? (vc as { disclosedClaims?: unknown }).disclosedClaims
            : undefined;
        const hasDisclosedClaims =
          disclosedClaims &&
          typeof disclosedClaims === "object" &&
          !Array.isArray(disclosedClaims);

        const isDisclosed = hasDisclosedClaims
          ? Object.keys(disclosedClaims as Record<string, unknown>).some(
              (key) => normalizeKey(key) === normalizeKey(label.key),
            )
          : false;

          const faceData = Array.isArray(label.value) && label.value.length > 0 ? label.value[0] : label.value;

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
                src={faceData}
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
