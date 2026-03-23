import { useEffect, useState } from "react";
import { convertToId, convertToTitleCase } from "../../../../utils/misc";
import { VectorDown, VectorUp } from "../../../../utils/theme-utils";
import { VpSubmissionResultInt } from "../../../../types/data-types";
import DisplayVcDetailsModal from "./DisplayVcDetailsModal";
import DisplayVcDetailView from "./DisplayVcDetailView";
import { useTranslation } from "react-i18next";
import { decodeSdJwtToken } from "../../../../utils/decodeSdJwt";
import {getCredentialType} from "../../../../utils/commonUtils";
import { backgroundColorMapping, borderColorMapping, textColorMapping } from "../../../../utils/config";

function DisplayVcCardView(ViewVc: VpSubmissionResultInt) {
  const { vc, vcStatus, view } = ViewVc;
  const [showDetailView, setShowDetailView] = useState(view);
  const [isModalOpen, setModalOpen] = useState(false);
  const { t } = useTranslation("Verify");
  const [claims, setClaims] = useState<any>();
  const [credentialType, setCredentialType] = useState<string>("");

  useEffect(() => {
    const fetchDecodedClaims = async () => {
      if (typeof vc === "string") {
        const claims = await decodeSdJwtToken(vc);
        setClaims(claims);
        setCredentialType(claims.regularClaims.vct);
      } else {
        setClaims(vc);
        setCredentialType(getCredentialType(vc));
      }
    };
    fetchDecodedClaims();
  }, [vc]);

  return (
    <div>
      <button
        type="button"
        className={`flex items-center justify-between bg-white w-[339px] lg:w-[410px] min-h-[82px] py-[5px] px-[15px] shadow-lg ${
          showDetailView ? "m-auto rounded-t-[12px] h-[82px]" : "rounded-[12px]"
        }`}
        onClick={view ? () => {} : () => setShowDetailView(!showDetailView)}
      >
        <div className="flex items-center">
          <div
            className={`py-2.5 px-1 xs:col-end-13 lg:col-start-1 lg:col-end-6`}
            key={credentialType}
          >
            <p
              id={`${convertToId(credentialType)}-value`}
              className="font-bold text-largeTextSize break-all"
            >
              {convertToTitleCase(credentialType)}
            </p>
            <div
              className={`rounded-xl max-w-[120px] ${backgroundColorMapping[vcStatus]} border ${borderColorMapping[vcStatus]} p-1`}
            >
              <p
                className={`font-normal text-smallTextSize text-center ${textColorMapping[vcStatus]}`}
              >
                {t(vcStatus)}
              </p>
            </div>
          </div>
        </div>
        {view === false && (showDetailView ? <VectorUp /> : <VectorDown />)}
      </button>
      {showDetailView && claims && (
        <div>
          <div className={`h-[3px] border-b-2 border-b-transparent`} />
          <DisplayVcDetailView
            vc={claims}
            onExpand={() => setModalOpen(true)}
            className={`${
              view ? "h-auto" : "h-[257px]"
            } rounded-t-0 rounded-b-lg overflow-y-auto`}
          />
        </div>
      )}
      {claims && (
        <DisplayVcDetailsModal
          isOpen={isModalOpen}
          onClose={() => setModalOpen(false)}
          vc={claims}
          status={vcStatus}
          vcType={credentialType}
        />
      )}
    </div>
  );
}

export default DisplayVcCardView;
