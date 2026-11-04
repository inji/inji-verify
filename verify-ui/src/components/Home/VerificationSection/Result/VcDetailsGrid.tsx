import React from "react";
import { convertToId, convertToTitleCase, getDisplayValue } from "../../../../utils/misc";
import { SharableLink } from "../../../../utils/theme-utils";
import { AnyVc, MdocVc } from "../../../../types/data-types";

interface VcDetailsGridProps {
  orderedDetails: { key: string; value: any }[];
  vc?: AnyVc;
}

export const getMdocDetailsOrder = (
  claims: MdocVc,
): { key: string; value: unknown }[] => {
  const details = Object.entries(claims).flatMap(([namespace, namespaceClaims]) => {
    if (
      typeof namespaceClaims !== "object" ||
      namespaceClaims === null ||
      Array.isArray(namespaceClaims)
    ) {
      return [];
    }
    return Object.entries(namespaceClaims).map(([key, value]) => ({
      key: `${namespace}.${key}`,
      value,
    }));
  });
  const docType = (claims as { docType?: unknown }).docType;
  return typeof docType === "string"
    ? [{ key: "docType", value: docType }, ...details]
    : details;
};

const isPlainObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const ISO_MDL_NAMESPACE = "org.iso.18013.5.1";

/**
 * mdoc result claims: one or more namespace objects keyed by element identifier.
 * The UI adds a string docType; backend results carry namespaces only, so the
 * ISO mDL namespace is also accepted as a marker. Every other entry must be a
 * namespace object, which keeps flat LDP / SD-JWT claims out.
 */
export const hasMdocNamespace = (
  claims: unknown,
): claims is MdocVc => {
  if (
    !isPlainObject(claims) ||
    (typeof claims.docType !== "string" &&
      !Object.prototype.hasOwnProperty.call(claims, ISO_MDL_NAMESPACE))
  ) {
    return false;
  }
  const namespaces = Object.entries(claims).filter(([key]) => key !== "docType");
  return namespaces.length > 0 && namespaces.every(([, value]) => isPlainObject(value));
};

const VcDetailsGrid: React.FC<VcDetailsGridProps> = ({
  orderedDetails,
  vc
}) => {
  const BIOMETRIC_KEYS = ["face", "portrait", "signature_usual_mark"].map((key) => key.toLowerCase());
  const detailKey = (key: string) => key.split(".").pop()?.toLocaleLowerCase() ?? key.toLocaleLowerCase();

  const biometricItems = orderedDetails.filter((item) =>
    BIOMETRIC_KEYS.includes(detailKey(item.key))
  );
  const otherItems = orderedDetails.filter(
    (item) => !BIOMETRIC_KEYS.includes(detailKey(item.key))
  );

  const renderingItems = [...biometricItems, ...otherItems];

  return (
    <div className="grid relative lg:grid-cols-12 lg:gap-y-4">
      {renderingItems.map((label, index) => {
        const isImage = BIOMETRIC_KEYS.includes(detailKey(label.key));
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
          const imageSrc =
            typeof faceData === "string" && /^data:image\//i.test(faceData)
              ? faceData
              : null;

        return (
          <div
            key={label.key}
            className={`py-2.5 px-1 xs:col-end-13 ${
              isEven
                ? "lg:col-start-1 lg:col-end-6"
                : "lg:col-start-8 lg:col-end-13"
            }`}
          >
            {isImage && imageSrc ? (
              <img
                src={imageSrc}
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
