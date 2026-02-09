import React, { useState } from "react";
import StyledButton from "../../../components/Home/VerificationSection/commons/StyledButton";
import { useTranslation } from "react-i18next";

const MINIMUM_LIMIT = 500;

const ModalPopup = ({ verifiedClaimsCount = 0, transactionLimitMax = MINIMUM_LIMIT }) => {
  const [isPopup, setPopup] = useState(true);
  const { t } = useTranslation("modal");

  const hasVerifiedClaims = verifiedClaimsCount > 0;

  const handleProceed = () => {
    setPopup(false);
  };

  return (
    isPopup && (
      <div className="fixed z-50 inset-0 overflow-y-auto">
        <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
          {/* Background overlay */}
          <div 
            className="fixed inset-0 bg-black bg-opacity-50 transition-opacity" 
            aria-hidden="true"
            onClick={handleProceed}
          ></div>

          {/* Center modal vertically */}
          <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">
            &#8203;
          </span>

          {/* Modal panel */}
          <div className="inline-block align-bottom bg-white rounded-2xl text-center overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:align-middle w-full max-w-[90vw] sm:max-w-lg md:max-w-xl lg:max-w-2xl">
            <div className="bg-white px-6 sm:px-8 md:px-12 pt-8 pb-6">
              {/* Title */}
              <h2 className="text-2xl sm:text-3xl md:text-4xl font-semibold text-gray-900 mb-6">
                {hasVerifiedClaims ? t("congrats") : t("note")}
              </h2>

              {/* Image */}
              <div className="flex justify-center mb-6">
                <img
                  src={hasVerifiedClaims ? "assets/images/excellent_cibil.svg" : "assets/images/fair_cibil.svg"}
                  alt={hasVerifiedClaims ? "excellent_verification" : "basic_verification"}
                  className="w-48 h-48 sm:w-56 sm:h-56 md:w-64 md:h-64 object-contain"
                />
              </div>

              {/* Message */}
              <p className="text-sm sm:text-base md:text-lg text-gray-700 leading-relaxed mb-8 px-2 sm:px-4 md:px-8">
                {hasVerifiedClaims 
                  ? t("verified_message", { limit: `$${transactionLimitMax.toLocaleString()}` })
                  : t("unverified_message", { limit: `$${MINIMUM_LIMIT.toLocaleString()}` })
                }
              </p>

              {/* Button */}
              <StyledButton
                id="verification-proceed-button"
                onClick={handleProceed}
                className="w-full sm:w-auto min-w-[200px] sm:min-w-[280px] mx-auto !text-[#7F56D9] hover:!text-white !border-2 !border-[#7F56D9] hover:!bg-[#7F56D9] !rounded-xl !px-6 !py-3 !text-base sm:!text-lg font-medium transition-colors duration-200"
                data-testid="verification-proceed"
              >
                {t("proceed")}
              </StyledButton>
            </div>
          </div>
        </div>
      </div>
    )
  );
};

export default ModalPopup;
