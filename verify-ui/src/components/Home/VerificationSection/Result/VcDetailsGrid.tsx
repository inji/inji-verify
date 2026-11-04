import React from "react";
import { convertToId, convertToTitleCase, getDisplayValue } from "../../../../utils/misc";
import { SharableLink } from "../../../../utils/theme-utils";
import { AnyVc, MdocVc } from "../../../../types/data-types";

interface VcDetailsGridProps {
  /** `label`, when set, is shown instead of the title-cased `key`. */
  orderedDetails: { key: string; value: any; label?: string }[];
  vc?: AnyVc;
}

/** "family_name" → "Family Name". */
const humanizeElementIdentifier = (identifier: string): string =>
  identifier
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");

/**
 * mdoc claims (namespace → element identifier → value) as rows. The key stays
 * `<namespace>.<element>` so rows from different namespaces stay distinct; the label
 * is the element name, qualified by its namespace only when that name repeats.
 */
export const getMdocDetailsOrder = (
  claims: MdocVc,
): { key: string; value: unknown; label: string }[] => {
  const rows = Object.entries(claims).flatMap(([namespace, namespaceClaims]) => {
    if (
      typeof namespaceClaims !== "object" ||
      namespaceClaims === null ||
      Array.isArray(namespaceClaims)
    ) {
      return [];
    }
    return Object.entries(namespaceClaims).map(([element, value]) => ({ namespace, element, value }));
  });
  const elementCounts = new Map<string, number>();
  rows.forEach(({ element }) => elementCounts.set(element, (elementCounts.get(element) ?? 0) + 1));
  return rows.map(({ namespace, element, value }) => ({
    key: `${namespace}.${element}`,
    value,
    label:
      (elementCounts.get(element) ?? 0) > 1
        ? `${humanizeElementIdentifier(element)} (${namespace})`
        : humanizeElementIdentifier(element),
  }));
};

/** Puts the document title first; it is display data from the caller, not a claim. */
export const withMdocDocTitle = <T extends { key: string; value: unknown }>(
  details: T[],
  docTitle?: string,
): (T | { key: string; value: unknown })[] =>
  docTitle ? [{ key: "docTitle", value: docTitle }, ...details] : details;

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
                alt={label.label ?? label.key}
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
                  {label.label ?? convertToTitleCase(label.key)}
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
