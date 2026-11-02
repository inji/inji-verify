import React from "react";
import {
  convertToId,
  convertToTitleCase,
  getDisplayValue,
  saveData,
} from "../../../../utils/misc";
import {
  DocumentIcon,
  VectorDownload,
  VectorExpand,
  VectorOutline,
} from "../../../../utils/theme-utils";

function VcDisplayCard({ vc, onExpand }: Readonly<{ vc: any; onExpand: any }>) {
  const desiredOrder = [
    "fullName",
    "gender",
    "dob",
    "benefits",
    "policyName",
    "policyNumber",
    "policyIssuedOn",
    "policyExpiresOn",
    "mobile",
    "email",
  ];
  type Detail = {
    key: string;
    value: string;
  };

    const orderedDetails: Detail[] = desiredOrder.map((field) => {
        const key = Object.keys(vc.credentialSubject || {}).find((k) => k === field);
        const rawValue = key ? vc.credentialSubject[key] : undefined;
        const value =
            rawValue !== undefined && rawValue !== null
                ? getDisplayValue(rawValue)
                : "N/A";

        return { key: field, value };
    });

    return (
    <div>
      <div
        className={`w-[410px] m-auto rounded-lg bg-white px-[15px] shadow-lg mb-4`}
      >
        {vc.credentialSubject ? (
          <div className="grid">
            {orderedDetails.map((label, index) => (
              <div
                className={`py-2.5 px-1 xs:col-end-13 ${
                  index % 2 === 0
                    ? "lg:col-start-1 lg:col-end-6"
                    : "lg:col-start-8 lg:col-end-13"
                }`}
                key={label.key}
              >
                <p
                  id={convertToId(label.key)}
                  className="font-normal text-verySmallTextSize break-all text-[#666666]"
                >
                  {convertToTitleCase(label.key)}
                </p>
                <p
                  id={`${convertToId(label.key)}-value`}
                  className="font-bold text-smallTextSize break-all"
                >
                  {getDisplayValue(label.value)}
                </p>
              </div>
            ))}
            <div
              className="flex items-center justify-center relative left-[331px] bottom-[100px] w-[40px] h-[40px] aspect-square bg-cover opacity-25"
              style={{ backgroundImage: `url(${VectorOutline})` }}
            >
              <VectorExpand onClick={onExpand} />
            </div>
            <div
              className="flex items-center justify-center relative left-[290px] bottom-[40px] w-[40px] h-[40px] aspect-square bg-cover opacity-25"
              style={{ backgroundImage: `url(${VectorOutline})` }}
            >
              <VectorDownload onClick={() => saveData(vc)} />
            </div>
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

export default VcDisplayCard;
